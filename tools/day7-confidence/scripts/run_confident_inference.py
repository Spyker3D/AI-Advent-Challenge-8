from __future__ import annotations

import argparse
import json
import statistics
import sys
import time
import urllib.error
import urllib.request
from collections import Counter
from pathlib import Path
from typing import Any


MODEL_NAME = "qwen2.5:7b-instruct"
OLLAMA_URL = "http://localhost:11434/api/chat"

ACCEPT_THRESHOLD = 0.85
RETRY_THRESHOLD = 0.75
SELF_CHECK_THRESHOLD = 0.80
MAJORITY_CONFIDENCE_THRESHOLD = 0.80

ALLOWED_LABELS = {
    "NETWORK_UNAVAILABLE",
    "OPENAI_RATE_LIMIT",
    "OPENAI_TIMEOUT",
    "EMPTY_AI_RESPONSE",
    "LOCAL_HISTORY_UNAVAILABLE",
}

SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent

DEFAULT_DATASET_PATH = PROJECT_DIR / "data" / "test_cases.jsonl"
REPORTS_DIR = PROJECT_DIR / "reports"


# Это основной системный промпт.
# Он используется при классификации сообщения.
CLASSIFICATION_SYSTEM_PROMPT = """
Ты выполняешь критически важную классификацию ошибок Android-приложения.

Допустимые метки:

NETWORK_UNAVAILABLE
OPENAI_RATE_LIMIT
OPENAI_TIMEOUT
EMPTY_AI_RESPONSE
LOCAL_HISTORY_UNAVAILABLE

Значения меток:

NETWORK_UNAVAILABLE — отсутствует интернет или сетевое подключение.
OPENAI_RATE_LIMIT — превышен лимит запросов, ошибка 429 или Too Many Requests.
OPENAI_TIMEOUT — истекло время ожидания ответа.
EMPTY_AI_RESPONSE — ответ ассистента существует, но его текст пустой.
LOCAL_HISTORY_UNAVAILABLE — локальная история чатов не загрузилась.

Верни только один JSON-объект:

{
  "label": "ОДНА_ИЗ_ДОПУСТИМЫХ_МЕТОК",
  "confidence": 0.0,
  "ambiguity": "LOW",
  "reason": "краткое объяснение"
}

ambiguity:

LOW
сообщение однозначное

MEDIUM
есть несколько возможных причин

HIGH
невозможно определить категорию
или присутствуют признаки нескольких категорий

Если сообщение неоднозначно —
ставь ambiguity=HIGH и confidence <=0.5

Правила:

1. Не придумывай новые метки.
2. confidence должен быть числом от 0.0 до 1.0.
3. При неоднозначном или противоречивом сообщении снижай confidence.
4. Не используй Markdown.
5. Не добавляй текст до или после JSON.
""".strip()


# Это отдельный системный промпт для проверки ответа.
# Он вызывается уже после основной классификации.
SELF_CHECK_SYSTEM_PROMPT = """
Ты проверяешь результат критически важной классификации.

Допустимые метки:

NETWORK_UNAVAILABLE
OPENAI_RATE_LIMIT
OPENAI_TIMEOUT
EMPTY_AI_RESPONSE
LOCAL_HISTORY_UNAVAILABLE

Проверь, действительно ли предложенная метка соответствует входному сообщению.

Верни только один JSON-объект:

{
    "verdict":"PASS",
    "confidence":0.0,
    "suggested_label":"МЕТКА_ИЛИ_NULL",
    "is_ambiguous":false,
    "reason":"краткое объяснение"
}

PASS

только если

1. предложенная метка правильная

2. сообщение НЕ является неоднозначным

FAIL

если есть признаки нескольких категорий

или информации недостаточно.

Правила:

1. verdict может быть только PASS или FAIL.
2. PASS означает, что предложенную метку можно принять.
3. FAIL означает, что метка неправильная или сообщение неоднозначно.
4. suggested_label должен содержать допустимую метку или null.
5. confidence должен быть числом от 0.0 до 1.0.
6. Не используй Markdown.
7. Не добавляй текст до или после JSON.
""".strip()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Контролируемый инференс с оценкой уверенности."
    )

    parser.add_argument(
        "--dataset",
        type=Path,
        default=DEFAULT_DATASET_PATH,
    )
    parser.add_argument(
        "--mode",
        choices=("raw", "controlled"),
        default="controlled",
    )
    parser.add_argument(
        "--model",
        default=MODEL_NAME,
    )
    parser.add_argument(
        "--report-name",
        default=None,
    )
    parser.add_argument(
        "--limit",
        type=int,
        default=None,
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
                    f"Некорректный JSON, строка {line_number}: {exc}"
                ) from exc

            records.append(record)

    if not records:
        raise ValueError("Тестовый набор пуст.")

    return records


