from typing import Optional

from app.domain.enums import Sentiment, Topic
from app.services.analyzers.base import AnalyzeResult
from app.services.keyword_service import KeywordService
from app.services.preprocess_service import PreprocessService


class RuleBasedAnalyzer:
    mode = "RULE_BASED"
    default_model_version = "rule-based-0.1.0"

    positive_markers: tuple[str, ...] = (
        "отличн",
        "хорош",
        "супер",
        "нрав",
        "удобн",
        "прекрасн",
        "интересн",
        "классн",
        "качествен",
    )
    negative_markers: tuple[str, ...] = (
        "ужас",
        "плох",
        "проблем",
        "хам",
        "неудоб",
        "гряз",
        "долго",
        "сложно",
        "кошмар",
        "отврат",
        "бюрократ",
    )
    topic_markers: dict[Topic, tuple[str, ...]] = {
        Topic.TEACHERS: ("преподавател",),
        Topic.EDUCATION: ("лекц", "пара", "семинар", "курс", "предмет", "обучен", "экзамен", "зачет"),
        Topic.DORMITORY: ("общежит", "комната", "сосед", "кухн", "душ", "комендант"),
        Topic.INFRASTRUCTURE: ("кампус", "столов", "аудитор", "корпус", "wifi", "библиотек", "здание"),
        Topic.ADMINISTRATION: ("деканат", "документ", "справк", "заявлен", "бюрократ", "расписан", "оформлен"),
        Topic.STUDENT_LIFE: ("мероприят", "клуб", "студенческ", "активност", "событ", "организац"),
    }
    tie_break_priority: tuple[Topic, ...] = (
        Topic.DORMITORY,
        Topic.ADMINISTRATION,
        Topic.INFRASTRUCTURE,
        Topic.STUDENT_LIFE,
        Topic.TEACHERS,
        Topic.EDUCATION,
    )

    def __init__(
        self,
        preprocess_service: PreprocessService,
        keyword_service: KeywordService,
        model_version: str = default_model_version,
    ) -> None:
        self.preprocess_service = preprocess_service
        self.keyword_service = keyword_service
        self.model_version = model_version

    def analyze(self, text: str) -> AnalyzeResult:
        normalized_text = self.preprocess_service.normalize_text(text)
        tokens = self.preprocess_service.tokenize(normalized_text)
        token_stems = self._build_token_stems(tokens)

        sentiment, sentiment_confidence = self._analyze_sentiment(normalized_text, token_stems)
        topic, topic_confidence = self._analyze_topic(token_stems)
        keywords = self.keyword_service.extract_keywords(normalized_text)
        confidence = round(max(0.0, min(1.0, (sentiment_confidence + topic_confidence) / 2)), 2)

        return AnalyzeResult(
            sentiment=sentiment,
            topic=topic,
            keywords=keywords,
            confidence=confidence,
            modelVersion=self.model_version,
        )

    def _build_token_stems(self, tokens: list[str]) -> list[str]:
        return [token[: max(4, len(token) - 2)] for token in tokens]

    def _count_marker_hits(self, token_stems: list[str], markers: tuple[str, ...]) -> int:
        return sum(1 for stem in token_stems if any(stem.startswith(marker) for marker in markers))

    def _analyze_sentiment(self, text: str, token_stems: list[str]) -> tuple[Sentiment, float]:
        positive_hits = self._count_marker_hits(token_stems, self.positive_markers)
        negative_hits = self._count_marker_hits(token_stems, self.negative_markers)
        total_hits = positive_hits + negative_hits

        if positive_hits > negative_hits:
            return Sentiment.POSITIVE, self._calculate_confidence(positive_hits, negative_hits, total_hits)

        if negative_hits > positive_hits:
            return Sentiment.NEGATIVE, self._calculate_confidence(negative_hits, positive_hits, total_hits)

        contrast_sentiment = self._analyze_contrast_clause(text)
        if contrast_sentiment is not None:
            return contrast_sentiment

        if total_hits > 0:
            return Sentiment.NEUTRAL, 0.6

        return Sentiment.NEUTRAL, 0.55

    def _analyze_contrast_clause(self, text: str) -> Optional[tuple[Sentiment, float]]:
        if " но " not in text:
            return None

        _, _, tail = text.partition(" но ")
        tail_tokens = self.preprocess_service.tokenize(tail)
        tail_token_stems = self._build_token_stems(tail_tokens)
        tail_positive_hits = self._count_marker_hits(tail_token_stems, self.positive_markers)
        tail_negative_hits = self._count_marker_hits(tail_token_stems, self.negative_markers)

        if tail_negative_hits > tail_positive_hits:
            return Sentiment.NEGATIVE, 0.72
        if tail_positive_hits > tail_negative_hits:
            return Sentiment.POSITIVE, 0.72
        return None

    def _analyze_topic(self, token_stems: list[str]) -> tuple[Topic, float]:
        scores = {
            topic: self._count_marker_hits(token_stems, markers)
            for topic, markers in self.topic_markers.items()
        }
        top_score = max(scores.values())

        if top_score == 0:
            return Topic.OTHER, 0.55

        top_topics = [topic for topic, score in scores.items() if score == top_score]
        top_topic = next(
            (topic for topic in self.tie_break_priority if topic in top_topics),
            top_topics[0],
        )
        sorted_scores = sorted(scores.values(), reverse=True)
        runner_up = sorted_scores[1] if len(sorted_scores) > 1 else 0
        confidence = 0.6 + min(0.2, top_score * 0.08) + min(0.1, (top_score - runner_up) * 0.05)
        return top_topic, round(min(confidence, 0.95), 2)

    def _calculate_confidence(self, dominant_hits: int, other_hits: int, total_hits: int) -> float:
        margin = dominant_hits - other_hits
        confidence = 0.55 + min(0.3, dominant_hits * 0.08) + min(0.1, margin * 0.05)
        if total_hits >= 4:
            confidence += 0.05
        return round(min(confidence, 0.95), 2)
