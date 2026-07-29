from __future__ import annotations

import json
import sys
import urllib.error
import urllib.request
import argparse
from collections import Counter
from pathlib import Path
from typing import Any


MODEL_NAME = "qwen2.5:7b-instruct"
OLLAMA_CHAT_URL = "http://localhost:11434/api/chat"

ALLOWED_LABELS = {
    "NETWORK_UNAVAILABLE",
    "OPENAI_RATE_LIMIT",
    "OPENAI_TIMEOUT",
    "EMPTY_AI_RESPONSE",
    "LOCAL_HISTORY_UNAVAILABLE",
}

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent

def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Оценка модели Ollama на сложном JSONL-наборе."
    )
    parser.add_argument(
        "--dataset",
        type=Path,
        default=PROJECT_DIR / "data" / "hard_eval.jsonl",
        help="Путь к тестовому JSONL-файлу.",
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=25,
        help="Число тестовых примеров.",
    )
    parser.add_argument(
        "--report-name",
        default="hard_baseline",
        help="Префикс файлов отчёта.",
    )
    return parser.parse_args()

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
                    f"Некорректный JSON в {path.name}, строка {line_number}: {exc}"
                ) from exc

            records.append(record)

    return records


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

    body = json.dumps(payload, ensure_ascii=False).encode("utf-8")

    request = urllib.request.Request(
        OLLAMA_CHAT_URL,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    try:
        with urllib.request.urlopen(request, timeout=180) as response:
            response_body = response.read().decode("utf-8")
    except urllib.error.HTTPError as exc:
        error_body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(
            f"Ollama вернул HTTP {exc.code}: {error_body}"
        ) from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(
            "Не удалось подключиться к Ollama по адресу "
            f"{OLLAMA_CHAT_URL}. Проверь, что Ollama запущен."
        ) from exc

    result = json.loads(response_body)

    try:
        return result["message"]["content"].strip()
    except (KeyError, TypeError) as exc:
        raise RuntimeError(
            f"Неожиданный ответ Ollama: {result}"
        ) from exc


def normalize_prediction(raw_response: str) -> str:
    """
    Нормализация нужна только для дополнительной диагностики.

    Exact match ниже всё равно проверяет исходный ответ строго:
    модель должна вернуть ровно одну метку.
    """
    normalized = raw_response.strip()

    if normalized.startswith("`") and normalized.endswith("`"):
        normalized = normalized.strip("`").strip()

    if normalized.upper().startswith("МЕТКА:"):
        normalized = normalized.split(":", maxsplit=1)[1].strip()

    if normalized.upper().startswith("LABEL:"):
        normalized = normalized.split(":", maxsplit=1)[1].strip()

    return normalized


def write_jsonl(path: Path, records: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)

    with path.open("w", encoding="utf-8") as file:
        for record in records:
            json.dump(record, file, ensure_ascii=False)
            file.write("\n")


def main() -> None:
    args = parse_args()
    print(f"Dataset: {args.dataset}")
    print(f"Report name: {args.report_name}")
    eval_records = load_jsonl(args.dataset)

    if args.limit is not None and args.limit <= 0:
        raise ValueError("--limit должен быть положительным числом")

    selected_records = (
        eval_records[:args.limit]
        if args.limit is not None
        else eval_records
    )

    if not selected_records:
        raise ValueError("Тестовый набор пуст")

    results_path = (
        PROJECT_DIR / "reports" / f"{args.report_name}_results.jsonl"
    )
    summary_path = (
        PROJECT_DIR / "reports" / f"{args.report_name}_summary.txt"
    )

    results: list[dict[str, Any]] = []

    print(f"Модель: {MODEL_NAME}")
    print(f"Тестовых примеров: {len(selected_records)}")
    print()

    for index, record in enumerate(selected_records, start=1):
        messages = record["messages"]

        system_prompt = messages[0]["content"]
        user_text = messages[1]["content"]
        expected = messages[2]["content"].strip()

        print(f"[{index}/{len(selected_records)}] {user_text}")

        raw_response = call_ollama(
            system_prompt=system_prompt,
            user_text=user_text,
        )

        normalized = normalize_prediction(raw_response)

        # Строгая проверка: исходный ответ должен быть ровно меткой.
        exact_match = raw_response == expected
        format_compliant = raw_response in ALLOWED_LABELS

        # Дополнительная мягкая проверка для анализа.
        normalized_match = normalized == expected

        result = {
            "index": index,
            "model": MODEL_NAME,
            "user": user_text,
            "expected": expected,
            "raw_response": raw_response,
            "normalized_prediction": normalized,
            "exact_match": exact_match,
            "normalized_match": normalized_match,
            "format_compliant": format_compliant,
        }

        results.append(result)

        print(f"  expected: {expected}")
        print(f"  response: {raw_response!r}")
        print(f"  exact:    {exact_match}")
        print()

    write_jsonl(results_path, results)

    total = len(results)
    exact_correct = sum(item["exact_match"] for item in results)
    normalized_correct = sum(item["normalized_match"] for item in results)
    compliant = sum(item["format_compliant"] for item in results)

    expected_counts = Counter(item["expected"] for item in results)
    correct_by_class = Counter(
        item["expected"]
        for item in results
        if item["exact_match"]
    )

    summary_lines = [
        f"Model: {MODEL_NAME}",
        f"Examples: {total}",
        f"Exact accuracy: {exact_correct}/{total} = {exact_correct / total:.2%}",
        (
            "Normalized accuracy: "
            f"{normalized_correct}/{total} = {normalized_correct / total:.2%}"
        ),
        (
            "Format compliance: "
            f"{compliant}/{total} = {compliant / total:.2%}"
        ),
        "",
        "Per-class exact accuracy:",
    ]

    for label in sorted(ALLOWED_LABELS):
        class_total = expected_counts[label]
        class_correct = correct_by_class[label]

        if class_total == 0:
            summary_lines.append(f"  {label}: нет примеров")
        else:
            summary_lines.append(
                f"  {label}: "
                f"{class_correct}/{class_total} = "
                f"{class_correct / class_total:.2%}"
            )

    summary = "\n".join(summary_lines)

    summary_path.parent.mkdir(parents=True, exist_ok=True)
    summary_path.write_text(summary + "\n", encoding="utf-8")

    print(summary)
    print()
    print(f"Подробные результаты: {results_path}")
    print(f"Сводка: {summary_path}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"\nОшибка: {exc}", file=sys.stderr)
        sys.exit(1)
