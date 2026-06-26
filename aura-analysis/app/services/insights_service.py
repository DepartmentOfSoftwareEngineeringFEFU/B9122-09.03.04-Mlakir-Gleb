from __future__ import annotations

from collections import Counter
from dataclasses import dataclass
from html import unescape
import json
import logging
import re
from typing import Optional

from app.core.config import Settings
from app.domain.enums import Sentiment, Topic
from app.domain.schemas import InsightReviewItem
from app.services.gemini_client import GeminiClient, GeminiClientError


logger = logging.getLogger(__name__)

FALLBACK_INSIGHTS_MODEL_VERSION = "fallback-insights-0.1.0"
MAX_REVIEW_TEXT_CHARS = 1500
MAX_LIST_ITEMS = 5

TOPIC_LABELS = {
    Topic.EDUCATION: "образовательный процесс",
    Topic.TEACHERS: "преподаватели",
    Topic.INFRASTRUCTURE: "инфраструктура",
    Topic.DORMITORY: "общежитие",
    Topic.ADMINISTRATION: "администрация",
    Topic.STUDENT_LIFE: "студенческая жизнь",
    Topic.OTHER: "прочие аспекты",
}

TOPIC_RECOMMENDATIONS = {
    Topic.EDUCATION: "Пересмотреть содержание программ, нагрузку и организацию учебного процесса.",
    Topic.TEACHERS: "Усилить практики обратной связи и качество взаимодействия преподавателей со студентами.",
    Topic.INFRASTRUCTURE: "Приоритизировать улучшение инфраструктуры и бытовых условий в корпусах.",
    Topic.DORMITORY: "Отдельно проработать качество проживания и базовые условия в общежитии.",
    Topic.ADMINISTRATION: "Сократить бюрократические задержки и упростить административные процессы.",
    Topic.STUDENT_LIFE: "Поддержать развитие внеучебных активностей и студенческих инициатив.",
    Topic.OTHER: "Собрать дополнительные отзывы по менее очевидным проблемным зонам.",
}


def build_insights_prompt(organization_name: str, reviews_block: str) -> str:
    return (
        f'Проанализируй отзывы об организации "{organization_name}".\n\n'
        "На основе отзывов выдели:\n"
        "1. Краткую общую сводку;\n"
        "2. Сильные стороны организации;\n"
        "3. Слабые стороны и проблемы;\n"
        "4. Рекомендации по улучшению.\n\n"
        "Требования:\n"
        "- отвечай только на русском языке;\n"
        "- не придумывай факты;\n"
        "- опирайся только на переданные отзывы;\n"
        "- не цитируй отзывы полностью;\n"
        "- формулируй кратко и по делу;\n"
        "- strengths: 3–5 пунктов;\n"
        "- weaknesses: 3–5 пунктов;\n"
        "- recommendations: 3–5 пунктов;\n"
        "- ответ верни строго в JSON.\n\n"
        "Формат JSON:\n"
        "{\n"
        '  "summary": "...",\n'
        '  "strengths": ["..."],\n'
        '  "weaknesses": ["..."],\n'
        '  "recommendations": ["..."]\n'
        "}\n\n"
        f"Отзывы:\n{reviews_block}"
    )


def build_compact_insights_prompt(organization_name: str, reviews_block: str) -> str:
    return (
        f'Организация: "{organization_name}".\n'
        "Верни только JSON с полями summary, strengths, weaknesses, recommendations.\n"
        "Пиши по-русски, кратко, без выдуманных фактов.\n"
        f"Отзывы:\n{reviews_block}"
    )


@dataclass(frozen=True)
class InsightsResult:
    summary: str
    strengths: list[str]
    weaknesses: list[str]
    recommendations: list[str]
    modelVersion: str


