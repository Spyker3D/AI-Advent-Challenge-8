from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path
from typing import Any


MODEL_NAME = "qwen2.5:7b-instruct"
OLLAMA_CHAT_URL = "http://localhost:11434/api/chat"

TEST_LIMIT = 25

MINIMAL_SYSTEM_PROMPT = (
    "Классифицируй ошибку. Верни только одну метку."
)

ALLOWED_LABELS = {
    "NETWORK_UNAVAILABLE",
    "OPENAI_RATE_LIMIT",
    "OPENAI_TIMEOUT",
    "EMPTY_AI_RESPONSE",
    "LOCAL_HISTORY_UNAVAILABLE",
}

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent

EVAL_PATH = PROJECT_DIR / "data" / "hard_eval.jsonl"
REPORTS_DIR = PROJECT_DIR / "reports"

RESULTS_PATH = REPORTS_DIR / "minimal_baseline_results.jsonl"
SUMMARY_PATH = REPORTS_DIR / "minimal_baseline_summary.txt"


def load_jsonl(path: Path) -> list[dict[str, Any]]:
    if not path.exists():
        raise FileNotFoundError(f"Файл не найден: {path}")

    records: list[dict[str, Any]] = []

    with path.open("r", encoding="utf-8") as file:
        for line_number, raw_line in enumerate(file, start=1):
            line = raw_line.strip()

            if not line:
                continue

            try:
                record = json.loads(line)
            except json.JSONDecodeError as exc:
                raise ValueError(
                    f"Некорректный JSON в файле {path.name}, "
                    f"строка {line_number}: {exc}"
                ) from exc

            if not isinstance(record, dict):
                raise ValueError(
                    f"Строка {line_number} в {path.name} "
                    "должна содержать JSON-объект."
                )

            records.append(record)

    if not records:
        raise ValueError(f"Файл {path} не содержит тестовых примеров.")

    return records


def extract_example(
    record: dict[str, Any],
    index: int,
) -> tuple[str, str]:
    messages = record.get("messages")

    if not isinstance(messages, list):
        raise ValueError(
            f"Пример №{index}: поле messages отсутствует "
            "или не является списком."
        )

    user_messages = [
        message
        for message in messages
        if isinstance(message, dict)
        and message.get("role") == "user"
    ]

    assistant_messages = [
        message
        for message in messages
        if isinstance(message, dict)
        and message.get("role") == "assistant"
    ]

    if len(user_messages) != 1:
        raise ValueError(
            f"Пример №{index}: ожидалось ровно одно user-сообщение, "
            f"получено {len(user_messages)}."
        )

    if len(assistant_messages) != 1:
        raise ValueError(
            f"Пример №{index}: ожидалось ровно одно assistant-сообщение, "
            f"получено {len(assistant_messages)}."
        )

    user_text = user_messages[0].get("content")
    expected = assistant_messages[0].get("content")

    if not isinstance(user_text, str) or not user_text.strip():
        raise ValueError(
            f"Пример №{index}: текст пользователя пуст "
            "или имеет неверный формат."
        )

    if not isinstance(expected, str) or not expected.strip():
        raise ValueError(
            f"Пример №{index}: ожидаемая метка пустая "
            "или имеет неверный формат."
        )

    expected = expected.strip()

    if expected not in ALLOWED_LABELS:
        raise ValueError(
            f"Пример №{index}: неизвестная ожидаемая метка "
            f"{expected!r}."
        )

    return user_text.strip(), expected


def call_ollama(
    system_prompt: str,
    user_text: str,
) -> str:
    payload = {
        "model": MODEL_NAME,
        "messages": [
            {
                "role": "system",
                "content": system_prompt,
            },
            {
                "role": "user",
                "content": user_text,
            },
        ],
        "stream": False,
        "options": {
            "temperature": 0,
            "seed": 42,
            "num_predict": 20,
        },
    }

    body = json.dumps(
        payload,
        ensure_ascii=False,
    ).encode("utf-8")

    request = urllib.request.Request(
        OLLAMA_CHAT_URL,
        data=body,
        headers={
            "Content-Type": "application/json",
        },
        method="POST",
    )

    try:
        with urllib.request.urlopen(
            request,
            timeout=180,
        ) as response:
            response_body = response.read().decode("utf-8")

    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode(
            "utf-8",
            errors="replace",
        )

        raise RuntimeError(
            f"Ollama вернул HTTP {exc.code}: {error_body}"
        ) from exc

    except urllib.error.URLError as exc:
        raise RuntimeError(
            "Не удалось подключиться к Ollama по адресу "
            f"{OLLAMA_CHAT_URL}. Проверь, что Ollama запущен."
        ) from exc

    except TimeoutError as exc:
        raise RuntimeError(
            "Истекло время ожидания ответа от Ollama."
        ) from exc

    try:
        result = json.loads(response_body)
    except json.JSONDecodeError as exc:
        raise RuntimeError(
            "Ollama вернул некорректный JSON: "
            f"{response_body[:500]}"
        ) from exc

    try:
        content = result["message"]["content"]
    except (KeyError, TypeError) as exc:
        raise RuntimeError(
            f"Неожиданный ответ Ollama: {result}"
        ) from exc

    if not isinstance(content, str):
        raise RuntimeError(
            "Поле message.content в ответе Ollama "
            "не является строкой."
        )

    return content.strip()


