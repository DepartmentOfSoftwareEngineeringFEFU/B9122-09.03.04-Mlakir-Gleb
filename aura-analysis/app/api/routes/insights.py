from fastapi import APIRouter, Depends

from app.api.dependencies import get_insights_service
from app.domain.schemas import InsightsRequest, InsightsResponse
from app.services.insights_service import InsightsService


router = APIRouter(tags=["insights"])


@router.post("/insights", response_model=InsightsResponse)
def insights(
    request: InsightsRequest,
    insights_service: InsightsService = Depends(get_insights_service),
) -> InsightsResponse:
    result = insights_service.build_insights(
        organization_name=request.organizationName,
        reviews=request.reviews,
    )
    return InsightsResponse(
        summary=result.summary,
        strengths=result.strengths,
        weaknesses=result.weaknesses,
        recommendations=result.recommendations,
        modelVersion=result.modelVersion,
    )