def call_ollama(
    model: str,
    system_prompt: str,
    user_text: str,
    seed: int,
) -> dict[str, Any]:
    payload = {
        "model": model,
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
        "format": "json",
        "options": {
            "temperature": 0.1,
            "seed": seed,
            "num_predict": 180,
        },
    }

    body = json.dumps(
        payload,
        ensure_ascii=False,
    ).encode("utf-8")

    request = urllib.request.Request(
        OLLAMA_URL,
        data=body,
        headers={"Content-Type": "application/json"},
        method="POST",
    )

    started_at = time.perf_counter()

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
            f"Не удалось подключиться к Ollama: {OLLAMA_URL}"
        ) from exc

    latency_ms = (time.perf_counter() - started_at) * 1000

    try:
        ollama_result = json.loads(response_body)
        content = ollama_result["message"]["content"].strip()
    except (json.JSONDecodeError, KeyError, TypeError) as exc:
        raise RuntimeError(
            f"Неожиданный ответ Ollama: {response_body[:500]}"
        ) from exc

    return {
        "content": content,
        "latency_ms": latency_ms,
        "prompt_tokens": int(
            ollama_result.get("prompt_eval_count", 0)
        ),
        "completion_tokens": int(
            ollama_result.get("eval_count", 0)
        ),
    }


def parse_json_object(raw_response: str) -> dict[str, Any]:
    try:
        parsed = json.loads(raw_response)
    except json.JSONDecodeError as exc:
        raise ValueError(
            f"Ответ не является валидным JSON: {exc}"
        ) from exc

    if not isinstance(parsed, dict):
        raise ValueError("Ответ должен быть JSON-объектом.")

    return parsed


# Это constraint-based проверка основного ответа.
# Она не обращается к модели.
# Она проверяет JSON обычным Python-кодом.
def validate_classification_response(
    data: dict[str, Any],
) -> list[str]:
    errors: list[str] = []

    required_fields = {
        "label",
        "confidence",
        "ambiguity",
        "reason",
    }

    missing_fields = required_fields - data.keys()

    if missing_fields:
        errors.append(
            f"Отсутствуют поля: {sorted(missing_fields)}"
        )

    label = data.get("label")

    if label not in ALLOWED_LABELS:
        errors.append(
            f"Недопустимая метка: {label!r}"
        )

    confidence = data.get("confidence")

    if isinstance(confidence, bool) or not isinstance(
        confidence,
        (int, float),
    ):
        errors.append("confidence должен быть числом.")
    else:
        confidence_value = float(confidence)

        if not 0.0 <= confidence_value <= 1.0:
            errors.append(
                "confidence должен находиться между 0.0 и 1.0."
            )

    ambiguity = data.get("ambiguity")

    if ambiguity not in {
        "LOW",
        "MEDIUM",
        "HIGH",
    }:
        errors.append(
            "ambiguity должен быть LOW, MEDIUM или HIGH."
        )

    if (
        ambiguity == "HIGH"
        and isinstance(confidence, (int, float))
        and not isinstance(confidence, bool)
        and float(confidence) > 0.6
    ):
        errors.append(
            "HIGH ambiguity не может иметь confidence > 0.6."
        )

    reason = data.get("reason")

    if not isinstance(reason, str) or not reason.strip():
        errors.append(
            "reason должен быть непустой строкой."
        )

    return errors


