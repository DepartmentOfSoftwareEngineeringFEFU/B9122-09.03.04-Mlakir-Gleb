from fastapi import APIRouter, Depends

from app.api.dependencies import get_analysis_service
from app.domain.schemas import (
    AnalyzeRequest,
    AnalyzeResponse,
    BatchAnalyzeItemResponse,
    BatchAnalyzeRequest,
    BatchAnalyzeResponse,
)
from app.services.analysis_service import AnalysisService

router = APIRouter(tags=["analysis"])


@router.post("/analyze", response_model=AnalyzeResponse)
def analyze(
    request: AnalyzeRequest,
    analysis_service: AnalysisService = Depends(get_analysis_service),
) -> AnalyzeResponse:
    result = analysis_service.analyze(request.text)
    return AnalyzeResponse(
        sentiment=result.sentiment,
        topic=result.topic,
        keywords=result.keywords,
        confidence=result.confidence,
        modelVersion=result.modelVersion,
    )


@router.post("/analyze/batch", response_model=BatchAnalyzeResponse)
def analyze_batch(
    request: BatchAnalyzeRequest,
    analysis_service: AnalysisService = Depends(get_analysis_service),
) -> BatchAnalyzeResponse:
    items = []
    results = analysis_service.analyze_batch([item.text for item in request.items])
    for result in results:
        items.append(
            BatchAnalyzeItemResponse(
                sentiment=result.sentiment,
                topic=result.topic,
                keywords=result.keywords,
                confidence=result.confidence,
                modelVersion=result.modelVersion,
            )
        )
    return BatchAnalyzeResponse(items=items)
