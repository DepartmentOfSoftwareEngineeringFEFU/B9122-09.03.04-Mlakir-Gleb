import logging
from typing import Optional

import httpx


logger = logging.getLogger(__name__)


class _HttpxClient(httpx.Client):
    pass


def build_summary_prompt(review_text: str) -> str:
    return (
        "Сделай краткий конспект отзыва об университете.\n"
        "Требования:\n"
        "- 1–3 предложения;\n"
        "- передай главные факты, проблемы или плюсы;\n"
        "- не добавляй ничего от себя;\n"
        "- не придумывай факты;\n"
        "- избегай общих фраз;\n"
        "- сохрани тональность автора;\n"
        "- пиши кратко и по делу.\n\n"
        "Текст отзыва:\n"
        '"""\n'
        f"{review_text}\n"
        '"""'
    )


class GeminiClientError(Exception):
    def __init__(self, message: str, status_code: Optional[int] = None) -> None:
        super().__init__(message)
        self.status_code = status_code


class GeminiClient:
    def __init__(
        self,
        api_key: str,
        api_url: str,
        model: str,
        timeout: float = 20.0,
    ) -> None:
        self.api_key = api_key
        self.api_url = api_url.rstrip("/")
        self.model = model
        self.timeout = timeout
        self._client = _HttpxClient(timeout=self.timeout)

    def generate(self, prompt: str) -> str:
        payload = {
            "contents": [
                {
                    "parts": [
                        {
                            "text": prompt,
                        }
                    ]
                }
            ]
        }
        url = f"{self.api_url}/{self.model}:generateContent"

        try:
            response = self._client.post(
                url,
                params={"key": self.api_key},
                json=payload,
            )
        except httpx.HTTPError as exc:
            raise GeminiClientError(
                f"Gemini request failed: {exc.__class__.__name__}",
            ) from exc

        if response.status_code >= 400:
            raise GeminiClientError(
                f"Gemini returned status {response.status_code}",
                status_code=response.status_code,
            )

        try:
            data = response.json()
            candidates = data["candidates"]
            if not candidates:
                raise GeminiClientError("Gemini returned empty candidates")
            parts = candidates[0]["content"]["parts"]
            if not parts:
                raise GeminiClientError("Gemini returned empty parts")
            return str(parts[0]["text"]).strip()
        except GeminiClientError:
            raise
        except (KeyError, IndexError, TypeError, ValueError) as exc:
            logger.warning("Failed to parse Gemini response: %s", exc.__class__.__name__)
            raise GeminiClientError("Gemini response parsing failed") from exc

    def summarize(self, text: str) -> str:
        return self.generate(build_summary_prompt(text))

    def close(self) -> None:
        self._client.close()
