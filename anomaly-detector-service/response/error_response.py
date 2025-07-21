from response.common_response import CommonResponse
from pydantic import Field
from typing import Optional, Any
from common.service_type_enum import ServiceTypeEnum


class ErrorResponse(CommonResponse):
    data: Optional[Any] = None

    @classmethod
    def _build(cls, service_type: ServiceTypeEnum, message: str, data: Any = None):
        return cls(
            success=False,
            serviceType=service_type,
            message=message,
            data=data
        )

    @classmethod
    def with_message(cls, service_type: ServiceTypeEnum, message: str):
        return cls._build(
            service_type=service_type,
            message=message,
            data=None
        )

    @classmethod
    def with_data(cls, service_type: ServiceTypeEnum, data: Any):
        return cls._build(
            service_type=service_type,
            message="Error",
            data=data
        )

