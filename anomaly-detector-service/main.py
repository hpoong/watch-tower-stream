from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from common.service_type_enum import ServiceTypeEnum
from db.elastic.elastic_client import get_es_client
from fastapi import Depends
from exception.exception_handler import add_exception_handlers
from response.success_response import SuccessResponse
from security.security_config import GlobalAuthMiddleware
from usecase.detector.zscore import calculate_z_score
from usecase.detector.fetcher import fetch_usage

# middleware
app = FastAPI()
add_exception_handlers(app)
app.add_middleware(GlobalAuthMiddleware)

# CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/success")
def success_example(es=Depends(get_es_client)):
    data = es.count(index="system_metrics")
    print(data)
    return SuccessResponse.with_data(
        service_type=ServiceTypeEnum.SERVER,
        data={"data": "asd"}
    )



@app.get("/test")
def build_recent_metrics_query(es=Depends(get_es_client)):
    # servers = ["Server-01", "Server-02", "Server-03", "Server-04"]
    # resources = ["CPU", "Memory", "Disk"]
    servers = ["Server-01"]
    resources = ["CPU"]

    for server in servers:
        for resource in resources:
            usage = fetch_usage(es, server, resource)
            if not usage:
                continue

            result = calculate_z_score(usage)
            if result is None:
                print(f"{server}/{resource}: insufficient or flat data")
                continue

            z, avg, std = result
            print(f"{server}:{resource} → z-score={z:.2f}, avg={avg:.2f}, std={std:.2f}")
            if abs(z) > 2.5:
                print(f" 이상 감지!")