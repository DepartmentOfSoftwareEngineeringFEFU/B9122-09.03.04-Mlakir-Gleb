from abc import ABC, abstractmethod

from app.services.analysis_result import SentimentResult


class BaseSentimentAnalyzer(ABC):
    @abstractmethod
    def analyze(self, text: str) -> SentimentResult:
        raise NotImplementedError