# Это constraint-based проверка self-check ответа.
def validate_self_check_response(
    data: dict[str, Any],
) -> list[str]:
    errors: list[str] = []

    required_fields = {
        "verdict",
        "confidence",
        "suggested_label",
        "is_ambiguous",
        "reason",
    }

    missing_fields = required_fields - data.keys()

    if missing_fields:
        errors.append(
            f"Отсутствуют поля self-check: "
            f"{sorted(missing_fields)}"
        )

    verdict = data.get("verdict")

    if verdict not in {"PASS", "FAIL"}:
        errors.append(
            "verdict должен быть PASS или FAIL."
        )

    confidence = data.get("confidence")

    if isinstance(confidence, bool) or not isinstance(
        confidence,
        (int, float),
    ):
        errors.append(
            "Self-check confidence должен быть числом."
        )
    elif not 0.0 <= float(confidence) <= 1.0:
        errors.append(
            "Self-check confidence вне диапазона 0.0–1.0."
        )

    suggested_label = data.get("suggested_label")

    if (
        suggested_label is not None
        and suggested_label not in ALLOWED_LABELS
    ):
        errors.append(
            f"Недопустимая suggested_label: "
            f"{suggested_label!r}"
        )

    is_ambiguous = data.get("is_ambiguous")

    if not isinstance(is_ambiguous, bool):
        errors.append(
            "is_ambiguous должен быть true или false."
        )

    reason = data.get("reason")

    if not isinstance(reason, str) or not reason.strip():
        errors.append(
            "Self-check reason должен быть непустой строкой."
        )

    return errors


def perform_classification(
    model: str,
    user_text: str,
    seed: int,
) -> dict[str, Any]:
    api_result = call_ollama(
        model=model,
        system_prompt=CLASSIFICATION_SYSTEM_PROMPT,
        user_text=user_text,
        seed=seed,
    )

    try:
        parsed = parse_json_object(api_result["content"])
        validation_errors = validate_classification_response(
            parsed
        )
    except ValueError as exc:
        parsed = None
        validation_errors = [str(exc)]

    return {
        "parsed": parsed,
        "raw_response": api_result["content"],
        "validation_errors": validation_errors,
        "latency_ms": api_result["latency_ms"],
        "prompt_tokens": api_result["prompt_tokens"],
        "completion_tokens": api_result["completion_tokens"],
    }


def perform_self_check(
    model: str,
    user_text: str,
    proposed_label: str,
) -> dict[str, Any]:
    self_check_user_prompt = (
        f"Входное сообщение:\n{user_text}\n\n"
        f"Предложенная метка:\n{proposed_label}"
    )

    api_result = call_ollama(
        model=model,
        system_prompt=SELF_CHECK_SYSTEM_PROMPT,
        user_text=self_check_user_prompt,
        seed=777,
    )

    try:
        parsed = parse_json_object(api_result["content"])
        validation_errors = validate_self_check_response(
            parsed
        )
    except ValueError as exc:
        parsed = None
        validation_errors = [str(exc)]

    return {
        "parsed": parsed,
        "raw_response": api_result["content"],
        "validation_errors": validation_errors,
        "latency_ms": api_result["latency_ms"],
        "prompt_tokens": api_result["prompt_tokens"],
        "completion_tokens": api_result["completion_tokens"],
    }


def calculate_majority(
    predictions: list[dict[str, Any]],
) -> tuple[str | None, float, int]:
    valid_predictions = [
        prediction
        for prediction in predictions
        if prediction["parsed"] is not None
        and not prediction["validation_errors"]
        and prediction["parsed"].get("ambiguity") == "LOW"
    ]

    if not valid_predictions:
        return None, 0.0, 0

    labels = [
        prediction["parsed"]["label"]
        for prediction in valid_predictions
    ]

    counts = Counter(labels)
    winning_label, winning_count = counts.most_common(1)[0]

    winning_confidences = [
        float(prediction["parsed"]["confidence"])
        for prediction in valid_predictions
        if prediction["parsed"]["label"] == winning_label
    ]

    average_confidence = statistics.mean(
        winning_confidences
    )

    return (
        winning_label,
        average_confidence,
        winning_count,
    )


