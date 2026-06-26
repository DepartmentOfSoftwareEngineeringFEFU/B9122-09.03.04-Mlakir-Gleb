from dataclasses import dataclass
from typing import Protocol

from app.domain.enums import Sentiment, Topic


@dataclass(frozen=True)
class AnalyzeResult:
    sentiment: Sentiment
    topic: Topic
    keywords: list[str]
    confidence: float
    modelVersion: str


class Analyzer(Protocol):
    model_version: str
    mode: str

    def analyze(self, text: str) -> AnalyzeResult:
        ...
