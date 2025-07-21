from fastapi.responses import JSONResponse

from common.service_type_enum import ServiceTypeEnum
from response.error_response import ErrorResponse


def forbidden_response():
    content = ErrorResponse.with_message(service_type=ServiceTypeEnum.SECURITY_LOGIN, message="Role empty.")
    return JSONResponse(status_code=480, content=content.dict())
