import json
from dataclasses import dataclass
from pathlib import Path

import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, precision_recall_fscore_support
from sklearn.model_selection import train_test_split

from app.domain.enums import Sentiment, Topic
from app.services.preprocess_service import PreprocessService

REQUIRED_COLUMNS = {"text", "sentiment", "topic"}


@dataclass(frozen=True)
class DatasetBundle:
    dataframe: pd.DataFrame
    text_column: pd.Series
    sentiment_column: pd.Series
    topic_column: pd.Series


def load_dataset(dataset_path: str) -> DatasetBundle:
    dataframe = pd.read_csv(dataset_path)
    missing_columns = REQUIRED_COLUMNS.difference(dataframe.columns)
    if missing_columns:
        missing = ", ".join(sorted(missing_columns))
        raise ValueError(f"Dataset must contain columns: {missing}")

    preprocess_service = PreprocessService()
    normalized = dataframe.copy()
    normalized["text"] = normalized["text"].fillna("").astype(str).map(preprocess_service.normalize_text)
    normalized["sentiment"] = normalized["sentiment"].fillna("").astype(str).str.strip().str.upper()
    normalized["topic"] = normalized["topic"].fillna("").astype(str).str.strip().str.upper()

    normalized = normalized[normalized["text"].str.len() >= 3]
    normalized = normalized.drop_duplicates(subset=["text", "sentiment", "topic"]).reset_index(drop=True)

    valid_sentiments = {item.value for item in Sentiment}
    valid_topics = {item.value for item in Topic}

    invalid_sentiments = sorted(set(normalized["sentiment"]).difference(valid_sentiments))
    invalid_topics = sorted(set(normalized["topic"]).difference(valid_topics))
    if invalid_sentiments:
        raise ValueError(f"Unsupported sentiment labels: {', '.join(invalid_sentiments)}")
    if invalid_topics:
        raise ValueError(f"Unsupported topic labels: {', '.join(invalid_topics)}")
    if normalized.empty:
        raise ValueError("Dataset is empty after validation")

    return DatasetBundle(
        dataframe=normalized,
        text_column=normalized["text"],
        sentiment_column=normalized["sentiment"],
        topic_column=normalized["topic"],
    )


def split_dataset(
    texts: pd.Series,
    labels: pd.Series,
    test_size: float,
    random_state: int,
) -> tuple[pd.Series, pd.Series, pd.Series, pd.Series]:
    class_counts = labels.value_counts()
    can_stratify = (
        labels.nunique() > 1
        and not class_counts.empty
        and class_counts.min() >= 2
        and len(labels) >= labels.nunique() * 2
    )

    if can_stratify:
        minimum_test_count = labels.nunique()
        proposed_test_count = max(minimum_test_count, int(round(len(labels) * test_size)))
        proposed_test_count = min(proposed_test_count, len(labels) - labels.nunique())
        return train_test_split(
            texts,
            labels,
            test_size=proposed_test_count,
            random_state=random_state,
            stratify=labels,
        )

    return train_test_split(
        texts,
        labels,
        test_size=test_size,
        random_state=random_state,
    )


def evaluate_model(
    vectorizer,
    model,
    texts,
    labels: pd.Series,
) -> tuple[dict[str, float], dict[str, object]]:
    features = vectorizer.transform(texts)
    predictions = model.predict(features)
    accuracy = accuracy_score(labels, predictions)
    precision, recall, f1_score, _ = precision_recall_fscore_support(
        labels,
        predictions,
        average="weighted",
        zero_division=0,
    )
    metrics = {
        "accuracy": round(float(accuracy), 4),
        "precision": round(float(precision), 4),
        "recall": round(float(recall), 4),
        "f1": round(float(f1_score), 4),
    }
    report = classification_report(labels, predictions, zero_division=0, output_dict=True)
    return metrics, report


def save_report(
    report_name: str,
    metrics: dict[str, float],
    report: dict[str, object],
    reports_dir: str = "reports",
) -> None:
    report_dir = Path(reports_dir)
    report_dir.mkdir(parents=True, exist_ok=True)
    payload = {
        "metrics": metrics,
        "report": report,
    }
    (report_dir / report_name).write_text(
        json.dumps(payload, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
