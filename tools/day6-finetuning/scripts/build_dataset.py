from __future__ import annotations

import json
import random
from collections import Counter
from pathlib import Path


LABELS = {
    "NETWORK_UNAVAILABLE",
    "OPENAI_RATE_LIMIT",
    "OPENAI_TIMEOUT",
    "EMPTY_AI_RESPONSE",
    "LOCAL_HISTORY_UNAVAILABLE",
}

SYSTEM_PROMPT = (
    "Классифицируй сообщение пользователя об ошибке Android-приложения. "
    "Верни только одну метку из списка: "
    "NETWORK_UNAVAILABLE, OPENAI_RATE_LIMIT, OPENAI_TIMEOUT, "
    "EMPTY_AI_RESPONSE, LOCAL_HISTORY_UNAVAILABLE. "
    "Не добавляй объяснений, знаков препинания или другого текста."
)

# Для воспроизводимого разделения train/eval.
RANDOM_SEED = 42

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
DATA_DIR = PROJECT_DIR / "data"

REAL_EXAMPLES_PATH = DATA_DIR / "real_examples.txt"
RAW_PATH = DATA_DIR / "dataset_raw.jsonl"
TRAIN_PATH = DATA_DIR / "train.jsonl"
EVAL_PATH = DATA_DIR / "eval.jsonl"


# Дополнительные примеры. Вместе с 12 строками из real_examples.txt
# получится 60 записей.
SYNTHETIC_EXAMPLES: dict[str, list[str]] = {
    "NETWORK_UNAVAILABLE": [
        "Не удаётся отправить запрос: проверьте подключение к сети.",
        "Приложение не видит интернет, хотя запрос уже был отправлен.",
        "При попытке получить ответ появляется ошибка соединения.",
        "Сервер недоступен из-за отсутствия сети.",
        "После отключения мобильного интернета запрос перестал выполняться.",
        "Приложение сообщает, что соединение с сетью отсутствует.",
        "Не получается подключиться к серверу через Wi-Fi.",
        "Запрос не отправляется в авиарежиме.",
        "Появляется сообщение Network unavailable.",
        "Интернет пропал, и приложение не может получить ответ.",
    ],
    "OPENAI_RATE_LIMIT": [
        "Слишком много запросов, приложение предлагает повторить позже.",
        "Получена ошибка HTTP 429.",
        "Сервис сообщает о превышении лимита запросов.",
        "После нескольких быстрых запросов появился rate limit.",
        "Достигнут предел обращений к модели.",
        "Запрос отклонён из-за ограничения частоты.",
        "Приложение пишет, что квота запросов временно исчерпана.",
        "Сервер вернул Too Many Requests.",
        "Новые запросы временно заблокированы из-за лимита.",
        "После серии сообщений появляется ошибка ограничения API.",
    ],
    "OPENAI_TIMEOUT": [
        "Запрос выполнялся слишком долго и был прерван.",
        "Ответ не пришёл до окончания времени ожидания.",
        "Приложение долго загружало результат, затем показало timeout.",
        "Время ожидания ответа от сервера истекло.",
        "Запрос отменился после длительного ожидания.",
        "Модель не успела ответить за отведённое время.",
        "Через несколько секунд появилась ошибка тайм-аута.",
        "Соединение было установлено, но ответ так и не пришёл вовремя.",
        "Операция завершилась сообщением Request timed out.",
        "Превышено допустимое время выполнения запроса.",
    ],
    "EMPTY_AI_RESPONSE": [
        "Запрос завершился успешно, но текста ответа нет.",
        "Модель вернула пустое сообщение.",
        "Ответ получен, однако поле content оказалось пустым.",
        "В чате появился пустой ответ ассистента.",
        "Ошибки нет, но результат от модели отсутствует.",
        "После загрузки отобразился пустой блок сообщения.",
        "Сервер ответил, но в ответе нет текста.",
        "Приложение добавило сообщение ассистента без содержимого.",
        "Получена пустая строка вместо ответа модели.",
        "Ответ имеет корректный статус, но его содержимое пустое.",
    ],
    "LOCAL_HISTORY_UNAVAILABLE": [
        "Не удалось открыть сохранённые диалоги.",
        "Приложение не загружает локальную историю сообщений.",
        "После перезапуска список чатов оказался недоступен.",
        "Возникла ошибка чтения сохранённой переписки.",
        "История сообщений не восстанавливается из локального хранилища.",
        "Не удаётся получить ранее сохранённые разговоры.",
        "Локальная база данных с чатами недоступна.",
        "При открытии истории появляется ошибка загрузки.",
        "Сохранённые сообщения не отображаются без подключения к серверу.",
        "Приложение не смогло прочитать историю из памяти устройства.",
    ],
}


