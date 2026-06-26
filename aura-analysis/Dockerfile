## syntax=docker/dockerfile:1.7

FROM python:3.12-slim AS ml-base

WORKDIR /service

ENV PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    PIP_DEFAULT_TIMEOUT=120 \
    HF_HOME=/opt/huggingface \
    TRANSFORMERS_CACHE=/opt/huggingface \
    HUGGINGFACE_HUB_CACHE=/opt/huggingface

ARG RUBERT_MODEL_NAME=cointegrated/rubert-tiny2

COPY requirements.txt .

RUN --mount=type=cache,target=/root/.cache/pip \
    python -m pip install --upgrade pip setuptools wheel \
    && python -m pip install -r requirements.txt

RUN mkdir -p /opt/huggingface

RUN --mount=type=cache,target=/root/.cache/huggingface \
    python -c "from transformers import AutoTokenizer, AutoModel; cache_dir='/root/.cache/huggingface'; AutoTokenizer.from_pretrained('${RUBERT_MODEL_NAME}', cache_dir=cache_dir); AutoModel.from_pretrained('${RUBERT_MODEL_NAME}', cache_dir=cache_dir)" \
    && cp -a /root/.cache/huggingface/. /opt/huggingface/

FROM python:3.12-slim AS runtime

WORKDIR /service

ENV PYTHONUNBUFFERED=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1 \
    HF_HOME=/opt/huggingface \
    TRANSFORMERS_CACHE=/opt/huggingface \
    HUGGINGFACE_HUB_CACHE=/opt/huggingface

COPY --from=ml-base /usr/local /usr/local
COPY --from=ml-base /opt/huggingface /opt/huggingface

COPY app app

EXPOSE 8090

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