def run_raw(
    model: str,
    record: dict[str, Any],
) -> dict[str, Any]:
    prediction = perform_classification(
        model=model,
        user_text=record["input"],
        seed=42,
    )

    parsed = prediction["parsed"]
    errors = prediction["validation_errors"]

    if parsed is None or errors:
        status = "FAIL"
        accepted_label = None
        confidence = None
        rejection_reason = "FORMAT_ERROR"
    else:
        status = "OK"
        accepted_label = parsed["label"]
        confidence = float(parsed["confidence"])
        rejection_reason = None

    expected = record.get("expected")

    is_correct = (
        accepted_label == expected
        if expected is not None
        else None
    )

    return {
        "id": record["id"],
        "category": record["category"],
        "input": record["input"],
        "expected": expected,
        "mode": "raw",
        "status": status,
        "accepted_label": accepted_label,
        "is_correct": is_correct,
        "initial_label": accepted_label,
        "initial_confidence": confidence,
        "final_confidence": confidence,
        "self_check_performed": False,
        "self_check_passed": False,
        "classification_calls": 1,
        "self_check_calls": 0,
        "format_retries": 0,
        "total_model_calls": 1,
        "latency_ms": prediction["latency_ms"],
        "prompt_tokens": prediction["prompt_tokens"],
        "completion_tokens": prediction["completion_tokens"],
        "rejection_reason": rejection_reason,
        "all_predictions": [
            {
                "raw_response": prediction["raw_response"],
                "parsed": parsed,
                "validation_errors": errors,
            }
        ],
    }


