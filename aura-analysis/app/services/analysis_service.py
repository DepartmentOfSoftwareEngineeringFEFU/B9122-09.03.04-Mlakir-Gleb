import logging
from typing import Optional

from app.core.config import Settings
from app.domain.enums import Sentiment, Topic
from app.services.analyzers.base import AnalyzeResult, Analyzer
from app.services.analyzers.rubert_embeddings_analyzer import RubertEmbeddingsAnalyzer
from app.services.analyzers.rule_based_analyzer import RuleBasedAnalyzer
from app.services.keyword_service import KeywordService
from app.services.preprocess_service import PreprocessService

logger = logging.getLogger(__name__)


class AnalysisService:
    def __init__(
        self,
        analyzer: Analyzer,
        requested_mode: str,
        degradation_reason: Optional[str] = None,
    ) -> None:
        self.analyzer = analyzer
        self.requested_mode = requested_mode
        self.degradation_reason = degradation_reason

    def analyze(self, text: str) -> AnalyzeResult:
        return self.analyzer.analyze(text)

    def analyze_batch(self, texts: list[str]) -> list[AnalyzeResult]:
        if hasattr(self.analyzer, "analyze_batch"):
            return self.analyzer.analyze_batch(texts)
        return [self.analyze(text) for text in texts]

    @property
    def mode(self) -> str:
        return self.analyzer.mode

    @property
    def model_version(self) -> str:
        return self.analyzer.model_version

    @property
    def degraded(self) -> bool:
        return self.degradation_reason is not None

    def info(self) -> dict[str, object]:
        return {
            "mode": self.mode,
            "requestedMode": self.requested_mode,
            "modelVersion": self.model_version,
            "degraded": self.degraded,
            "degradationReason": self.degradation_reason,
            "supportedSentiments": list(Sentiment),
            "supportedTopics": list(Topic),
        }


def build_analysis_service(settings: Settings) -> AnalysisService:
    preprocess_service = PreprocessService()
    keyword_service = KeywordService(preprocess_service=preprocess_service)
    fallback_analyzer = RuleBasedAnalyzer(
        preprocess_service=preprocess_service,
        keyword_service=keyword_service,
        model_version=settings.model_version,
    )

    if settings.analysis_mode == "RUBERT_EMBEDDINGS":
        try:
            logger.info("Loading RuBERT embeddings artifacts model=%s", settings.rubert_model_name)
            analyzer = RubertEmbeddingsAnalyzer(
                preprocess_service=preprocess_service,
                keyword_service=keyword_service,
                model_name=settings.rubert_model_name,
                sentiment_classifier_path=settings.rubert_sentiment_classifier_path,
                topic_classifier_path=settings.rubert_topic_classifier_path,
                metadata_path=settings.rubert_metadata_path,
            )
            return AnalysisService(
                analyzer=analyzer,
                requested_mode=settings.analysis_mode,
            )
        except Exception as exc:
            logger.warning("Failed to load RuBERT artifacts, falling back to rule-based mode: %s", exc)
            return AnalysisService(
                analyzer=fallback_analyzer,
                requested_mode=settings.analysis_mode,
                degradation_reason=(
                    "Requested RUBERT_EMBEDDINGS mode is unavailable; "
                    f"rule-based fallback activated ({exc.__class__.__name__})."
                ),
            )

    return AnalysisService(
        analyzer=fallback_analyzer,
        requested_mode=settings.analysis_mode,
    )
