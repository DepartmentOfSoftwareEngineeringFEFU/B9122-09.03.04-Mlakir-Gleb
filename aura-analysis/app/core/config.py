from functools import lru_cache
from typing import Literal

from pydantic import Field
from pydantic import field_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = Field(default="aura-analysis-service", alias="APP_NAME")
    app_port: int = Field(default=8090, alias="APP_PORT")
    analysis_mode: Literal["RULE_BASED", "RUBERT_EMBEDDINGS"] = Field(
        default="RUBERT_EMBEDDINGS",
        alias="ANALYSIS_MODE",
    )
    model_version: str = Field(
        default="rule-based-0.1.0",
        alias="MODEL_VERSION",
    )
    rubert_model_name: str = Field(
        default="cointegrated/rubert-tiny2",
        alias="RUBERT_MODEL_NAME",
    )
    rubert_sentiment_classifier_path: str = Field(
        default="app/ml/artifacts/rubert_sentiment_classifier.joblib",
        alias="RUBERT_SENTIMENT_CLASSIFIER_PATH",
    )
    rubert_topic_classifier_path: str = Field(
        default="app/ml/artifacts/rubert_topic_classifier.joblib",
        alias="RUBERT_TOPIC_CLASSIFIER_PATH",
    )
    rubert_metadata_path: str = Field(
        default="app/ml/artifacts/rubert_metadata.json",
        alias="RUBERT_METADATA_PATH",
    )
    google_ai_api_key: str = Field(default="", alias="GOOGLE_AI_API_KEY")
    google_ai_url: str = Field(
        default="https://generativelanguage.googleapis.com/v1beta/models",
        alias="GOOGLE_AI_URL",
    )
    google_ai_model: str = Field(
        default="gemini-2.5-flash-lite",
        alias="GOOGLE_AI_MODEL",
    )
    summary_max_input_length: int = Field(default=20000, alias="SUMMARY_MAX_INPUT_LENGTH")
    summary_max_output_chars: int = Field(default=700, alias="SUMMARY_MAX_OUTPUT_CHARS")
    insights_max_input_chars: int = Field(default=12000, alias="INSIGHTS_MAX_INPUT_CHARS")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=False,
        populate_by_name=True,
        extra="ignore",
    )

    @field_validator("analysis_mode", mode="before")
    @classmethod
    def normalize_analysis_mode(cls, value: str) -> str:
        if isinstance(value, str):
            normalized = value.strip().replace("-", "_").upper()
            mode_aliases = {
                "RULE_BASED": "RULE_BASED",
                "RUBERT": "RUBERT_EMBEDDINGS",
                "RUBERT_EMBEDDINGS": "RUBERT_EMBEDDINGS",
            }
            return mode_aliases.get(normalized, normalized)
        return value


@lru_cache(maxsize=1)
def get_settings() -> Settings:
    return Settings()
