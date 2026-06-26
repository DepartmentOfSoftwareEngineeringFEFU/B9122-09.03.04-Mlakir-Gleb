class AppError(Exception):
    def __init__(self, detail: str, error_code: str, status_code: int = 500) -> None:
        super().__init__(detail)
        self.detail = detail
        self.error_code = error_code
        self.status_code = status_code


class UnsupportedAnalysisModeError(AppError):
    def __init__(self, mode: str) -> None:
        super().__init__(
            detail=f"Unsupported analysis mode: {mode}",
            error_code="UNSUPPORTED_ANALYSIS_MODE",
            status_code=500,
        )


class ModelNotFoundError(AppError):
    def __init__(self, path: str) -> None:
        super().__init__(
            detail=f"Model artifacts not found: {path}",
            error_code="MODEL_NOT_FOUND",
            status_code=500,
        )
