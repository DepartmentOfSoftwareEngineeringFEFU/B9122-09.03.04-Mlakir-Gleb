FROM mirror.gcr.io/library/python:3.12-slim

WORKDIR /service

ENV PYTHONUNBUFFERED=1 \
    HF_HOME=/opt/huggingface \
    TRANSFORMERS_CACHE=/opt/huggingface \
    HUGGINGFACE_HUB_CACHE=/opt/huggingface

ARG RUBERT_MODEL_NAME=cointegrated/rubert-tiny2

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

RUN python -c "from transformers import AutoTokenizer, AutoModel; AutoTokenizer.from_pretrained('${RUBERT_MODEL_NAME}'); AutoModel.from_pretrained('${RUBERT_MODEL_NAME}')"

COPY . .

EXPOSE 8090

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8090"]
