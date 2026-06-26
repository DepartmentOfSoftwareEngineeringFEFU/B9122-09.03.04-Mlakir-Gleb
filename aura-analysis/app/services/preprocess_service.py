from html import unescape
import re


class PreprocessService:
    _control_chars_pattern = re.compile(r"[\x00-\x1f\x7f]+")
    _cleanup_pattern = re.compile(r"[^0-9a-zа-яё\s.,!?;:()\"'%\-]+", re.IGNORECASE)
    _spaces_pattern = re.compile(r"\s+")
    _token_pattern = re.compile(r"[a-zа-яё0-9]+", re.IGNORECASE)

    def normalize_text(self, text: str) -> str:
        normalized = unescape(text).lower().replace("ё", "е")
        normalized = self._control_chars_pattern.sub(" ", normalized)
        normalized = self._cleanup_pattern.sub(" ", normalized)
        normalized = self._spaces_pattern.sub(" ", normalized).strip()
        return normalized

    def tokenize(self, text: str) -> list[str]:
        normalized = self.normalize_text(text)
        return [token for token in self._token_pattern.findall(normalized) if len(token) > 1]
