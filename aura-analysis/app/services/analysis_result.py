from dataclasses import dataclass

from app.domain.enums import Sentiment, Topic


@dataclass(frozen=True)
class SentimentResult:
    sentiment: Sentiment
    confidence: float


@dataclass(frozen=True)
class TopicResult:
    topic: Topic
    confidence: float


@dataclass(frozen=True)
class AnalysisResult:
    sentiment: Sentiment
    topic: Topic
    keywords: list[str]
    confidence: float
    modelVersion: str
