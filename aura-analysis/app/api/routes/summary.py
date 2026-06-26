from fastapi import APIRouter, Depends

from app.api.dependencies import get_summarization_service
from app.domain.schemas import SummarizeRequest, SummarizeResponse
from app.services.summarization_service import SummarizationService


router = APIRouter(tags=["summary"])


@router.post("/summarize", response_model=SummarizeResponse)
def summarize(
    request: SummarizeRequest,
    summarization_service: SummarizationService = Depends(get_summarization_service),
) -> SummarizeResponse:
    result = summarization_service.summarize(request.text)
    return SummarizeResponse(
        summary=result.summary,
        modelVersion=result.modelVersion,
    )
