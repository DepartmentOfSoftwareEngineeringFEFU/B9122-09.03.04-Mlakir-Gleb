from abc import ABC, abstractmethod

from app.services.analysis_result import TopicResult


class BaseTopicAnalyzer(ABC):
    @abstractmethod
    def analyze(self, text: str) -> TopicResult:
        raise NotImplementedError
