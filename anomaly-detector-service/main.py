from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from common.service_type_enum import ServiceTypeEnum
from db.elastic.elastic_client import get_es_client
from fastapi import Depends
from exception.exception_handler import add_exception_handlers
from response.success_response import SuccessResponse
from security.security_config import GlobalAuthMiddleware
from usecase.anomaly_detector.fetcher import fetch_usage
from usecase.anomaly_detector.detection_methods import calculate_z_score, calculate_isolation_score

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
    servers = ["Server-01"]
    resources = ["CPU"]

    for server in servers:
        for resource in resources:
            usage = fetch_usage(es, server, resource)
            if not usage:
                print(f"{server}/{resource}: 데이터 없음")
                continue

            # === z-score 기반 탐지 ===
            z_result = calculate_z_score(usage)
            if z_result is None:
                print(f"{server}/{resource}: z-score 계산 불가 (데이터 부족 or 표준편차 0)")
            else:
                z, avg, std = z_result
                print(f"{server}:{resource} → z-score={z:.2f}, avg={avg:.2f}, std={std:.2f}")
                if abs(z) > 2.5:
                    print(" → z-score 이상 감지!")

            # === Isolation Forest 기반 탐지 ===
            isolation_result = calculate_isolation_score(usage)
            if isolation_result is None:
                print(f"{server}/{resource}: Isolation Forest 계산 불가 (데이터 부족)")
            else:
                prediction, score = isolation_result
                print(f"{server}:{resource} → IsolationForest prediction={prediction}, score={score:.4f}")
                if prediction == -1:
                    print(" → Isolation Forest 이상 감지!")