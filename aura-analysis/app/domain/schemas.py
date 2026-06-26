from __future__ import annotations

from typing import Annotated
from typing import Optional

from pydantic import BaseModel, ConfigDict, Field, StringConstraints
from pydantic import field_validator

from app.domain.enums import Sentiment, Topic

NonEmptyText = Annotated[
    str,
    StringConstraints(strip_whitespace=True, min_length=1, max_length=10000),
]
SummarizeText = Annotated[
    str,
    StringConstraints(strip_whitespace=True, min_length=1, max_length=20000),
]
InsightReviewText = Annotated[
    str,
    StringConstraints(strip_whitespace=True, min_length=1, max_length=1500),
]


class StrictBaseModel(BaseModel):
    model_config = ConfigDict(extra="forbid", strict=True, use_enum_values=False)


class AnalyzeRequest(StrictBaseModel):
    text: NonEmptyText


class SummarizeRequest(StrictBaseModel):
    text: SummarizeText


class InsightReviewItem(StrictBaseModel):
    text: InsightReviewText
    sentiment: Sentiment
    topic: Topic

    @field_validator("sentiment", mode="before")
    @classmethod
    def parse_sentiment(cls, value: Sentiment | str) -> Sentiment:
        if isinstance(value, Sentiment):
            return value
        return Sentiment(str(value).strip().upper())

    @field_validator("topic", mode="before")
    @classmethod
    def parse_topic(cls, value: Topic | str) -> Topic:
        if isinstance(value, Topic):
            return value
        return Topic(str(value).strip().upper())


class InsightsRequest(StrictBaseModel):
    organizationName: Annotated[str, StringConstraints(strip_whitespace=True, min_length=1, max_length=200)]
    reviews: list[InsightReviewItem] = Field(min_length=1, max_length=100)


class AnalyzeResponse(StrictBaseModel):
    sentiment: Sentiment
    topic: Topic
    keywords: list[str] = Field(min_length=1, max_length=5)
    confidence: float = Field(ge=0.0, le=1.0)
    modelVersion: str = Field(min_length=1)


class BatchAnalyzeItemRequest(StrictBaseModel):
    text: NonEmptyText


class BatchAnalyzeRequest(StrictBaseModel):
    items: list[BatchAnalyzeItemRequest] = Field(min_length=1, max_length=1000)


class BatchAnalyzeItemResponse(StrictBaseModel):
    sentiment: Sentiment
    topic: Topic
    keywords: list[str] = Field(min_length=1, max_length=5)
    confidence: float = Field(ge=0.0, le=1.0)
    modelVersion: str = Field(min_length=1)


class BatchAnalyzeResponse(StrictBaseModel):
    items: list[BatchAnalyzeItemResponse] = Field(min_length=1, max_length=1000)


class HealthResponse(StrictBaseModel):
    status: str
    mode: str
    requestedMode: Optional[str] = None
    degraded: bool = False
    degradationReason: Optional[str] = None
    modelVersion: str = Field(min_length=1)


class InfoResponse(StrictBaseModel):
    mode: str
    requestedMode: Optional[str] = None
    degraded: bool = False
    degradationReason: Optional[str] = None
    modelVersion: str = Field(min_length=1)
    supportedSentiments: list[Sentiment]
    supportedTopics: list[Topic]


class SummarizeResponse(StrictBaseModel):
    summary: str = Field(min_length=1)
    modelVersion: str = Field(min_length=1)


class InsightsResponse(StrictBaseModel):
    summary: str = Field(min_length=1)
    strengths: list[str] = Field(min_length=1, max_length=5)
    weaknesses: list[str] = Field(min_length=1, max_length=5)
    recommendations: list[str] = Field(min_length=1, max_length=5)
    modelVersion: str = Field(min_length=1)
