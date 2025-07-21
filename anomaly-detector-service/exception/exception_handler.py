from fastapi import Request, FastAPI
from fastapi.responses import JSONResponse
from common.service_type_enum import ServiceTypeEnum
from exception.business_exception import BusinessException
from response.error_response import ErrorResponse
import logging
from fastapi import HTTPException

logger = logging.getLogger("uvicorn.error")

def add_exception_handlers(app: FastAPI):


    @app.exception_handler(Exception)
    async def exception_handler(request: Request, ex: Exception):
        logger.warning("Unhandled Exception", exc_info=ex)
        error_response = ErrorResponse.with_message(
            service_type=ServiceTypeEnum.SERVER,
            message="관리자 확인 필요"
        )
        return JSONResponse(status_code=500, content=error_response.dict())

    
    @app.exception_handler(BusinessException)
    async def business_exception_handler(request: Request, ex: BusinessException):
        logger.warning("BusinessException", exc_info=ex)
        error_response = ErrorResponse.with_message(
            service_type=ex.service_type,
            message=ex.message
        )
        return JSONResponse(status_code=401, content=error_response.dict())


