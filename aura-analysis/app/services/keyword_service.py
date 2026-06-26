from __future__ import annotations

from collections import Counter
from dataclasses import dataclass
import re
from typing import Optional

import numpy as np

from app.domain.enums import Topic
from app.services.morphology_service import MorphToken, MorphologyService
from app.services.preprocess_service import PreprocessService
from app.services.stopwords import RUSSIAN_STOPWORDS


@dataclass(frozen=True)
class RankedKeywordCandidate:
    candidate: str
    score: float
    embedding: np.ndarray | None = None


class KeywordService:
    _keyword_cleanup_pattern = re.compile(r"[^\w\s-]+", re.IGNORECASE)
    _max_phrase_words = 3
    _preferred_noun_forms = {
        "преподаватель": "преподаватели",
        "студент": "студенты",
        "дисциплина": "дисциплины",
        "предмет": "предметы",
        "занятие": "занятия",
        "мероприятие": "мероприятия",
        "документ": "документы",
        "аудитория": "аудитории",
        "критерий": "критерии",
    }
    _surface_token_map = {
        "обратную": "обратная",
        "понятную": "понятная",
        "прозрачными": "прозрачные",
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
        "связь",
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
        "комиссия",
    }
    _topic_markers: dict[Topic, tuple[str, ...]] = {
        Topic.EDUCATION: ("учеб", "дисципл", "программ", "курс", "лекц", "семинар", "экзам", "практик", "теори"),
        Topic.TEACHERS: ("преподав", "оцен", "обратн", "кафедр", "объясн", "провер"),
        Topic.INFRASTRUCTURE: ("кампус", "аудитор", "корпус", "столов", "библиот", "территор", "простран", "транспорт"),
        Topic.DORMITORY: ("общежит", "комнат", "кухн", "сосед", "прачеч", "заселен", "проживан", "бытов"),
        Topic.ADMINISTRATION: ("деканат", "администр", "документ", "комисси", "расписан", "срок", "справк", "заявлен"),
        Topic.STUDENT_LIFE: ("студенческ", "клуб", "мероприят", "сообществ", "друз", "атмосфер", "инициатив", "внеучеб", "проект"),
        Topic.OTHER: (),
    }

    def __init__(self, preprocess_service: PreprocessService) -> None:
        self.preprocess_service = preprocess_service
        self.morphology_service = MorphologyService()

    def extract_keywords(
        self,
        text: str,
        min_keywords: int = 3,
        max_keywords: int = 5,
        topic: Topic | None = None,
        embedder=None,
    ) -> list[str]:
        normalized_text = self.preprocess_service.normalize_text(text)
        token_infos = self._tokenize_infos(normalized_text)
        candidates = self._collect_candidates(token_infos)
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
                for fallback in self._extract_frequency_keywords(token_infos, max_keywords=max_keywords):
                    if fallback not in selected:
                        selected.append(fallback)
                    if len(selected) == max_keywords:
                        break
                return selected[:max_keywords]

        return self._extract_frequency_keywords(token_infos, max_keywords=max_keywords, min_keywords=min_keywords)

    def _extract_frequency_keywords(
        self,
        token_infos: list[MorphToken],
        max_keywords: int = 5,
        min_keywords: int = 3,
    ) -> list[str]:
        filtered_tokens = [
            self._display_token(info)
            for info in token_infos
            if self._is_single_keyword_candidate(info)
        ]

        if not filtered_tokens:
            return ["отзыв"]

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
            if not normalized_candidate or normalized_candidate in seen:
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

        token_infos = self._tokenize_infos(normalized)
        if not token_infos or len(token_infos) > self._max_phrase_words:
            return None

        if len(token_infos) == 1:
            if not self._is_single_keyword_candidate(token_infos[0]):
                return None
        elif not self._is_phrase_candidate(token_infos):
            return None

        if len(token_infos) == 1:
            phrase = self._display_token(token_infos[0]).strip()
        else:
            phrase = " ".join(self._surface_phrase_token(info) for info in token_infos).strip()
        return phrase or None

    def is_content_keyword(self, token: str) -> bool:
        info = self.morphology_service.analyze_token(self.preprocess_service.normalize_text(token).lower().strip())
        return self._is_content_keyword_info(info)

    def is_valid_keyword_part(self, token: str) -> bool:
        return bool(token) and len(token) >= 3 and not token.isdigit()

    def _tokenize_infos(self, text: str) -> list[MorphToken]:
        return [self.morphology_service.analyze_token(token) for token in self.preprocess_service.tokenize(text)]

    def _collect_candidates(self, token_infos: list[MorphToken]) -> list[dict[str, float | int | str]]:
        if not token_infos:
            return []

        candidate_stats: dict[str, dict[str, float | int | str]] = {}
        token_count = len(token_infos)

        for size in range(1, self._max_phrase_words + 1):
            for start_index in range(0, token_count - size + 1):
                window = token_infos[start_index : start_index + size]
                candidate = self.normalize_keyword(" ".join(info.surface for info in window))
                if not candidate:
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
    ) -> list[RankedKeywordCandidate]:
        if not candidates:
            return []

        max_frequency = max(int(item["frequency"]) for item in candidates)
        semantic_scores, semantic_embeddings = self._build_semantic_data(
            text=text,
            candidates=[str(item["candidate"]) for item in candidates],
            embedder=embedder,
        )

        ranked: list[RankedKeywordCandidate] = []
        for item in candidates:
            candidate = str(item["candidate"])
            frequency = int(item["frequency"])
            first_position = int(item["first_position"])
            size = int(item["size"])

            frequency_score = frequency / max_frequency if max_frequency else 0.0
            position_score = max(0.0, 1.0 - (first_position / max(1, len(text.split()))))
            phrase_score = min(1.0, 0.45 + (size - 1) * 0.2)
            topic_score = self._topic_bonus(candidate, topic)
            semantic_score = semantic_scores.get(candidate, 0.0)

            if embedder is not None:
                score = (
                    0.46 * semantic_score
                    + 0.18 * frequency_score
                    + 0.14 * position_score
                    + 0.12 * phrase_score
                    + 0.10 * topic_score
                )
            else:
                score = (
                    0.34 * frequency_score
                    + 0.28 * phrase_score
                    + 0.18 * position_score
                    + 0.20 * topic_score
                )

            ranked.append(
                RankedKeywordCandidate(
                    candidate=candidate,
                    score=score,
                    embedding=semantic_embeddings.get(candidate),
                )
            )

        ranked.sort(key=lambda item: (-item.score, -len(item.candidate), item.candidate))
        return ranked

    def _build_semantic_data(
        self,
        text: str,
        candidates: list[str],
        embedder,
    ) -> tuple[dict[str, float], dict[str, np.ndarray]]:
        if embedder is None or not candidates:
            return {}, {}

        embeddings = np.asarray(embedder.embed_texts([text, *candidates]), dtype=np.float32)
        if embeddings.ndim != 2 or len(embeddings) != len(candidates) + 1:
            return {}, {}

        text_embedding = embeddings[0]
        candidate_embeddings = embeddings[1:]
        text_norm = np.linalg.norm(text_embedding)
        if text_norm == 0:
            return {}, {}

        scores: dict[str, float] = {}
        vectors: dict[str, np.ndarray] = {}
        for candidate, candidate_embedding in zip(candidates, candidate_embeddings):
            candidate_norm = np.linalg.norm(candidate_embedding)
            if candidate_norm == 0:
                scores[candidate] = 0.0
                vectors[candidate] = candidate_embedding
                continue
            similarity = float(np.dot(text_embedding, candidate_embedding) / (text_norm * candidate_norm))
            scores[candidate] = max(0.0, min(1.0, (similarity + 1.0) / 2.0))
            vectors[candidate] = candidate_embedding

        return scores, vectors

    def _topic_bonus(self, candidate: str, topic: Topic | None) -> float:
        if topic is None or topic == Topic.OTHER:
            return 0.0

        candidate_stem = self._stem_phrase(candidate)
        markers = self._topic_markers.get(topic, ())
        return 1.0 if any(marker in candidate_stem for marker in markers) else 0.0

    def _select_diverse_keywords(
        self,
        ranked_candidates: list[RankedKeywordCandidate],
        max_keywords: int,
    ) -> list[str]:
        if not ranked_candidates:
            return ["отзыв"]

        if any(candidate.embedding is not None for candidate in ranked_candidates):
            return self._select_keywords_with_mmr(ranked_candidates, max_keywords)

        selected: list[str] = []
        selected_token_sets: list[set[str]] = []
        for ranked in ranked_candidates:
            candidate_token_set = set(ranked.candidate.split())
            if any(
                ranked.candidate == selected_candidate
                or ranked.candidate in selected_candidate
                or selected_candidate in ranked.candidate
                or candidate_token_set.issubset(existing_token_set)
                or existing_token_set.issubset(candidate_token_set)
                for selected_candidate, existing_token_set in zip(selected, selected_token_sets)
            ):
                continue
            selected.append(ranked.candidate)
            selected_token_sets.append(candidate_token_set)
            if len(selected) == max_keywords:
                break

        return selected or ["отзыв"]

    def _select_keywords_with_mmr(
        self,
        ranked_candidates: list[RankedKeywordCandidate],
        max_keywords: int,
    ) -> list[str]:
        selected: list[RankedKeywordCandidate] = []
        remaining = ranked_candidates.copy()
        diversity_lambda = 0.72

        while remaining and len(selected) < max_keywords:
            if not selected:
                selected.append(remaining.pop(0))
                continue

            best_index = 0
            best_score = float("-inf")
            for index, candidate in enumerate(remaining):
                redundancy_penalty = max(
                    self._cosine_similarity(candidate.embedding, picked.embedding)
                    for picked in selected
                )
                mmr_score = diversity_lambda * candidate.score - (1.0 - diversity_lambda) * redundancy_penalty
                if mmr_score > best_score:
                    best_score = mmr_score
                    best_index = index

            selected.append(remaining.pop(best_index))

        return [candidate.candidate for candidate in selected] or ["отзыв"]

    def _cosine_similarity(self, left: np.ndarray | None, right: np.ndarray | None) -> float:
        if left is None or right is None:
            return 0.0
        left_norm = np.linalg.norm(left)
        right_norm = np.linalg.norm(right)
        if left_norm == 0 or right_norm == 0:
            return 0.0
        similarity = float(np.dot(left, right) / (left_norm * right_norm))
        return max(0.0, min(1.0, (similarity + 1.0) / 2.0))

    def _stem_phrase(self, value: str) -> str:
        parts = [token[: max(4, len(token) - 2)] for token in value.split()]
        return " ".join(parts)

    def _display_token(self, info: MorphToken) -> str:
        if self._is_adjective_pos(info.pos):
            return self._surface_token_map.get(info.surface, info.surface)
        return self._preferred_noun_forms.get(info.lemma, info.lemma)

    def _surface_phrase_token(self, info: MorphToken) -> str:
        if self._is_adjective_pos(info.pos):
            return self._surface_token_map.get(info.surface, info.surface)
        return info.surface

    def _is_content_keyword_info(self, info: MorphToken) -> bool:
        normalized = info.lemma
        return (
            self.is_valid_keyword_part(normalized)
            and normalized not in RUSSIAN_STOPWORDS
            and normalized not in self._weak_keyword_tokens
            and info.surface not in RUSSIAN_STOPWORDS
            and info.surface not in self._weak_keyword_tokens
            and not self._is_verb_pos(info.pos)
        )

    def _is_single_keyword_candidate(self, info: MorphToken) -> bool:
        if not self._is_content_keyword_info(info):
            return False
        if self._is_noun_pos(info.pos):
            return True
        return self._display_token(info) in self._adjective_keyword_allowlist

    def _is_phrase_candidate(self, token_infos: list[MorphToken]) -> bool:
        if not token_infos or len(token_infos) < 2 or len(token_infos) > self._max_phrase_words:
            return False
        if any(not self._is_content_keyword_info(info) for info in token_infos):
            return False
        if any(self._display_token(info) in self._weak_phrase_parts for info in token_infos):
            return False

        pos_pattern = tuple(self._pos_group(info.pos) for info in token_infos)

        if len(token_infos) == 2:
            if pos_pattern == ("ADJ", "NOUN"):
                return True
            if pos_pattern == ("NOUN", "NOUN"):
                return self._is_noun_chain(token_infos)
            return False

        if pos_pattern == ("ADJ", "ADJ", "NOUN"):
            return True
        if pos_pattern == ("ADJ", "NOUN", "NOUN"):
            return self._is_noun_chain(token_infos[1:])
        if pos_pattern == ("NOUN", "ADJ", "NOUN"):
            return self._display_token(token_infos[0]) in self._noun_phrase_heads and self._is_noun_chain(token_infos[1:])
        return False

    def _is_noun_chain(self, token_infos: list[MorphToken]) -> bool:
        head = self._display_token(token_infos[0])
        tail = self._display_token(token_infos[-1])
        tail_case = token_infos[-1].case
        return head in self._noun_phrase_heads and (
            tail in self._phrase_tail_tokens or tail_case in {"gent", "gen2"}
        )

    def _pos_group(self, pos: str) -> str:
        if self._is_noun_pos(pos):
            return "NOUN"
        if self._is_adjective_pos(pos):
            return "ADJ"
        return "OTHER"

    def _is_noun_pos(self, pos: str) -> bool:
        return pos in {"NOUN", "NPRO"}

    def _is_adjective_pos(self, pos: str) -> bool:
        return pos in {"ADJF", "ADJS", "PRTF", "PRTS"}

    def _is_verb_pos(self, pos: str) -> bool:
        return pos in {"VERB", "INFN", "GRND"}