class InsightsService:
    _control_chars_pattern = re.compile(r"[\x00-\x1f\x7f]+")
    _spaces_pattern = re.compile(r"\s+")

    def __init__(
        self,
        gemini_client: Optional[GeminiClient],
        model_version: str,
        max_input_chars: int,
    ) -> None:
        self.gemini_client = gemini_client
        self.model_version = model_version
        self.max_input_chars = max_input_chars

    def build_insights(self, organization_name: str, reviews: list[InsightReviewItem]) -> InsightsResult:
        normalized_name = self._normalize_text(organization_name)
        prepared_reviews = [self._prepare_review(review) for review in reviews]

        if self.gemini_client is None:
            logger.info("Insights fallback used because Gemini API key is not configured")
            return self._build_fallback_result(prepared_reviews)

        try:
            prompt = self._build_prompt(normalized_name, prepared_reviews)
            response_text = self.gemini_client.generate(prompt)
            parsed = self._parse_gemini_json(response_text)
            return InsightsResult(
                summary=parsed["summary"],
                strengths=parsed["strengths"],
                weaknesses=parsed["weaknesses"],
                recommendations=parsed["recommendations"],
                modelVersion=self.model_version,
            )
        except GeminiClientError as exc:
            logger.warning("Insights fallback used because Gemini failed: %s", exc)
            return self._build_fallback_result(prepared_reviews)
        except (KeyError, TypeError, ValueError, json.JSONDecodeError) as exc:
            logger.warning("Insights fallback used because Gemini response is invalid: %s", exc.__class__.__name__)
            return self._build_fallback_result(prepared_reviews)
        except Exception as exc:
            logger.warning("Insights fallback used because of unexpected insights error: %s", exc.__class__.__name__)
            return self._build_fallback_result(prepared_reviews)

    def close(self) -> None:
        if self.gemini_client is not None:
            self.gemini_client.close()

    def _prepare_review(self, review: InsightReviewItem) -> InsightReviewItem:
        text = self._trim_to_limit(self._normalize_text(review.text), MAX_REVIEW_TEXT_CHARS)
        return InsightReviewItem(text=text, sentiment=review.sentiment, topic=review.topic)

    def _build_reviews_block(self, reviews: list[InsightReviewItem]) -> str:
        lines = [
            f"{index}. [{review.sentiment.value}, {review.topic.value}] {review.text}"
            for index, review in enumerate(reviews, start=1)
        ]
        return "\n".join(lines)

    def _build_prompt(self, organization_name: str, reviews: list[InsightReviewItem]) -> str:
        prompt_builder = build_insights_prompt
        if len(prompt_builder(organization_name, "")) >= self.max_input_chars:
            prompt_builder = build_compact_insights_prompt

        prompt_reviews = self._select_reviews_for_prompt(organization_name, reviews, prompt_builder)
        reviews_block = self._build_reviews_block(prompt_reviews)
        prompt = prompt_builder(organization_name, reviews_block)
        if len(prompt) <= self.max_input_chars:
            return prompt

        available_chars = max(1, self.max_input_chars - len(prompt_builder(organization_name, "")))
        trimmed_reviews_block = self._trim_to_limit(reviews_block, available_chars)
        return prompt_builder(organization_name, trimmed_reviews_block)

    def _select_reviews_for_prompt(
        self,
        organization_name: str,
        reviews: list[InsightReviewItem],
        prompt_builder,
    ) -> list[InsightReviewItem]:
        if not reviews:
            return []

        prompt_overhead = len(prompt_builder(organization_name, ""))
        available_chars = max(1, self.max_input_chars - prompt_overhead)
        selected_reviews: list[InsightReviewItem] = []
        used_chars = 0

        for review in reviews:
            prefix = f"{len(selected_reviews) + 1}. [{review.sentiment.value}, {review.topic.value}] "
            separator_length = 1 if selected_reviews else 0
            full_line_length = separator_length + len(prefix) + len(review.text)

            if not selected_reviews and full_line_length > available_chars:
                text_budget = max(1, available_chars - len(prefix))
                trimmed_text = self._trim_to_limit(review.text, text_budget)
                selected_reviews.append(
                    InsightReviewItem(
                        text=trimmed_text,
                        sentiment=review.sentiment,
                        topic=review.topic,
                    )
                )
                used_chars = len(prefix) + len(trimmed_text)
                break

            if used_chars + full_line_length > available_chars:
                break

            selected_reviews.append(review)
            used_chars += full_line_length

        if len(selected_reviews) < len(reviews):
            logger.info(
                "Insights prompt truncated reviews selected=%s total=%s maxInputChars=%s",
                len(selected_reviews),
                len(reviews),
                self.max_input_chars,
            )

        return selected_reviews or [reviews[0]]

    def _parse_gemini_json(self, response_text: str) -> dict[str, object]:
        cleaned = response_text.strip()
        cleaned = re.sub(r"^```(?:json)?\s*", "", cleaned, flags=re.IGNORECASE).strip()
        cleaned = re.sub(r"\s*```$", "", cleaned).strip()

        payload = json.loads(cleaned)
        summary = self._ensure_non_empty_string(payload.get("summary"))
        strengths = self._normalize_items(payload.get("strengths"))
        weaknesses = self._normalize_items(payload.get("weaknesses"))
        recommendations = self._normalize_items(payload.get("recommendations"))
        return {
            "summary": summary,
            "strengths": strengths,
            "weaknesses": weaknesses,
            "recommendations": recommendations,
        }

    def _build_fallback_result(self, reviews: list[InsightReviewItem]) -> InsightsResult:
        positive_topics = Counter(review.topic for review in reviews if review.sentiment == Sentiment.POSITIVE)
        negative_topics = Counter(review.topic for review in reviews if review.sentiment == Sentiment.NEGATIVE)

        strengths = self._build_strengths(positive_topics)
        weaknesses = self._build_weaknesses(negative_topics)
        recommendations = self._build_recommendations(negative_topics)

        return InsightsResult(
            summary="Отчёт сформирован на основе локальной статистики отзывов.",
            strengths=strengths,
            weaknesses=weaknesses,
            recommendations=recommendations,
            modelVersion=FALLBACK_INSIGHTS_MODEL_VERSION,
        )

    def _build_strengths(self, positive_topics: Counter[Topic]) -> list[str]:
        if not positive_topics:
            return ["Позитивных сигналов недостаточно для уверенного выделения сильных сторон."]
        return [
            f"Положительно оценивается аспект «{TOPIC_LABELS[topic]}»."
            for topic, _ in positive_topics.most_common(MAX_LIST_ITEMS)
        ]

    def _build_weaknesses(self, negative_topics: Counter[Topic]) -> list[str]:
        if not negative_topics:
            return ["Выраженных проблемных зон по локальной статистике не выявлено."]
        return [
            f"Наиболее заметные проблемы связаны с аспектом «{TOPIC_LABELS[topic]}»."
            for topic, _ in negative_topics.most_common(MAX_LIST_ITEMS)
        ]

    def _build_recommendations(self, negative_topics: Counter[Topic]) -> list[str]:
        if not negative_topics:
            return ["Продолжать мониторинг отзывов и отслеживать изменения тональности по ключевым темам."]
        return [
            TOPIC_RECOMMENDATIONS[topic]
            for topic, _ in negative_topics.most_common(MAX_LIST_ITEMS)
        ]

    def _normalize_items(self, values: object) -> list[str]:
        if not isinstance(values, list):
            raise TypeError("Expected list in Gemini insights response")
        items = [self._ensure_non_empty_string(value) for value in values]
        limited_items = items[:MAX_LIST_ITEMS]
        if not limited_items:
            raise ValueError("Gemini returned empty list")
        return limited_items

    def _ensure_non_empty_string(self, value: object) -> str:
        if not isinstance(value, str):
            raise TypeError("Expected string in Gemini insights response")
        normalized = self._normalize_text(value)
        if not normalized:
            raise ValueError("Gemini returned empty string")
        return normalized

    def _normalize_text(self, text: str) -> str:
        normalized = unescape(text)
        normalized = normalized.replace("ё", "е")
        normalized = self._control_chars_pattern.sub(" ", normalized)
        normalized = self._spaces_pattern.sub(" ", normalized).strip()
        return normalized

    def _trim_to_limit(self, text: str, limit: int) -> str:
        if len(text) <= limit:
            return text
        if limit <= 3:
            return text[:limit]
        return text[: limit - 3].rstrip(" ,;:-") + "..."


def build_insights_service(settings: Settings) -> InsightsService:
    gemini_client = None
    if settings.google_ai_api_key.strip():
        gemini_client = GeminiClient(
            api_key=settings.google_ai_api_key,
            api_url=settings.google_ai_url,
            model=settings.google_ai_model,
        )

    return InsightsService(
        gemini_client=gemini_client,
        model_version=settings.google_ai_model,
        max_input_chars=settings.insights_max_input_chars,
    )
