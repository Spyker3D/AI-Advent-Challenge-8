from __future__ import annotations

import argparse
import os
import sys
import time
from pathlib import Path

from openai import OpenAI


SCRIPT_DIR = Path(__file__).resolve().parent
PROJECT_DIR = SCRIPT_DIR.parent
DATA_DIR = PROJECT_DIR / "data"

DEFAULT_TRAIN_FILE = DATA_DIR / "train.jsonl"
DEFAULT_EVAL_FILE = DATA_DIR / "eval.jsonl"

TERMINAL_STATUSES = {
    "succeeded",
    "failed",
    "cancelled",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description=(
            "Загружает train/eval JSONL в OpenAI, создаёт fine-tuning job "
            "и периодически проверяет его статус."
        )
    )

    parser.add_argument(
        "--train-file",
        type=Path,
        default=DEFAULT_TRAIN_FILE,
        help="Путь к train.jsonl",
    )

    parser.add_argument(
        "--eval-file",
        type=Path,
        default=DEFAULT_EVAL_FILE,
        help="Путь к eval.jsonl",
    )

    parser.add_argument(
        "--model",
        default=os.getenv("OPENAI_FINE_TUNE_MODEL"),
        help=(
            "Модель, поддерживающая fine-tuning. "
            "Можно задать через OPENAI_FINE_TUNE_MODEL."
        ),
    )

    parser.add_argument(
        "--poll-seconds",
        type=int,
        default=30,
        help="Интервал проверки статуса задачи",
    )

    parser.add_argument(
        "--confirm",
        action="store_true",
        help="Разрешить реальную загрузку файлов и запуск fine-tuning",
    )

    return parser.parse_args()


def check_file(path: Path) -> Path:
    resolved = path.resolve()

    if not resolved.exists():
        raise FileNotFoundError(f"Файл не найден: {resolved}")

    if resolved.suffix.lower() != ".jsonl":
        raise ValueError(f"Ожидался JSONL-файл: {resolved}")

    if resolved.stat().st_size == 0:
        raise ValueError(f"Файл пуст: {resolved}")

    return resolved


def upload_fine_tuning_file(client: OpenAI, path: Path) -> str:
    print(f"Загрузка файла: {path}")

    with path.open("rb") as file:
        uploaded = client.files.create(
            file=file,
            purpose="fine-tune",
        )

    print(f"Файл загружен: {uploaded.id}")
    return uploaded.id


def main() -> None:
    args = parse_args()

    train_path = check_file(args.train_file)
    eval_path = check_file(args.eval_file)

    print("Параметры:")
    print(f"  Train: {train_path}")
    print(f"  Eval:  {eval_path}")
    print(f"  Model: {args.model or '[не указана]'}")

    if not args.confirm:
        print()
        print("Режим проверки: API не вызывается.")
        print("Это соответствует требованию задания «пока не запускайте».")
        print()
        print("Для реального запуска потребовалось бы:")
        print("  1. Установить переменную OPENAI_API_KEY")
        print("  2. Указать OPENAI_FINE_TUNE_MODEL")
        print("  3. Добавить аргумент --confirm")
        return

    if not os.getenv("OPENAI_API_KEY"):
        raise RuntimeError("Не задана переменная окружения OPENAI_API_KEY")

    if not args.model:
        raise RuntimeError(
            "Не указана модель. Используй --model или "
            "переменную OPENAI_FINE_TUNE_MODEL."
        )

    client = OpenAI()

    training_file_id = upload_fine_tuning_file(client, train_path)
    validation_file_id = upload_fine_tuning_file(client, eval_path)

    print("Создание fine-tuning job...")

    job = client.fine_tuning.jobs.create(
        training_file=training_file_id,
        validation_file=validation_file_id,
        model=args.model,
    )

    print(f"Job ID: {job.id}")
    print(f"Начальный статус: {job.status}")

    while job.status not in TERMINAL_STATUSES:
        time.sleep(args.poll_seconds)

        job = client.fine_tuning.jobs.retrieve(job.id)

        print(
            f"Статус: {job.status}; "
            f"fine_tuned_model: {job.fine_tuned_model}"
        )

    if job.status == "succeeded":
        print("Fine-tuning успешно завершён.")
        print(f"Модель: {job.fine_tuned_model}")
        return

    print(f"Fine-tuning завершился со статусом: {job.status}")
    print(f"Ошибка: {job.error}")
    sys.exit(1)


if __name__ == "__main__":
    main()