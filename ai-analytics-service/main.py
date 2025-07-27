from fastapi import FastAPI, Request, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sklearn.metrics import mean_squared_error
from sklearn.model_selection import train_test_split
import xgboost as xgb
from common.service_type_enum import ServiceTypeEnum
from db.elastic.elastic_client import get_es_client
from fastapi import Depends
from exception.exception_handler import add_exception_handlers
from response.success_response import SuccessResponse
from security.security_config import GlobalAuthMiddleware
from usecase.anomaly_detector.fetcher import fetch_usage, fetch_usage_dataframe
from usecase.anomaly_detector.detection_methods import calculate_z_score, calculate_isolation_score
from usecase.predictive.resource.resource_methods import preprocess_usage_data, preprocess_usage_dataframe, \
    add_time_features, build_training_data, plot_usage_series, plot_prediction_result, tune_xgboost_hyperparameters

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

    for server in servers:
        for resource in resources:
            # 데이터 수집 및 전처리
            df = fetch_usage_dataframe(es, server, resource, 30)
            df = add_time_features(df)
            X, y = build_training_data(df)

            if not X or not y:
                print("데이터 부족으로 학습 불가")
                return {"error": "데이터 부족"}

            # 학습/검증 분리
            X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, shuffle=False)

            # 하이퍼파라미터 튜닝 + 모델 학습
            model, best_params, best_cv_rmse = tune_xgboost_hyperparameters(X_train, y_train)
            print(f"[{server}/{resource}] BestParams={best_params}, CV RMSE={best_cv_rmse:.3f}")

            # 예측 및 평가
            y_pred = model.predict(X_test)
            rmse = mean_squared_error(y_test, y_pred, squared=False)
            print(f"[{server}/{resource}] Test RMSE: {rmse:.3f}")
            print("예측 결과 샘플:", list(zip(y_test[:5], y_pred[:5])))

            # 시각화 응답
            return plot_prediction_result(y_test, y_pred)