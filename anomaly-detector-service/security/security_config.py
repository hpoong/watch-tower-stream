from fastapi import Request
from starlette.middleware.base import BaseHTTPMiddleware


class GlobalAuthMiddleware(BaseHTTPMiddleware):

    async def dispatch(self, request: Request, call_next):
        return await call_next(request)