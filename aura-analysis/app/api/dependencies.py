from fastapi import Request

from app.services.analysis_service import AnalysisService
from app.services.insights_service import InsightsService
from app.services.summarization_service import SummarizationService


def get_analysis_service(request: Request) -> AnalysisService:
    return request.app.state.analysis_service


def get_summarization_service(request: Request) -> SummarizationService:
    return request.app.state.summarization_service


def get_insights_service(request: Request) -> InsightsService:
    return request.app.state.insights_service