def run_controlled(
    model: str,
    record: dict[str, Any],
) -> dict[str, Any]:
    user_text = record["input"]
    expected = record.get("expected")

    predictions: list[dict[str, Any]] = []

    total_latency_ms = 0.0
    total_prompt_tokens = 0
    total_completion_tokens = 0

    classification_calls = 0
    self_check_calls = 0
    format_retries = 0

    first_prediction = perform_classification(
        model=model,
        user_text=user_text,
        seed=42,
    )

    predictions.append(first_prediction)
    classification_calls += 1

    total_latency_ms += first_prediction["latency_ms"]
    total_prompt_tokens += first_prediction["prompt_tokens"]
    total_completion_tokens += first_prediction[
        "completion_tokens"
    ]

    # Если формат первого ответа нарушен,
    # выполняем один повторный запрос.
    if (
        first_prediction["parsed"] is None
        or first_prediction["validation_errors"]
    ):
        format_retries += 1

        retry_prediction = perform_classification(
            model=model,
            user_text=user_text,
            seed=137,
        )

        predictions.append(retry_prediction)
        classification_calls += 1

        total_latency_ms += retry_prediction["latency_ms"]
        total_prompt_tokens += retry_prediction[
            "prompt_tokens"
        ]
        total_completion_tokens += retry_prediction[
            "completion_tokens"
        ]

        if (
            retry_prediction["parsed"] is None
            or retry_prediction["validation_errors"]
        ):
            return {
                "id": record["id"],
                "category": record["category"],
                "input": user_text,
                "expected": expected,
                "mode": "controlled",
                "status": "FAIL",
                "accepted_label": None,
                "is_correct": None,
                "initial_label": None,
                "initial_confidence": None,
                "final_confidence": None,
                "self_check_performed": False,
                "self_check_passed": False,
                "classification_calls": classification_calls,
                "self_check_calls": self_check_calls,
                "format_retries": format_retries,
                "total_model_calls": (
                    classification_calls + self_check_calls
                ),
                "latency_ms": total_latency_ms,
                "prompt_tokens": total_prompt_tokens,
                "completion_tokens": total_completion_tokens,
                "rejection_reason": "FORMAT_ERROR",
                "all_predictions": predictions,
            }

        first_prediction = retry_prediction

    first_data = first_prediction["parsed"]
    initial_label = first_data["label"]
    initial_confidence = float(
        first_data["confidence"]
    )

    # Высокая неоднозначность — сразу UNSURE.
    if first_data["ambiguity"] == "HIGH":
        return {
            "id": record["id"],
            "category": record["category"],
            "input": user_text,
            "expected": expected,
            "mode": "controlled",
            "status": "UNSURE",
            "accepted_label": None,
            "is_correct": None,
            "initial_label": initial_label,
            "initial_confidence": initial_confidence,
            "final_confidence": initial_confidence,
            "self_check_performed": False,
            "self_check_passed": False,
            "classification_calls": classification_calls,
            "self_check_calls": self_check_calls,
            "format_retries": format_retries,
            "total_model_calls": classification_calls,
            "latency_ms": total_latency_ms,
            "prompt_tokens": total_prompt_tokens,
            "completion_tokens": total_completion_tokens,
            "rejection_reason": "HIGH_AMBIGUITY",
            "all_predictions": predictions,
        }

    self_check_performed = False
    self_check_passed = False
    self_check_result: dict[str, Any] | None = None

    # При высокой уверенности запускается self-check.
    if (
        initial_confidence >= ACCEPT_THRESHOLD
        and first_data["ambiguity"] == "LOW"
    ):
        self_check_performed = True
        self_check_calls += 1

        self_check_result = perform_self_check(
            model=model,
            user_text=user_text,
            proposed_label=initial_label,
        )

        total_latency_ms += self_check_result["latency_ms"]
        total_prompt_tokens += self_check_result[
            "prompt_tokens"
        ]
        total_completion_tokens += self_check_result[
            "completion_tokens"
        ]

        if (
            self_check_result["parsed"] is not None
            and not self_check_result["validation_errors"]
        ):
            self_check_data = self_check_result["parsed"]

            self_check_passed = (
                self_check_data["verdict"] == "PASS"
                and self_check_data["is_ambiguous"] is False
                and self_check_data["suggested_label"] == initial_label
                and float(self_check_data["confidence"])
                >= SELF_CHECK_THRESHOLD
            )

        if self_check_passed:
            is_correct = (
                initial_label == expected
                if expected is not None
                else None
            )

            return {
                "id": record["id"],
                "category": record["category"],
                "input": user_text,
                "expected": expected,
                "mode": "controlled",
                "status": "OK",
                "accepted_label": initial_label,
                "is_correct": is_correct,
                "initial_label": initial_label,
                "initial_confidence": initial_confidence,
                "final_confidence": initial_confidence,
                "self_check_performed": True,
                "self_check_passed": True,
                "self_check_result": self_check_result,
                "classification_calls": classification_calls,
                "self_check_calls": self_check_calls,
                "format_retries": format_retries,
                "total_model_calls": (
                    classification_calls + self_check_calls
                ),
                "latency_ms": total_latency_ms,
                "prompt_tokens": total_prompt_tokens,
                "completion_tokens": total_completion_tokens,
                "rejection_reason": None,
                "all_predictions": predictions,
            }

    # Средняя уверенность или проваленный self-check:
    # выполняем ещё два классификационных запроса.
    for seed in (137, 999):
        additional_prediction = perform_classification(
            model=model,
            user_text=user_text,
            seed=seed,
        )

        predictions.append(additional_prediction)
        classification_calls += 1

        total_latency_ms += additional_prediction[
            "latency_ms"
        ]
        total_prompt_tokens += additional_prediction[
            "prompt_tokens"
        ]
        total_completion_tokens += additional_prediction[
            "completion_tokens"
        ]

    winning_label, final_confidence, winning_count = (
        calculate_majority(predictions)
    )

    accepted = (
        winning_label is not None
        and winning_count == 3
        and final_confidence
        >= MAJORITY_CONFIDENCE_THRESHOLD
    )

    if accepted:
        status = "OK"
        accepted_label = winning_label
        rejection_reason = None
        is_correct = (
            accepted_label == expected
            if expected is not None
            else None
        )
    else:
        status = "UNSURE"
        accepted_label = None
        rejection_reason = "NO_CONSENSUS"
        is_correct = None

    return {
        "id": record["id"],
        "category": record["category"],
        "input": user_text,
        "expected": expected,
        "mode": "controlled",
        "status": status,
        "accepted_label": accepted_label,
        "is_correct": is_correct,
        "initial_label": initial_label,
        "initial_confidence": initial_confidence,
        "final_confidence": final_confidence,
        "self_check_performed": self_check_performed,
        "self_check_passed": self_check_passed,
        "self_check_result": self_check_result,
        "classification_calls": classification_calls,
        "self_check_calls": self_check_calls,
        "format_retries": format_retries,
        "total_model_calls": (
            classification_calls + self_check_calls
        ),
        "latency_ms": total_latency_ms,
        "prompt_tokens": total_prompt_tokens,
        "completion_tokens": total_completion_tokens,
        "rejection_reason": rejection_reason,
        "all_predictions": predictions,
    }


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


