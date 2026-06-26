import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
from sklearn.linear_model import LogisticRegression

from app.domain.enums import Sentiment, Topic
from app.ml.rubert_embeddings import RubertEmbedder
from app.ml.training.common import (
    evaluate_model,
    load_dataset,
    save_report,
    split_dataset,
)

DEFAULT_RUBERT_MODEL_VERSION = "rubert-embeddings-0.1.0"


def build_argument_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Train sentiment and topic classifiers on RuBERT embeddings")
    parser.add_argument("--dataset", required=True, help="Path to CSV dataset with text,sentiment,topic columns")
    parser.add_argument("--output", required=True, help="Directory to store model artifacts")
    parser.add_argument("--model-name", default="cointegrated/rubert-tiny2", help="Hugging Face model name")
    parser.add_argument("--test-size", type=float, default=0.2, help="Test set fraction")
    parser.add_argument("--random-state", type=int, default=42, help="Random seed")
    parser.add_argument("--batch-size", type=int, default=16, help="Embedding batch size")
    parser.add_argument("--model-version", default=DEFAULT_RUBERT_MODEL_VERSION, help="Model version to save into metadata")
    parser.add_argument("--reports-dir", default="reports", help="Directory to write JSON evaluation reports")
    return parser


def train_classifier(train_embeddings: np.ndarray, labels) -> LogisticRegression:
    classifier = LogisticRegression(max_iter=2000)
    classifier.fit(train_embeddings, labels)
    return classifier


def save_rubert_artifacts(
    output_dir: str,
    sentiment_classifier: LogisticRegression,
    topic_classifier: LogisticRegression,
    model_name: str,
    model_version: str,
) -> None:
    artifact_dir = Path(output_dir)
    artifact_dir.mkdir(parents=True, exist_ok=True)

    joblib.dump(sentiment_classifier, artifact_dir / "rubert_sentiment_classifier.joblib")
    joblib.dump(topic_classifier, artifact_dir / "rubert_topic_classifier.joblib")

    metadata = {
        "mode": "RUBERT_EMBEDDINGS",
        "modelVersion": model_version,
        "rubertModelName": model_name,
        "trainedAt": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "sentimentClasses": [item.value for item in Sentiment],
        "topicClasses": [item.value for item in Topic],
    }
    (artifact_dir / "rubert_metadata.json").write_text(
        json.dumps(metadata, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )


def main() -> None:
    args = build_argument_parser().parse_args()
    dataset = load_dataset(args.dataset)
    embedder = RubertEmbedder(model_name=args.model_name)

    x_train_sentiment, x_test_sentiment, y_train_sentiment, y_test_sentiment = split_dataset(
        dataset.text_column,
        dataset.sentiment_column,
        args.test_size,
        args.random_state,
    )
    x_train_topic, x_test_topic, y_train_topic, y_test_topic = split_dataset(
        dataset.text_column,
        dataset.topic_column,
        args.test_size,
        args.random_state,
    )

    sentiment_train_embeddings = embedder.embed_texts(x_train_sentiment.tolist(), batch_size=args.batch_size)
    sentiment_test_embeddings = embedder.embed_texts(x_test_sentiment.tolist(), batch_size=args.batch_size)
    topic_train_embeddings = embedder.embed_texts(x_train_topic.tolist(), batch_size=args.batch_size)
    topic_test_embeddings = embedder.embed_texts(x_test_topic.tolist(), batch_size=args.batch_size)

    sentiment_classifier = train_classifier(sentiment_train_embeddings, y_train_sentiment)
    topic_classifier = train_classifier(topic_train_embeddings, y_train_topic)

    sentiment_metrics, sentiment_report = evaluate_model(
        vectorizer=_IdentityVectorizer(),
        model=sentiment_classifier,
        texts=sentiment_test_embeddings,
        labels=y_test_sentiment,
    )
    topic_metrics, topic_report = evaluate_model(
        vectorizer=_IdentityVectorizer(),
        model=topic_classifier,
        texts=topic_test_embeddings,
        labels=y_test_topic,
    )

    save_rubert_artifacts(
        output_dir=args.output,
        sentiment_classifier=sentiment_classifier,
        topic_classifier=topic_classifier,
        model_name=args.model_name,
        model_version=args.model_version,
    )
    save_report("rubert_sentiment_report.json", sentiment_metrics, sentiment_report, args.reports_dir)
    save_report("rubert_topic_report.json", topic_metrics, topic_report, args.reports_dir)

    print(f"Sentiment metrics: {sentiment_metrics}")
    print(f"Topic metrics: {topic_metrics}")
    print(f"Artifacts saved to {args.output}")


class _IdentityVectorizer:
    def transform(self, values):
        return values


if __name__ == "__main__":
    main()
