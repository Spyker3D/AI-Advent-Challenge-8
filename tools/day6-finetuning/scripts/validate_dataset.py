from __future__ import annotations

import json
from collections import Counter
from pathlib import Path


ALLOWED_LABELS = {
    "NETWORK_UNAVAILABLE",
    "OPENAI_RATE_LIMIT",
    "OPENAI_TIMEOUT",
    "EMPTY_AI_RESPONSE",
    "LOCAL_HISTORY_UNAVAILABLE",
}

EXPECTED_ROLES = ["system", "user", "assistant"]

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
DATA_DIR = PROJECT_DIR / "data"

TRAIN_PATH = DATA_DIR / "train.jsonl"
EVAL_PATH = DATA_DIR / "eval.jsonl"


def load_and_validate(path: Path) -> list[dict]:
    errors: list[str] = []
    records: list[dict] = []
    seen_user_texts: set[str] = set()

    if not path.exists():
        raise FileNotFoundError(f"Файл не найден: {path}")

    with path.open("r", encoding="utf-8") as file:
        for line_number, raw_line in enumerate(file, start=1):
            line = raw_line.strip()

            if not line:
                errors.append(f"{path.name}, строка {line_number}: пустая строка")
                continue

            try:
                record = json.loads(line)
            except json.JSONDecodeError as exc:
                errors.append(
                    f"{path.name}, строка {line_number}: невалидный JSON: {exc}"
                )
                continue

            if not isinstance(record, dict):
                errors.append(
                    f"{path.name}, строка {line_number}: корневой объект должен быть JSON-объектом"
                )
                continue

            messages = record.get("messages")

            if not isinstance(messages, list):
                errors.append(
                    f"{path.name}, строка {line_number}: поле messages отсутствует или не является массивом"
                )
                continue

            if len(messages) != 3:
                errors.append(
                    f"{path.name}, строка {line_number}: ожидалось 3 сообщения, получено {len(messages)}"
                )
                continue

            roles = [message.get("role") for message in messages]

            if roles != EXPECTED_ROLES:
                errors.append(
                    f"{path.name}, строка {line_number}: роли должны быть "
                    f"{EXPECTED_ROLES}, получено {roles}"
                )

            for index, message in enumerate(messages):
                if not isinstance(message, dict):
                    errors.append(
                        f"{path.name}, строка {line_number}: "
                        f"сообщение {index + 1} не является объектом"
                    )
                    continue

                content = message.get("content")

                if not isinstance(content, str) or not content.strip():
                    errors.append(
                        f"{path.name}, строка {line_number}: "
                        f"пустой content у роли {message.get('role')}"
                    )

            assistant_content = messages[2].get("content")

            if assistant_content not in ALLOWED_LABELS:
                errors.append(
                    f"{path.name}, строка {line_number}: "
                    f"недопустимая метка {assistant_content!r}"
                )

            user_text = messages[1].get("content", "").strip()

            if user_text in seen_user_texts:
                errors.append(
                    f"{path.name}, строка {line_number}: "
                    f"дубликат пользовательского текста: {user_text!r}"
                )
            else:
                seen_user_texts.add(user_text)

            records.append(record)

    if errors:
        print(f"\nОшибки в {path.name}:")
        for error in errors:
            print(f"- {error}")

        raise ValueError(
            f"Файл {path.name} не прошёл проверку. Найдено ошибок: {len(errors)}"
        )

    return records


def get_user_text(record: dict) -> str:
    return record["messages"][1]["content"].strip()


def get_label(record: dict) -> str:
    return record["messages"][2]["content"].strip()


def print_stats(name: str, records: list[dict]) -> None:
    counts = Counter(get_label(record) for record in records)

    print(f"\n{name}:")
    print(f"Количество примеров: {len(records)}")

    for label in sorted(ALLOWED_LABELS):
        print(f"  {label}: {counts[label]}")


def check_train_eval_overlap(
    train_records: list[dict],
    eval_records: list[dict],
) -> None:
    train_texts = {get_user_text(record) for record in train_records}
    eval_texts = {get_user_text(record) for record in eval_records}

    overlap = train_texts & eval_texts

    if overlap:
        print("\nНайдены пересечения между train и eval:")

        for text in sorted(overlap):
            print(f"- {text}")

        raise ValueError(
            f"Train и eval содержат одинаковые сообщения: {len(overlap)}"
        )


def main() -> None:
    train_records = load_and_validate(TRAIN_PATH)
    eval_records = load_and_validate(EVAL_PATH)

    check_train_eval_overlap(train_records, eval_records)

    print_stats("TRAIN", train_records)
    print_stats("EVAL", eval_records)

    print("\nВсе проверки успешно пройдены.")


if __name__ == "__main__":
    main()