def percentile_95(values: list[float]) -> float:
    if not values:
        return 0.0

    sorted_values = sorted(values)
    index = round(0.95 * (len(sorted_values) - 1))

    return sorted_values[index]


def create_summary(
    results: list[dict[str, Any]],
    model: str,
    mode: str,
) -> str:
    total = len(results)

    accepted = sum(
        result["status"] == "OK"
        for result in results
    )
    unsure = sum(
        result["status"] == "UNSURE"
        for result in results
    )
    failed = sum(
        result["status"] == "FAIL"
        for result in results
    )

    rejected = unsure + failed

    correct_accepted = sum(
        result["status"] == "OK"
        and result["is_correct"] is True
        for result in results
    )

    incorrect_accepted = sum(
        result["status"] == "OK"
        and result["is_correct"] is False
        for result in results
    )

    repeated_examples = sum(
        result["total_model_calls"] > 1
        for result in results
    )

    total_model_calls = sum(
        result["total_model_calls"]
        for result in results
    )

    format_retries = sum(
        result["format_retries"]
        for result in results
    )

    self_check_calls = sum(
        result["self_check_calls"]
        for result in results
    )

    latencies = [
        float(result["latency_ms"])
        for result in results
    ]

    prompt_tokens = sum(
        result["prompt_tokens"]
        for result in results
    )

    completion_tokens = sum(
        result["completion_tokens"]
        for result in results
    )

    ambiguous_total = sum(
        result["expected"] is None
        for result in results
    )

    ambiguous_accepted = sum(
        result["expected"] is None
        and result["status"] == "OK"
        for result in results
    )

    ambiguous_rejected = sum(
        result["expected"] is None
        and result["status"] != "OK"
        for result in results
    )

    lines = [
        f"Model: {model}",
        f"Mode: {mode}",
        f"Examples: {total}",
        "",
        f"Accepted: {accepted}",
        f"Unsure: {unsure}",
        f"Failed: {failed}",
        (
            f"Rejected total: {rejected}/{total} = "
            f"{rejected / total:.2%}"
        ),
        "",
        f"Correct accepted predictions: {correct_accepted}",
        f"Incorrect accepted predictions: {incorrect_accepted}",
        "",
        (
            "Examples requiring repeated inference: "
            f"{repeated_examples}/{total} = "
            f"{repeated_examples / total:.2%}"
        ),
        f"Format retries: {format_retries}",
        f"Self-check calls: {self_check_calls}",
        f"Total model calls: {total_model_calls}",
        (
            "Average model calls per example: "
            f"{total_model_calls / total:.2f}"
        ),
        "",
        (
            "Average latency: "
            f"{statistics.mean(latencies):.2f} ms"
        ),
        (
            "Median latency: "
            f"{statistics.median(latencies):.2f} ms"
        ),
        (
            "P95 latency: "
            f"{percentile_95(latencies):.2f} ms"
        ),
        "",
        "Ambiguous examples:",
        f"  Total: {ambiguous_total}",
        f"  Accepted: {ambiguous_accepted}",
        f"  Rejected: {ambiguous_rejected}",
        (
            "  Rejection rate: "
            f"{ambiguous_rejected}/{ambiguous_total} = "
            f"{ambiguous_rejected / ambiguous_total:.2%}"
            if ambiguous_total
            else "  Rejection rate: нет примеров"
        ),
        "",
        f"Prompt tokens: {prompt_tokens}",
        f"Completion tokens: {completion_tokens}",
        "Estimated local model cost: $0.00",
        "",
        "Results by category:",
    ]

    for category in ("correct", "boundary", "noisy"):
        category_results = [
            result
            for result in results
            if result["category"] == category
        ]

        category_total = len(category_results)

        category_ok = sum(
            result["status"] == "OK"
            for result in category_results
        )

        category_rejected = sum(
            result["status"] != "OK"
            for result in category_results
        )

        lines.append(
            f"  {category}: "
            f"total={category_total}, "
            f"accepted={category_ok}, "
            f"rejected={category_rejected}"
        )

    lines.extend(
        [
            "",
            "Incorrect accepted examples:",
        ]
    )

    incorrect_items = [
        result
        for result in results
        if result["status"] == "OK"
        and result["is_correct"] is False
    ]

    if not incorrect_items:
        lines.append("  None")
    else:
        for result in incorrect_items:
            lines.append(
                f"  {result['id']}: "
                f"expected={result['expected']}, "
                f"accepted={result['accepted_label']}"
            )

    lines.extend(
        [
            "",
            "Rejected examples:",
        ]
    )

    rejected_items = [
        result
        for result in results
        if result["status"] != "OK"
    ]

    if not rejected_items:
        lines.append("  None")
    else:
        for result in rejected_items:
            lines.append(
                f"  {result['id']}: "
                f"status={result['status']}, "
                f"reason={result['rejection_reason']}"
            )

    return "\n".join(lines)


