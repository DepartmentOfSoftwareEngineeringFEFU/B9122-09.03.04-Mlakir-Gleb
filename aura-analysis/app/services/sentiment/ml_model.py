from pathlib import Path

import joblib
import numpy as np

from app.core.exceptions import ModelNotFoundError
from app.domain.enums import Sentiment
from app.services.analysis_result import SentimentResult
from app.services.sentiment.base import BaseSentimentAnalyzer


class MlSentimentAnalyzer(BaseSentimentAnalyzer):
    def __init__(self, model_path: str) -> None:
        artifact_path = Path(model_path)
        if not artifact_path.exists():
            raise ModelNotFoundError(model_path)
        self.model = joblib.load(artifact_path)

    def analyze(self, text: str) -> SentimentResult:
        predicted_label = self.model.predict([text])[0]
        confidence = self._extract_confidence(text)
        return SentimentResult(sentiment=Sentiment(predicted_label), confidence=confidence)

    def _extract_confidence(self, text: str) -> float:
        if hasattr(self.model, "predict_proba"):
            probabilities = self.model.predict_proba([text])[0]
            return round(float(np.max(probabilities)), 2)
        if hasattr(self.model, "decision_function"):
            raw_scores = np.asarray(self.model.decision_function([text])).reshape(-1)
            normalized = 1.0 / (1.0 + np.exp(-np.max(np.abs(raw_scores))))
            return round(float(max(0.55, min(0.95, normalized))), 2)
        return 0.7