def read_real_examples(path: Path) -> list[tuple[str, str]]:
    """Читает строки формата LABEL | пользовательское сообщение."""
    if not path.exists():
        raise FileNotFoundError(
            f"Файл не найден: {path}\n"
            "Создай real_examples.txt и добавь строки формата LABEL | текст."
        )

    examples: list[tuple[str, str]] = []

    with path.open("r", encoding="utf-8-sig") as file:
        for line_number, raw_line in enumerate(file, start=1):
            line = raw_line.strip()

            if not line or line.startswith("#"):
                continue

            if "|" not in line:
                raise ValueError(
                    f"Строка {line_number}: отсутствует разделитель '|': {line}"
                )

            label, user_text = (part.strip() for part in line.split("|", maxsplit=1))

            if label not in LABELS:
                raise ValueError(
                    f"Строка {line_number}: неизвестная метка {label!r}"
                )

            if not user_text:
                raise ValueError(
                    f"Строка {line_number}: пользовательский текст пуст"
                )

            examples.append((label, user_text))

    if len(examples) < 12:
        raise ValueError(
            f"Найдено только {len(examples)} примеров. Нужно минимум 12."
        )

    return examples


def make_record(label: str, user_text: str) -> dict:
    return {
        "messages": [
            {
                "role": "system",
                "content": SYSTEM_PROMPT,
            },
            {
                "role": "user",
                "content": user_text,
            },
            {
                "role": "assistant",
                "content": label,
            },
        ]
    }


def write_jsonl(path: Path, records: list[dict]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    with path.open("w", encoding="utf-8") as file:
        for record in records:
            json.dump(record, file, ensure_ascii=False)
            file.write("\n")


def get_label(record: dict) -> str:
    return record["messages"][2]["content"]


def split_stratified(
    records: list[dict],
    eval_ratio: float = 0.2,
) -> tuple[list[dict], list[dict]]:
    """
    Разделяет данные отдельно внутри каждого класса.

    При 12 примерах на класс:
    - примерно 9–10 идут в train;
    - примерно 2–3 идут в eval.
    """
    random_generator = random.Random(RANDOM_SEED)

    grouped: dict[str, list[dict]] = {label: [] for label in LABELS}

    for record in records:
        grouped[get_label(record)].append(record)

    train_records: list[dict] = []
    eval_records: list[dict] = []

    for label in sorted(LABELS):
        label_records = grouped[label]
        random_generator.shuffle(label_records)

        eval_count = max(1, round(len(label_records) * eval_ratio))

        eval_records.extend(label_records[:eval_count])
        train_records.extend(label_records[eval_count:])

    random_generator.shuffle(train_records)
    random_generator.shuffle(eval_records)

    return train_records, eval_records


def main() -> None:
    real_examples = read_real_examples(REAL_EXAMPLES_PATH)

    all_examples: list[tuple[str, str]] = list(real_examples)

    for label, texts in SYNTHETIC_EXAMPLES.items():
        for text in texts:
            all_examples.append((label, text))

    # Удаляем полные дубли по метке и тексту.
    unique_examples = list(dict.fromkeys(all_examples))

    records = [
        make_record(label=label, user_text=user_text)
        for label, user_text in unique_examples
    ]

    train_records, eval_records = split_stratified(records)

    write_jsonl(RAW_PATH, records)
    write_jsonl(TRAIN_PATH, train_records)
    write_jsonl(EVAL_PATH, eval_records)

    total_counts = Counter(get_label(record) for record in records)
    train_counts = Counter(get_label(record) for record in train_records)
    eval_counts = Counter(get_label(record) for record in eval_records)

    print(f"Всего примеров: {len(records)}")
    print(f"Train: {len(train_records)}")
    print(f"Eval: {len(eval_records)}")
    print()
    print(f"Все данные: {dict(sorted(total_counts.items()))}")
    print(f"Train: {dict(sorted(train_counts.items()))}")
    print(f"Eval: {dict(sorted(eval_counts.items()))}")
    print()
    print(f"Создан: {RAW_PATH}")
    print(f"Создан: {TRAIN_PATH}")
    print(f"Создан: {EVAL_PATH}")


if __name__ == "__main__":
    main()