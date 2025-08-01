import pandas as pd
from fastapi import Depends
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sklearn.model_selection import train_test_split

from common.service_type_enum import ServiceTypeEnum
from db.elastic.elastic_client import get_es_client
from exception.exception_handler import add_exception_handlers
from response.success_response import SuccessResponse
from security.security_config import GlobalAuthMiddleware
from usecase.anomaly_detector.detection_methods import calculate_z_score, calculate_isolation_score
from usecase.anomaly_detector.fetcher import fetch_usage, fetch_usage_dataframe
from usecase.predictive.resource.resource_methods import add_time_features, build_training_data, plot_prediction_result, \
    tune_xgboost_hyperparameters, \
    predict_future_steps

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



@app.get("/anomaly_detector")
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


@app.get("/predictive-resource")
def predict_resource_usage(es=Depends(get_es_client)):
    servers = ["Server-01"]
    resources = ["CPU"]
    window_size = 10           # 과거 10분 사용률 사용
    predict_horizon = 60       # 앞으로 60분 예측

    for server in servers:
        for resource in resources:
            # 데이터 수집
            df = fetch_usage_dataframe(es, server, resource, 120)
            df = add_time_features(df)
            print(df.tail(5))

            X, y = build_training_data(df, window_size, predict_horizon)
            if not X or not y:
                return {"error": "데이터 부족"}

            # 모델 학습
            X_train, _, y_train, _ = train_test_split(X, y, test_size=0.2, shuffle=False)
            model, best_params, best_cv_rmse = tune_xgboost_hyperparameters(X_train, y_train)

            # 현재 이후 60분간 예측
            timestamps, predictions = predict_future_steps(
                df=df,
                model=model,
                window_size=window_size,
                predict_steps=predict_horizon
            )

            return plot_prediction_result(
                y_true=predictions,
                y_pred=predictions,  # 비교 대상 없으므로 동일하게
                timestamps=pd.Series(timestamps)
            )