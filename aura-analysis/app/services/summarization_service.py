from dataclasses import dataclass
from html import unescape
import logging
import re
from typing import Optional

from app.core.config import Settings
from app.services.gemini_client import GeminiClient, GeminiClientError


logger = logging.getLogger(__name__)

FALLBACK_MODEL_VERSION = "fallback-extractive-0.2.0"


@dataclass(frozen=True)
class SummaryResult:
    summary: str
    modelVersion: str


class SummarizationService:
    _control_chars_pattern = re.compile(r"[\x00-\x1f\x7f]+")
    _spaces_pattern = re.compile(r"\s+")
    _sentence_split_pattern = re.compile(r"(?<=[.!?])\s+")

    def __init__(
        self,
        gemini_client: Optional[GeminiClient],
        model_version: str,
        max_input_length: int,
        max_output_chars: int,
    ) -> None:
        self.gemini_client = gemini_client
        self.model_version = model_version
        self.max_input_length = max_input_length
        self.max_output_chars = max_output_chars

    def summarize(self, text: str) -> SummaryResult:
        normalized_text = self._normalize_input(text)
        truncated_text = self._trim_to_limit(normalized_text, self.max_input_length)

        if self.gemini_client is None:
            logger.info("Summary fallback used because Gemini API key is not configured")
            return self._build_fallback_result(truncated_text)

        try:
            summary = self.gemini_client.summarize(truncated_text)
            cleaned_summary = self._clean_summary(summary)
            if not cleaned_summary:
                raise GeminiClientError("Gemini returned empty summary")
            final_summary = self._trim_to_limit(cleaned_summary, self.max_output_chars)
            return SummaryResult(summary=final_summary, modelVersion=self.model_version)
        except GeminiClientError as exc:
            logger.warning("Summary fallback used because Gemini failed: %s", exc)
            return self._build_fallback_result(truncated_text)
        except Exception as exc:
            logger.warning(
                "Summary fallback used because of unexpected summarization error: %s",
                exc.__class__.__name__,
            )
            return self._build_fallback_result(truncated_text)

    def close(self) -> None:
        if self.gemini_client is not None:
            self.gemini_client.close()

    def _build_fallback_result(self, text: str) -> SummaryResult:
        fallback_summary = self._extractive_fallback(text)
        return SummaryResult(summary=fallback_summary, modelVersion=FALLBACK_MODEL_VERSION)

    def _normalize_input(self, text: str) -> str:
        normalized = unescape(text)
        normalized = normalized.replace("ё", "е")
        normalized = self._control_chars_pattern.sub(" ", normalized)
        normalized = self._spaces_pattern.sub(" ", normalized).strip()
        return normalized

    def _clean_summary(self, summary: str) -> str:
        cleaned = summary.strip()
        if cleaned.startswith("```") and cleaned.endswith("```"):
            cleaned = cleaned[3:-3].strip()
        cleaned = re.sub(r"^```[a-zA-Z0-9_-]*\s*", "", cleaned).strip()
        cleaned = re.sub(r"\s*```$", "", cleaned).strip()
        cleaned = cleaned.strip("`").strip()

        for _ in range(2):
            if len(cleaned) >= 2 and cleaned[0] == cleaned[-1] and cleaned[0] in {'"', "'", "«", "»"}:
                cleaned = cleaned[1:-1].strip()

        cleaned = re.sub(r"^\s*[-*]\s*", "", cleaned).strip()
        cleaned = self._spaces_pattern.sub(" ", cleaned)
        return cleaned

    def _extractive_fallback(self, text: str) -> str:
        sentences = [sentence.strip() for sentence in self._sentence_split_pattern.split(text) if sentence.strip()]
        selected: list[str] = []

        for sentence in sentences:
            if self._is_content_sentence(sentence):
                selected.append(sentence)
            if len(selected) == 3:
                break

        if len(selected) < 2:
            for sentence in sentences:
                if sentence not in selected:
                    selected.append(sentence)
                if len(selected) == 3:
                    break

        summary = " ".join(selected[:3]).strip()
        if not summary:
            summary = text.strip()
        if not summary:
            summary = "Отзыв не содержит достаточно данных для краткого конспекта."

        return self._trim_to_limit(summary, self.max_output_chars)

    def _is_content_sentence(self, sentence: str) -> bool:
        words = [word for word in re.findall(r"[A-Za-zА-Яа-яЁё0-9-]+", sentence) if len(word) >= 3]
        return len(words) >= 3

    def _trim_to_limit(self, text: str, limit: int) -> str:
        if len(text) <= limit:
            return text

        if limit <= 3:
            return text[:limit]

        candidate = text[:limit].rstrip()
        sentence_end = max(candidate.rfind(". "), candidate.rfind("! "), candidate.rfind("? "))
        if sentence_end >= max(0, limit // 2):
            return candidate[: sentence_end + 1].rstrip()

        whitespace = candidate.rfind(" ")
        if whitespace >= max(0, limit // 2):
            return candidate[:whitespace].rstrip(" ,;:-") + "..."

        return candidate[:-3].rstrip(" ,;:-") + "..."


def build_summarization_service(settings: Settings) -> SummarizationService:
    gemini_client = None
    if settings.google_ai_api_key.strip():
        gemini_client = GeminiClient(
            api_key=settings.google_ai_api_key,
            api_url=settings.google_ai_url,
            model=settings.google_ai_model,
        )

    return SummarizationService(
        gemini_client=gemini_client,
        model_version=settings.google_ai_model,
        max_input_length=settings.summary_max_input_length,
        max_output_chars=settings.summary_max_output_chars,
    )
