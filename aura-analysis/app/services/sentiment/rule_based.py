from typing import Optional

from app.domain.enums import Sentiment
from app.services.analysis_result import SentimentResult
from app.services.sentiment.base import BaseSentimentAnalyzer


class RuleBasedSentimentAnalyzer(BaseSentimentAnalyzer):
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

    def analyze(self, text: str) -> SentimentResult:
        positive_hits = self._count_hits(text, self.positive_markers)
        negative_hits = self._count_hits(text, self.negative_markers)
        total_hits = positive_hits + negative_hits

        if positive_hits > negative_hits:
            confidence = self._calculate_confidence(positive_hits, negative_hits, total_hits)
            return SentimentResult(sentiment=Sentiment.POSITIVE, confidence=confidence)

        if negative_hits > positive_hits:
            confidence = self._calculate_confidence(negative_hits, positive_hits, total_hits)
            return SentimentResult(sentiment=Sentiment.NEGATIVE, confidence=confidence)

        contrast_sentiment = self._analyze_contrast_clause(text)
        if contrast_sentiment is not None:
            return contrast_sentiment

        if total_hits > 0:
            return SentimentResult(sentiment=Sentiment.NEUTRAL, confidence=0.6)

        return SentimentResult(sentiment=Sentiment.NEUTRAL, confidence=0.55)

    def _count_hits(self, text: str, markers: tuple[str, ...]) -> int:
        return sum(text.count(marker) for marker in markers)

    def _calculate_confidence(self, dominant_hits: int, other_hits: int, total_hits: int) -> float:
        margin = dominant_hits - other_hits
        confidence = 0.55 + min(0.3, dominant_hits * 0.08) + min(0.1, margin * 0.05)
        if total_hits >= 4:
            confidence += 0.05
        return round(min(confidence, 0.95), 2)

    def _analyze_contrast_clause(self, text: str) -> Optional[SentimentResult]:
        if " но " not in text:
            return None

        _, _, tail = text.partition(" но ")
        tail_positive_hits = self._count_hits(tail, self.positive_markers)
        tail_negative_hits = self._count_hits(tail, self.negative_markers)

        if tail_negative_hits > tail_positive_hits:
            return SentimentResult(sentiment=Sentiment.NEGATIVE, confidence=0.72)
        if tail_positive_hits > tail_negative_hits:
            return SentimentResult(sentiment=Sentiment.POSITIVE, confidence=0.72)
        return None