def normalize_prediction(raw_response: str) -> str:
    """
    Нормализация используется только для дополнительной диагностики.

    Основная exact-проверка остаётся строгой: модель должна вернуть
    ровно одну допустимую метку без дополнительного текста.
    """
    normalized = raw_response.strip()

    if normalized.startswith("```") and normalized.endswith("```"):
        normalized = normalized[3:-3].strip()

    elif normalized.startswith("`") and normalized.endswith("`"):
        normalized = normalized[1:-1].strip()

    upper_value = normalized.upper()

    if upper_value.startswith("МЕТКА:"):
        normalized = normalized.split(":", maxsplit=1)[1].strip()

    elif upper_value.startswith("LABEL:"):
        normalized = normalized.split(":", maxsplit=1)[1].strip()

    return normalized


def write_jsonl(
    path: Path,
    records: list[dict[str, Any]],
) -> None:
    path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    with path.open("w", encoding="utf-8") as file:
        for record in records:
            json.dump(
                record,
                file,
                ensure_ascii=False,
            )
            file.write("\n")


def main() -> None:
    eval_records = load_jsonl(EVAL_PATH)

    if TEST_LIMIT <= 0:
        raise ValueError(
            "TEST_LIMIT должен быть положительным числом."
        )

    if len(eval_records) < TEST_LIMIT:
        raise ValueError(
            f"В файле {EVAL_PATH.name} только "
            f"{len(eval_records)} примеров, "
            f"а требуется минимум {TEST_LIMIT}."
        )

    selected_records = eval_records[:TEST_LIMIT]

    results: list[dict[str, Any]] = []

    print(f"Модель: {MODEL_NAME}")
    print(f"Dataset: {EVAL_PATH}")
    print(f"Режим: minimal system prompt")
    print(f"System prompt: {MINIMAL_SYSTEM_PROMPT!r}")
    print(f"Тестовых примеров: {len(selected_records)}")
    print()

    for index, record in enumerate(
        selected_records,
        start=1,
    ):
        user_text, expected = extract_example(
            record=record,
            index=index,
        )

        print(
            f"[{index}/{len(selected_records)}] "
            f"{user_text}"
        )

        raw_response = call_ollama(
            system_prompt=MINIMAL_SYSTEM_PROMPT,
            user_text=user_text,
        )

        normalized = normalize_prediction(raw_response)

        # Строгая проверка:
        # исходный ответ должен точно совпадать с ожидаемой меткой.
        exact_match = raw_response == expected

        # Формат считается корректным только тогда,
        # когда ответ целиком является одной допустимой меткой.
        format_compliant = raw_response in ALLOWED_LABELS

        # Мягкая проверка нужна только для диагностики.
        normalized_match = normalized == expected

        result = {
            "index": index,
            "model": MODEL_NAME,
            "evaluation_mode": "minimal_system_prompt",
            "system_prompt": MINIMAL_SYSTEM_PROMPT,
            "user": user_text,
            "expected": expected,
            "raw_response": raw_response,
            "normalized_prediction": normalized,
            "exact_match": exact_match,
            "normalized_match": normalized_match,
            "format_compliant": format_compliant,
        }

        results.append(result)

        print(f"  expected:   {expected}")
        print(f"  response:   {raw_response!r}")
        print(f"  normalized: {normalized!r}")
        print(f"  exact:      {exact_match}")
        print(f"  format:     {format_compliant}")
        print()

    write_jsonl(
        RESULTS_PATH,
        results,
    )

    total = len(results)

    exact_correct = sum(
        item["exact_match"]
        for item in results
    )

    normalized_correct = sum(
        item["normalized_match"]
        for item in results
    )

    compliant = sum(
        item["format_compliant"]
        for item in results
    )

    expected_counts = Counter(
        item["expected"]
        for item in results
    )

    correct_by_class = Counter(
        item["expected"]
        for item in results
        if item["exact_match"]
    )

    summary_lines = [
        f"Model: {MODEL_NAME}",
        "Evaluation mode: minimal system prompt",
        f"System prompt: {MINIMAL_SYSTEM_PROMPT}",
        f"Dataset: {EVAL_PATH}",
        f"Examples: {total}",
        (
            "Exact accuracy: "
            f"{exact_correct}/{total} = "
            f"{exact_correct / total:.2%}"
        ),
        (
            "Normalized accuracy: "
            f"{normalized_correct}/{total} = "
            f"{normalized_correct / total:.2%}"
        ),
        (
            "Format compliance: "
            f"{compliant}/{total} = "
            f"{compliant / total:.2%}"
        ),
        "",
        "Per-class exact accuracy:",
    ]

    for label in sorted(ALLOWED_LABELS):
        class_total = expected_counts[label]
        class_correct = correct_by_class[label]

        if class_total == 0:
            summary_lines.append(
                f"  {label}: нет примеров"
            )
        else:
            summary_lines.append(
                f"  {label}: "
                f"{class_correct}/{class_total} = "
                f"{class_correct / class_total:.2%}"
            )

    failed_results = [
        item
        for item in results
        if not item["exact_match"]
    ]

    summary_lines.extend(
        [
            "",
            f"Failed examples: {len(failed_results)}",
        ]
    )

    for item in failed_results:
        summary_lines.append(
            f"  #{item['index']}: "
            f"expected={item['expected']}, "
            f"response={item['raw_response']!r}"
        )

    summary = "\n".join(summary_lines)

    SUMMARY_PATH.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    SUMMARY_PATH.write_text(
        summary + "\n",
        encoding="utf-8",
    )

    print(summary)
    print()
    print(f"Подробные результаты: {RESULTS_PATH}")
    print(f"Сводка: {SUMMARY_PATH}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(
            "\nВыполнение остановлено пользователем.",
            file=sys.stderr,
        )
        sys.exit(130)
    except Exception as exc:
        print(
            f"\nОшибка: {exc}",
            file=sys.stderr,
        )
        sys.exit(1)