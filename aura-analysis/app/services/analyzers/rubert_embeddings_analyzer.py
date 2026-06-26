from __future__ import annotations

import json
from pathlib import Path

import joblib
import numpy as np

from app.core.exceptions import ModelNotFoundError
from app.domain.enums import Sentiment, Topic
from app.ml.rubert_embeddings import RubertEmbedder
from app.services.analyzers.base import AnalyzeResult
from app.services.keyword_service import KeywordService
from app.services.preprocess_service import PreprocessService


class RubertEmbeddingsAnalyzer:
    mode = "RUBERT_EMBEDDINGS"
    topic_markers: dict[Topic, tuple[str, ...]] = {
        Topic.TEACHERS: ("преподав", "оцен", "обратн", "кафедр", "объясн", "провер"),
        Topic.EDUCATION: ("учеб", "дисципл", "программ", "курс", "лекц", "семинар", "экзам", "теори"),
        Topic.DORMITORY: ("общежит", "комнат", "сосед", "кухн", "душ", "комендант", "заселен", "бытов"),
        Topic.INFRASTRUCTURE: ("кампус", "столов", "аудитор", "корпус", "wifi", "библиотек", "здание", "транспорт"),
        Topic.ADMINISTRATION: (
            "деканат",
            "документ",
            "справк",
            "заявлен",
            "бюрократ",
            "расписан",
            "оформлен",
            "комисси",
            "срок",
        ),
        Topic.STUDENT_LIFE: ("мероприят", "клуб", "студенческ", "активност", "событ", "организац", "сообществ"),
        Topic.OTHER: (),
    }
    topic_tie_break_priority: tuple[Topic, ...] = (
        Topic.DORMITORY,
        Topic.ADMINISTRATION,
        Topic.TEACHERS,
        Topic.EDUCATION,
        Topic.STUDENT_LIFE,
        Topic.INFRASTRUCTURE,
    )

    def __init__(
        self,
        preprocess_service: PreprocessService,
        keyword_service: KeywordService,
        model_name: str,
        sentiment_classifier_path: str,
        topic_classifier_path: str,
        metadata_path: str,
        embedder: RubertEmbedder | None = None,
    ) -> None:
        self.preprocess_service = preprocess_service
        self.keyword_service = keyword_service
        self.sentiment_classifier = self._load_artifact(sentiment_classifier_path)
        self.topic_classifier = self._load_artifact(topic_classifier_path)
        self.metadata = self._load_metadata(metadata_path)
        self.embedder = embedder or RubertEmbedder(model_name=model_name)
        self.model_version = str(self.metadata.get("modelVersion", "rubert-embeddings-unknown"))

    def analyze(self, text: str) -> AnalyzeResult:
        return self.analyze_batch([text])[0]

    def analyze_batch(self, texts: list[str]) -> list[AnalyzeResult]:
        normalized_texts = [self.preprocess_service.normalize_text(text) for text in texts]
        embeddings = self.embedder.embed_texts(normalized_texts)

        sentiment_labels = self.sentiment_classifier.predict(embeddings)
        topic_labels = self.topic_classifier.predict(embeddings)
        sentiment_confidence = self._predict_confidence(self.sentiment_classifier, embeddings)
        topic_confidence = self._predict_confidence(self.topic_classifier, embeddings)

        results: list[AnalyzeResult] = []
        for index, normalized_text in enumerate(normalized_texts):
            topic, resolved_topic_confidence = self._resolve_topic(
                normalized_text=normalized_text,
                predicted_topic=Topic(str(topic_labels[index])),
                predicted_confidence=float(topic_confidence[index]),
            )
            confidence = round(
                float(max(0.0, min(1.0, (sentiment_confidence[index] + resolved_topic_confidence) / 2))),
                2,
            )
            results.append(
                AnalyzeResult(
                    sentiment=Sentiment(str(sentiment_labels[index])),
                    topic=topic,
                    keywords=self.keyword_service.extract_keywords(
                        normalized_text,
                        topic=topic,
                        embedder=self.embedder,
                    ),
                    confidence=confidence,
                    modelVersion=self.model_version,
                )
            )
        return results

    def _predict_confidence(self, classifier, embeddings: np.ndarray) -> np.ndarray:
        if hasattr(classifier, "predict_proba"):
            probabilities = np.asarray(classifier.predict_proba(embeddings))
            return np.max(probabilities, axis=1)
        return np.full(shape=(len(embeddings),), fill_value=0.7, dtype=np.float32)

    def _resolve_topic(
        self,
        normalized_text: str,
        predicted_topic: Topic,
        predicted_confidence: float,
    ) -> tuple[Topic, float]:
        token_stems = self._build_token_stems(self.preprocess_service.tokenize(normalized_text))
        scores = {
            topic: self._count_marker_hits(token_stems, markers)
            for topic, markers in self.topic_markers.items()
        }
        concrete_scores = {topic: score for topic, score in scores.items() if topic != Topic.OTHER}
        top_score = max(concrete_scores.values(), default=0)
        if top_score == 0:
            return predicted_topic, predicted_confidence

        top_topics = [topic for topic, score in concrete_scores.items() if score == top_score]
        best_topic = next(
            (topic for topic in self.topic_tie_break_priority if topic in top_topics),
            top_topics[0],
        )
        predicted_score = scores.get(predicted_topic, 0)
        runner_up = max(
            (score for topic, score in concrete_scores.items() if topic != best_topic),
            default=0,
        )
        score_gap = top_score - runner_up

        should_override = False
        if predicted_topic == Topic.OTHER and top_score >= 2:
            should_override = True
        elif predicted_topic == Topic.INFRASTRUCTURE and best_topic in {Topic.DORMITORY, Topic.ADMINISTRATION}:
            should_override = top_score >= max(2, predicted_score + 1)
        elif (
            predicted_topic != best_topic
            and best_topic != Topic.OTHER
            and predicted_confidence <= 0.62
            and top_score >= max(2, predicted_score + 1)
            and score_gap >= 1
        ):
            should_override = True

        if not should_override:
            return predicted_topic, predicted_confidence

        signal_confidence = 0.58 + min(0.22, top_score * 0.08) + min(0.08, score_gap * 0.04)
        return best_topic, round(max(predicted_confidence, min(signal_confidence, 0.9)), 2)

    def _build_token_stems(self, tokens: list[str]) -> list[str]:
        return [token[: max(4, len(token) - 2)] for token in tokens]

    def _count_marker_hits(self, token_stems: list[str], markers: tuple[str, ...]) -> int:
        return sum(1 for stem in token_stems if any(stem.startswith(marker) for marker in markers))

    def _load_artifact(self, path: str):
        artifact_path = Path(path)
        if not artifact_path.exists():
            raise ModelNotFoundError(path)
        return joblib.load(artifact_path)

    def _load_metadata(self, path: str) -> dict[str, object]:
        metadata_path = Path(path)
        if not metadata_path.exists():
            raise ModelNotFoundError(path)
        return json.loads(metadata_path.read_text(encoding="utf-8"))
