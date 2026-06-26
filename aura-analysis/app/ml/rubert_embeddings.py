from __future__ import annotations

import importlib
import logging
from typing import Any

import numpy as np


logger = logging.getLogger(__name__)

MAX_MODEL_TOKENS = 512
MAX_CHUNK_TOKENS = 448


def _load_torch_module():
    try:
        return importlib.import_module("torch")
    except ImportError as exc:
        raise RuntimeError("torch is required for RUBERT_EMBEDDINGS mode") from exc


def _load_transformers():
    try:
        transformers = importlib.import_module("transformers")
    except ImportError as exc:
        raise RuntimeError("transformers is required for RUBERT_EMBEDDINGS mode") from exc
    return transformers.AutoModel, transformers.AutoTokenizer


class RuBERTService:
    """Builds fixed-size embeddings for short and long texts with RuBERT."""

    def __init__(
        self,
        model_name: str,
        tokenizer: Any | None = None,
        model: Any | None = None,
        device: str | None = None,
        max_chunk_tokens: int = MAX_CHUNK_TOKENS,
    ) -> None:
        torch = _load_torch_module()
        auto_model, auto_tokenizer = _load_transformers()

        self.torch = torch
        self.model_name = model_name
        self.device = device or ("cuda" if torch.cuda.is_available() else "cpu")
        self.max_chunk_tokens = min(max_chunk_tokens, MAX_MODEL_TOKENS - 2)
        self.tokenizer = tokenizer or auto_tokenizer.from_pretrained(model_name)
        self.model = model or auto_model.from_pretrained(model_name)
        if hasattr(self.model, "to"):
            self.model.to(self.device)
        if hasattr(self.model, "eval"):
            self.model.eval()

    def embed(self, text: str) -> np.ndarray:
        """Embeds one text with CLS pooling for regular-length inputs."""
        batch_embeddings = self._embed_batch([text])
        return batch_embeddings[0]

    def embed_long_text(self, text: str) -> np.ndarray:
        """Embeds long text by chunking tokens and averaging chunk embeddings."""
        token_ids = self.tokenizer.encode(
            text,
            add_special_tokens=False,
            truncation=False,
        )
        if len(token_ids) <= MAX_MODEL_TOKENS:
            logger.info("RuBERT text length chars=%s chunks=1", len(text))
            return self.embed(text)

        chunk_token_ids = [
            token_ids[start_index : start_index + self.max_chunk_tokens]
            for start_index in range(0, len(token_ids), self.max_chunk_tokens)
        ]
        logger.info("RuBERT text length chars=%s chunks=%s", len(text), len(chunk_token_ids))

        chunk_texts = [
            self.tokenizer.decode(chunk_ids, skip_special_tokens=True, clean_up_tokenization_spaces=True)
            for chunk_ids in chunk_token_ids
        ]
        chunk_embeddings = self._embed_batch(chunk_texts)
        return np.mean(chunk_embeddings, axis=0, dtype=np.float32)

    def embed_texts(self, texts: list[str], batch_size: int = 16) -> np.ndarray:
        if not texts:
            return np.empty((0, 0), dtype=np.float32)

        short_texts: list[str] = []
        short_indices: list[int] = []
        embedded_by_index: dict[int, np.ndarray] = {}

        for index, text in enumerate(texts):
            token_ids = self.tokenizer.encode(
                text,
                add_special_tokens=False,
                truncation=False,
            )
            if len(token_ids) < MAX_MODEL_TOKENS:
                short_texts.append(text)
                short_indices.append(index)
                continue
            embedded_by_index[index] = self.embed_long_text(text)

        if short_texts:
            short_embeddings = self._embed_short_texts(short_texts, batch_size=batch_size)
            for index, embedding in zip(short_indices, short_embeddings):
                embedded_by_index[index] = embedding

        ordered_embeddings = [embedded_by_index[index] for index in range(len(texts))]
        return np.asarray(ordered_embeddings, dtype=np.float32)

    def _embed_short_texts(self, texts: list[str], batch_size: int) -> np.ndarray:
        embeddings: list[np.ndarray] = []
        for start_index in range(0, len(texts), batch_size):
            batch = texts[start_index : start_index + batch_size]
            embeddings.append(self._embed_batch(batch))
        return np.vstack(embeddings)

    def _embed_batch(self, texts: list[str]) -> np.ndarray:
        encoded = self.tokenizer(
            texts,
            padding=True,
            truncation=True,
            max_length=MAX_MODEL_TOKENS,
            return_tensors="pt",
        )
        encoded = {
            key: value.to(self.device) if hasattr(value, "to") else value
            for key, value in encoded.items()
        }
        with self.torch.no_grad():
            outputs = self.model(**encoded)

        # CLS token keeps a stable fixed-size sentence representation.
        cls_embeddings = outputs.last_hidden_state[:, 0, :]
        if hasattr(cls_embeddings, "cpu"):
            cls_embeddings = cls_embeddings.cpu()
        if hasattr(cls_embeddings, "numpy"):
            cls_embeddings = cls_embeddings.numpy()
        return np.asarray(cls_embeddings, dtype=np.float32)


RubertEmbedder = RuBERTService
