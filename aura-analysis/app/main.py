import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.api.routes.analysis import router as analysis_router
from app.api.routes.insights import router as insights_router
from app.api.routes.summary import router as summary_router
from app.api.routes.system import router as system_router
from app.core.config import get_settings
from app.core.exceptions import AppError
from app.core.logging import configure_logging
from app.services.analysis_service import build_analysis_service
from app.services.insights_service import build_insights_service
from app.services.summarization_service import build_summarization_service

logger = logging.getLogger(__name__)


def create_app() -> FastAPI:
    get_settings.cache_clear()
    settings = get_settings()
    configure_logging(settings.log_level)

    @asynccontextmanager
    async def lifespan(app: FastAPI):
        logger.info(
            "Starting application name=%s requestedMode=%s",
            settings.app_name,
            settings.analysis_mode,
        )
        try:
            app.state.analysis_service = build_analysis_service(settings)
            app.state.summarization_service = build_summarization_service(settings)
            app.state.insights_service = build_insights_service(settings)
        except Exception:
            logger.exception("Failed to initialize application services")
            raise
        logger.info(
            "Analysis service active mode=%s modelVersion=%s",
            app.state.analysis_service.mode,
            app.state.analysis_service.model_version,
        )
        yield
        app.state.summarization_service.close()
        app.state.insights_service.close()

    app = FastAPI(title=settings.app_name, lifespan=lifespan)
    app.include_router(system_router)
    app.include_router(analysis_router)
    app.include_router(summary_router)
    app.include_router(insights_router)

    @app.exception_handler(AppError)
    async def app_error_handler(_: Request, exc: AppError) -> JSONResponse:
        logger.error("Application error code=%s detail=%s", exc.error_code, exc.detail)
        return JSONResponse(
            status_code=exc.status_code,
            content={"detail": exc.detail, "errorCode": exc.error_code},
        )

    @app.exception_handler(RequestValidationError)
    async def validation_error_handler(_: Request, exc: RequestValidationError) -> JSONResponse:
        return JSONResponse(
            status_code=422,
            content={
                "detail": "Validation error",
                "errorCode": "VALIDATION_ERROR",
                "errors": exc.errors(),
            },
        )

    @app.exception_handler(Exception)
    async def unexpected_error_handler(_: Request, exc: Exception) -> JSONResponse:
        logger.exception("Unhandled internal error: %s", exc)
        return JSONResponse(
            status_code=500,
            content={"detail": "Internal server error", "errorCode": "INTERNAL_ERROR"},
        )

    return app


app = create_app()
