from fastapi import APIRouter, Depends, Response, status

from app.api.dependencies import get_analysis_service
from app.domain.schemas import HealthResponse, InfoResponse
from app.services.analysis_service import AnalysisService

router = APIRouter(tags=["system"])


@router.get("/health", response_model=HealthResponse)
def health(
    analysis_service: AnalysisService = Depends(get_analysis_service),
) -> HealthResponse:
    return HealthResponse(
        status="degraded" if analysis_service.degraded else "ok",
        mode=analysis_service.mode,
        requestedMode=analysis_service.requested_mode,
        degraded=analysis_service.degraded,
        degradationReason=analysis_service.degradation_reason,
        modelVersion=analysis_service.model_version,
    )


@router.get("/ready", response_model=HealthResponse)
def ready(
    response: Response,
    analysis_service: AnalysisService = Depends(get_analysis_service),
) -> HealthResponse:
    degraded = analysis_service.degraded
    response.status_code = status.HTTP_503_SERVICE_UNAVAILABLE if degraded else status.HTTP_200_OK
    return HealthResponse(
        status="degraded" if degraded else "ready",
        mode=analysis_service.mode,
        requestedMode=analysis_service.requested_mode,
        degraded=degraded,
        degradationReason=analysis_service.degradation_reason,
        modelVersion=analysis_service.model_version,
    )


@router.get("/info", response_model=InfoResponse)
def info(
    analysis_service: AnalysisService = Depends(get_analysis_service),
) -> InfoResponse:
    return InfoResponse(**analysis_service.info())
