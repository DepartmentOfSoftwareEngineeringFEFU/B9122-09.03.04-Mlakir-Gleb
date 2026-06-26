from __future__ import annotations

from dataclasses import dataclass
from functools import lru_cache

try:
    from pymorphy3 import MorphAnalyzer
except ImportError:  # pragma: no cover - runtime fallback when dependency is unavailable
    MorphAnalyzer = None


@dataclass(frozen=True)
class MorphToken:
    surface: str
    lemma: str
    pos: str
    case: str | None = None


class MorphologyService:
    def __init__(self) -> None:
        self._analyzer = MorphAnalyzer() if MorphAnalyzer is not None else None

    @lru_cache(maxsize=4096)
    def analyze_token(self, token: str) -> MorphToken:
        normalized = token.lower().strip()
        if not normalized:
            return MorphToken(surface="", lemma="", pos="X")

        if self._analyzer is None:
            return self._fallback_token(normalized)

        parsed = self._analyzer.parse(normalized)
        if not parsed:
            return self._fallback_token(normalized)

        best = parsed[0]
        pos = best.tag.POS or "X"
        case = best.tag.case
        lemma = best.normal_form or normalized
        return MorphToken(
            surface=normalized,
            lemma=lemma,
            pos=pos,
            case=case,
        )

    def _fallback_token(self, token: str) -> MorphToken:
        if self._looks_like_verbish(token):
            pos = "VERB"
        elif self._looks_like_adjective(token):
            pos = "ADJF"
        elif self._looks_like_nounish(token):
            pos = "NOUN"
        else:
            pos = "X"

        return MorphToken(
            surface=token,
            lemma=token,
            pos=pos,
            case=None,
        )

    def _looks_like_adjective(self, token: str) -> bool:
        return token.endswith(
            (
                "ый",
                "ий",
                "ой",
                "ая",
                "яя",
                "ое",
                "ее",
                "ые",
                "ие",
                "ого",
                "его",
                "ому",
                "ему",
                "ым",
                "им",
                "ую",
                "юю",
                "ых",
                "их",
            )
        )

    def _looks_like_verbish(self, token: str) -> bool:
        if token in {"дает", "дают", "можно", "нужно", "стоит"}:
            return True

        return token.endswith(
            (
                "ать",
                "ять",
                "еть",
                "ить",
                "уть",
                "аю",
                "яю",
                "ею",
                "ую",
                "ют",
                "ет",
                "ит",
                "ат",
                "ят",
                "ал",
                "ял",
                "ел",
                "ил",
                "ла",
                "ли",
                "утся",
                "ются",
                "ется",
                "ится",
                "ался",
                "илась",
                "ились",
                "али",
                "или",
                "ает",
                "яют",
                "ено",
                "ена",
                "ены",
                "ен",
            )
        )

    def _looks_like_nounish(self, token: str) -> bool:
        return token.endswith(
            (
                "а",
                "я",
                "о",
                "е",
                "ы",
                "и",
                "ия",
                "ие",
                "ость",
                "ение",
                "ание",
                "изм",
                "ор",
                "ер",
                "ель",
                "арь",
                "ус",
                "ум",
                "ция",
            )
        )
