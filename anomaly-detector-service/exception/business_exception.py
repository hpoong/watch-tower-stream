from common.service_type_enum import ServiceTypeEnum


class BusinessException(Exception):
    def __init__(self, message: str, service_type: ServiceTypeEnum):
        self.message = message
        self.service_type = service_type
        super().__init__(message)