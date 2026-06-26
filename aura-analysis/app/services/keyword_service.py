from __future__ import annotations

from collections import Counter
import re
from typing import Optional

import numpy as np

from app.domain.enums import Topic
from app.services.preprocess_service import PreprocessService
from app.services.stopwords import RUSSIAN_STOPWORDS


class KeywordService:
    _keyword_cleanup_pattern = re.compile(r"[^\w\s-]+", re.IGNORECASE)
    _max_phrase_words = 2
    _canonical_token_map = {
        "преподаватель": "преподаватели",
        "преподавателя": "преподаватели",
        "преподавателю": "преподаватели",
        "преподавателем": "преподаватели",
        "преподавателей": "преподаватели",
        "преподавателях": "преподаватели",
        "студента": "студенты",
        "студенту": "студенты",
        "студентом": "студенты",
        "студентов": "студенты",
        "студентам": "студенты",
        "кампуса": "кампус",
        "кампусе": "кампус",
        "кампусу": "кампус",
        "корпуса": "корпус",
        "корпусе": "корпус",
        "корпусов": "корпус",
        "общежития": "общежитие",
        "общежитии": "общежитие",
        "общежитию": "общежитие",
        "документов": "документы",
        "документам": "документы",
        "документах": "документы",
        "дисциплин": "дисциплины",
        "дисциплинам": "дисциплины",
        "предметов": "предметы",
        "предметам": "предметы",
        "занятий": "занятия",
        "занятиях": "занятия",
        "занятиям": "занятия",
        "оценивания": "оценивание",
        "оцениванию": "оценивание",
        "оцениванием": "оценивание",
        "обратную": "обратная",
        "понятную": "понятная",
        "прозрачными": "прозрачность",
        "критерии": "критерии",
        "мероприятий": "мероприятия",
        "мероприятиях": "мероприятия",
        "мероприятиям": "мероприятия",
        "стипендии": "стипендия",
        "стипендию": "стипендия",
        "стипендией": "стипендия",
        "расписания": "расписание",
        "расписанию": "расписание",
        "расписанием": "расписание",
        "расписании": "расписание",
        "корпусах": "корпус",
        "аудиториях": "аудитории",
        "курса": "курс",
        "курсе": "курс",
        "языка": "язык",
        "языке": "язык",
    }
    _adjective_keyword_allowlist = {
        "учебный",
        "студенческий",
        "административный",
        "технический",
        "практический",
    }
    _weak_keyword_tokens = {
        "жалею",
        "пошла",
        "пошел",
        "пошла",
        "будучи",
        "знаете",
        "причем",
        "кстати",
        "вообще",
        "просто",
        "хотя",
        "итоге",
        "сразу",
        "потом",
        "этом",
        "всем",
        "деле",
        "однако",
        "действительно",
        "множество",
        "сюда",
        "часто",
        "учусь",
        "учиться",
        "этому",
        "этим",
        "также",
        "равно",
    }
    _weak_phrase_prefixes = {
        "хотя",
        "если",
        "когда",
        "потому",
        "зато",
        "кстати",
        "просто",
        "вообще",
        "будто",
        "даже",
    }
    _weak_phrase_parts = {
        "этом",
        "этому",
        "этим",
        "всем",
        "деле",
        "однако",
        "действительно",
        "множество",
        "сюда",
        "часто",
        "учусь",
        "учиться",
    }
    _noun_phrase_heads = {
        "качество",
        "уровень",
        "процесс",
        "система",
        "формат",
        "организация",
        "вопросы",
        "подача",
        "атмосфера",
        "поддержка",
        "условия",
        "заселение",
        "стипендия",
        "расписание",
    }
    _phrase_tail_tokens = {
        "связь",
        "процесс",
        "жизнь",
        "поддержка",
        "система",
        "расписание",
        "стипендия",
        "заселение",
        "атмосфера",
        "курс",
        "язык",
    }
    _topic_markers: dict[Topic, tuple[str, ...]] = {
        Topic.EDUCATION: (
            "учеб",
            "дисципл",
            "программ",
            "курс",
            "лекц",
            "семинар",
            "экзам",
            "практик",
            "теори",
        ),
        Topic.TEACHERS: (
            "преподав",
            "оцен",
            "обратн",
            "кафедр",
            "объясн",
            "провер",
        ),
        Topic.INFRASTRUCTURE: (
            "кампус",
            "аудитор",
            "корпус",
            "столов",
            "библиот",
            "территор",
            "простран",
            "транспорт",
        ),
        Topic.DORMITORY: (
            "общежит",
            "комнат",
            "кухн",
            "сосед",
            "прачеч",
            "заселен",
            "проживан",
            "бытов",
        ),
        Topic.ADMINISTRATION: (
            "деканат",
            "администр",
            "документ",
            "комисси",
            "расписан",
            "срок",
            "справк",
            "заявлен",
        ),
        Topic.STUDENT_LIFE: (
            "студенческ",
            "клуб",
            "мероприят",
            "сообществ",
            "друз",
            "атмосфер",
            "инициатив",
            "внеучеб",
            "проект",
        ),
        Topic.OTHER: (),
    }

    def __init__(self, preprocess_service: PreprocessService) -> None:
        self.preprocess_service = preprocess_service

    def extract_keywords(
        self,
        text: str,
        min_keywords: int = 3,
        max_keywords: int = 5,
        topic: Topic | None = None,
        embedder=None,
    ) -> list[str]:
        normalized_text = self.preprocess_service.normalize_text(text)
        candidates = self._collect_candidates(normalized_text)
        if candidates:
            ranked_candidates = self._rank_candidates(
                normalized_text,
                candidates,
                topic=topic,
                embedder=embedder,
            )
            selected = self._select_diverse_keywords(ranked_candidates, max_keywords=max_keywords)
            if len(selected) >= min_keywords:
                return selected
            if selected:
                for fallback in self._extract_frequency_keywords(normalized_text, max_keywords=max_keywords):
                    if fallback not in selected:
                        selected.append(fallback)
                    if len(selected) == max_keywords:
                        break
                return selected[:max_keywords]

        return self._extract_frequency_keywords(normalized_text, max_keywords=max_keywords, min_keywords=min_keywords)

    def _extract_frequency_keywords(
        self,
        text: str,
        max_keywords: int = 5,
        min_keywords: int = 3,
    ) -> list[str]:
        tokens = self.preprocess_service.tokenize(text)
        filtered_tokens = [
            self._canonicalize_token(token)
            for token in tokens
            if self.is_content_keyword(token)
            and not (
                self._looks_like_adjective(self._canonicalize_token(token))
                and self._canonicalize_token(token) not in self._adjective_keyword_allowlist
            )
        ]

        if not filtered_tokens:
            fallback = [
                self._canonicalize_token(token)
                for token in tokens
                if self.is_content_keyword(token)
                and not (
                    self._looks_like_adjective(self._canonicalize_token(token))
                    and self._canonicalize_token(token) not in self._adjective_keyword_allowlist
                )
            ][:max_keywords]
            return fallback[:max_keywords] or ["отзыв"]

        frequencies = Counter(filtered_tokens)
        scored_tokens = sorted(
            frequencies.items(),
            key=lambda item: (-item[1], -len(item[0]), filtered_tokens.index(item[0])),
        )

        keywords = [token for token, _ in scored_tokens[:max_keywords]]
        if len(keywords) >= min_keywords:
            return keywords

        for token in filtered_tokens:
            if token not in keywords:
                keywords.append(token)
            if len(keywords) == max_keywords:
                break

        return keywords[:max_keywords]

    def filter_keywords(self, candidates: list[str], max_keywords: int = 5) -> list[str]:
        keywords: list[str] = []
        seen: set[str] = set()

        for candidate in candidates:
            normalized_candidate = self.normalize_keyword(candidate)
            if not normalized_candidate:
                continue
            if normalized_candidate in seen:
                continue
            seen.add(normalized_candidate)
            keywords.append(normalized_candidate)
            if len(keywords) == max_keywords:
                break

        return keywords or ["отзыв"]

    def normalize_keyword(self, candidate: str) -> Optional[str]:
        normalized = self.preprocess_service.normalize_text(candidate).lower()
        normalized = self._keyword_cleanup_pattern.sub(" ", normalized)
        normalized = re.sub(r"\s+", " ", normalized).strip()
        if not normalized:
            return None

        parts = normalized.split()
        if not parts:
            return None
        if len(parts) > self._max_phrase_words:
            return None
        parts = [self._canonicalize_token(part) for part in parts]
        if all(not self.is_content_keyword(part) for part in parts):
            return None
        if any(not self.is_valid_keyword_part(part) for part in parts):
            return None
        if any(part in self._weak_phrase_parts for part in parts):
            return None
        return " ".join(parts)

    def is_content_keyword(self, token: str) -> bool:
        normalized = self.preprocess_service.normalize_text(token).lower().strip()
        return (
            self.is_valid_keyword_part(normalized)
            and normalized not in RUSSIAN_STOPWORDS
            and normalized not in self._weak_keyword_tokens
            and not self._looks_like_verbish(normalized)
        )

    def is_valid_keyword_part(self, token: str) -> bool:
        return bool(token) and len(token) >= 3 and not token.isdigit()

    def _collect_candidates(self, text: str) -> list[dict[str, float | int | str]]:
        tokens = self.preprocess_service.tokenize(text)
        if not tokens:
            return []

        candidate_stats: dict[str, dict[str, float | int | str]] = {}
        token_count = len(tokens)
        for size in range(1, self._max_phrase_words + 1):
            for start_index in range(0, token_count - size + 1):
                candidate = self.normalize_keyword(" ".join(tokens[start_index : start_index + size]))
                if not candidate:
                    continue

                if size == 1 and candidate in RUSSIAN_STOPWORDS:
                    continue
                if size == 1 and self._looks_like_adjective(candidate) and candidate not in self._adjective_keyword_allowlist:
                    continue

                parts = candidate.split()
                if size > 1 and not all(self.is_content_keyword(part) for part in parts):
                    continue
                if size > 1 and not self._is_phrase_candidate(parts):
                    continue

                stats = candidate_stats.setdefault(
                    candidate,
                    {
                        "candidate": candidate,
                        "frequency": 0,
                        "first_position": start_index,
                        "size": size,
                    },
                )
                stats["frequency"] = int(stats["frequency"]) + 1
                stats["first_position"] = min(int(stats["first_position"]), start_index)

        return list(candidate_stats.values())

    def _rank_candidates(
        self,
        text: str,
        candidates: list[dict[str, float | int | str]],
        topic: Topic | None,
        embedder,
    ) -> list[str]:
        if not candidates:
            return []

        max_frequency = max(int(item["frequency"]) for item in candidates)
        semantic_scores = self._build_semantic_scores(
            text=text,
            candidates=[str(item["candidate"]) for item in candidates],
            embedder=embedder,
        )

        ranked: list[tuple[float, str]] = []
        for item in candidates:
            candidate = str(item["candidate"])
            frequency = int(item["frequency"])
            first_position = int(item["first_position"])
            size = int(item["size"])

            frequency_score = frequency / max_frequency if max_frequency else 0.0
            position_score = max(0.0, 1.0 - (first_position / max(1, len(text.split()))))
            phrase_score = min(1.0, 0.45 + (size - 1) * 0.25)
            topic_score = self._topic_bonus(candidate, topic)
            semantic_score = semantic_scores.get(candidate, 0.0)

            if embedder is not None:
                score = (
                    0.5 * semantic_score
                    + 0.18 * frequency_score
                    + 0.14 * position_score
                    + 0.1 * phrase_score
                    + 0.08 * topic_score
                )
            else:
                score = (
                    0.42 * frequency_score
                    + 0.24 * phrase_score
                    + 0.2 * position_score
                    + 0.14 * topic_score
                )

            ranked.append((score, candidate))

        ranked.sort(key=lambda item: (-item[0], -len(item[1]), item[1]))
        return [candidate for _, candidate in ranked]

    def _build_semantic_scores(
        self,
        text: str,
        candidates: list[str],
        embedder,
    ) -> dict[str, float]:
        if embedder is None or not candidates:
            return {}

        embeddings = np.asarray(embedder.embed_texts([text, *candidates]), dtype=np.float32)
        if embeddings.ndim != 2 or len(embeddings) != len(candidates) + 1:
            return {}

        text_embedding = embeddings[0]
        candidate_embeddings = embeddings[1:]
        text_norm = np.linalg.norm(text_embedding)
        if text_norm == 0:
            return {}

        scores: dict[str, float] = {}
        for candidate, candidate_embedding in zip(candidates, candidate_embeddings):
            candidate_norm = np.linalg.norm(candidate_embedding)
            if candidate_norm == 0:
                scores[candidate] = 0.0
                continue
            similarity = float(np.dot(text_embedding, candidate_embedding) / (text_norm * candidate_norm))
            scores[candidate] = max(0.0, min(1.0, (similarity + 1.0) / 2.0))
        return scores

    def _topic_bonus(self, candidate: str, topic: Topic | None) -> float:
        if topic is None or topic == Topic.OTHER:
            return 0.0

        candidate_stem = self._stem_phrase(candidate)
        markers = self._topic_markers.get(topic, ())
        if any(marker in candidate_stem for marker in markers):
            return 1.0
        return 0.0

    def _select_diverse_keywords(self, ranked_candidates: list[str], max_keywords: int) -> list[str]:
        selected: list[str] = []
        selected_token_sets: list[set[str]] = []

        for candidate in ranked_candidates:
            candidate_token_set = set(candidate.split())
            if any(
                candidate == selected_candidate
                or candidate in selected_candidate
                or selected_candidate in candidate
                or candidate_token_set.issubset(existing_token_set)
                or existing_token_set.issubset(candidate_token_set)
                for selected_candidate, existing_token_set in zip(selected, selected_token_sets)
            ):
                continue

            selected.append(candidate)
            selected_token_sets.append(candidate_token_set)
            if len(selected) == max_keywords:
                break

        return selected or ["отзыв"]

    def _stem_phrase(self, value: str) -> str:
        parts = [token[: max(4, len(token) - 2)] for token in value.split()]
        return " ".join(parts)

    def _canonicalize_token(self, token: str) -> str:
        return self._canonical_token_map.get(token, token)

    def _is_phrase_candidate(self, parts: list[str]) -> bool:
        if len(parts) != 2:
            return False

        first, second = parts
        if first in self._weak_phrase_prefixes or second in self._weak_phrase_prefixes:
            return False
        if first in self._weak_phrase_parts or second in self._weak_phrase_parts:
            return False
        if self._looks_like_verbish(first) or self._looks_like_verbish(second):
            return False
        if self._looks_like_adjective(second):
            return False
        if self._looks_like_nounish(first) and self._looks_like_nounish(second):
            return first in self._noun_phrase_heads and (
                self._looks_like_genitiveish(second) or second in self._phrase_tail_tokens
            )
        if self._looks_like_adjective(first):
            return self._looks_like_nounish(second) or second in self._phrase_tail_tokens
        if first in self._noun_phrase_heads and (
            self._looks_like_genitiveish(second) or second in self._phrase_tail_tokens
        ):
            return True
        return False

    def _looks_like_adjective(self, token: str) -> bool:
        adjective_endings = (
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
            "ым",
            "им",
        )
        return token.endswith(adjective_endings)

    def _looks_like_verbish(self, token: str) -> bool:
        verb_like_endings = (
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
        if token in {"дает", "дают", "можно", "нужно", "стоит"}:
            return True
        return token.endswith(verb_like_endings)

    def _looks_like_nounish(self, token: str) -> bool:
        noun_like_endings = (
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
        return token.endswith(noun_like_endings)

    def _looks_like_genitiveish(self, token: str) -> bool:
        genitive_like_endings = (
            "а",
            "я",
            "ы",
            "и",
            "ии",
            "ения",
            "ания",
            "ости",
            "ства",
        )
        return token.endswith(genitive_like_endings)