def main() -> None:
    args = parse_args()

    records = load_jsonl(args.dataset)

    if args.limit is not None:
        if args.limit <= 0:
            raise ValueError(
                "--limit должен быть положительным."
            )

        records = records[:args.limit]

    report_name = (
        args.report_name
        if args.report_name
        else args.mode
    )

    results_path = (
        REPORTS_DIR
        / f"{report_name}_results.jsonl"
    )

    summary_path = (
        REPORTS_DIR
        / f"{report_name}_summary.txt"
    )

    results: list[dict[str, Any]] = []

    print(f"Model: {args.model}")
    print(f"Mode: {args.mode}")
    print(f"Dataset: {args.dataset}")
    print(f"Examples: {len(records)}")
    print()

    for index, record in enumerate(records, start=1):
        print(
            f"[{index}/{len(records)}] "
            f"{record['id']}: {record['input']}"
        )

        if args.mode == "raw":
            result = run_raw(
                model=args.model,
                record=record,
            )
        else:
            result = run_controlled(
                model=args.model,
                record=record,
            )

        results.append(result)

        print(
            f"  status: {result['status']}"
        )
        print(
            f"  label: {result['accepted_label']}"
        )
        print(
            f"  confidence: "
            f"{result['final_confidence']}"
        )
        print(
            f"  model calls: "
            f"{result['total_model_calls']}"
        )
        print(
            f"  latency: "
            f"{result['latency_ms']:.2f} ms"
        )
        print()

    write_jsonl(
        results_path,
        results,
    )

    summary = create_summary(
        results=results,
        model=args.model,
        mode=args.mode,
    )

    summary_path.parent.mkdir(
        parents=True,
        exist_ok=True,
    )

    summary_path.write_text(
        summary + "\n",
        encoding="utf-8",
    )

    print(summary)
    print()
    print(f"Results: {results_path}")
    print(f"Summary: {summary_path}")


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print(
            "\nОстановлено пользователем.",
            file=sys.stderr,
        )
        sys.exit(130)
    except Exception as exc:
        print(
            f"\nОшибка: {exc}",
            file=sys.stderr,
        )
        sys.exit(1)