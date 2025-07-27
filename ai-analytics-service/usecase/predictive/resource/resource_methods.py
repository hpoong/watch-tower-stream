import io

import pandas as pd
import matplotlib.pyplot as plt
from starlette.responses import StreamingResponse


def preprocess_usage_data(usage, window_size=10, predict_horizon=10):
    """
    시계열 데이터 → XGBoost가 학습 가능한 입력 데이터로 변환
    기초적인 전처리 흐름을 보여주는 베이스라인 예제


    usage: list of float, 시계열 사용률 (예: 180개)
    window_size: 입력에 사용할 과거 데이터 개수
    predict_horizon: 예측하려는 미래 시점 거리 (예: +10분 후)

    return: X (2D), y (1D)
    """
    X, y = [], []
    total_points = len(usage)

    for i in range(total_points - window_size - predict_horizon + 1):
        input_window = usage[i : i + window_size]
        target = usage[i + window_size + predict_horizon - 1]

        X.append(input_window)
        y.append(target)

    return X, y




def preprocess_usage_dataframe(df: pd.DataFrame, window_size=10, predict_horizon=10) -> tuple:
    """
    Pandas DataFrame 기반 슬라이딩 윈도우 전처리 함수.
    시간 기반 피처(hour, minute, weekday 등 포함 가능)

    Parameters:
    - df: timestamp + usagePercent + 피처 컬럼 포함 DataFrame
    - window_size: 과거 사용률 개수
    - predict_horizon: 미래 예측 거리 (예: t+10)

    Returns:
    - X: List of feature rows
    - y: List of labels
    - columns: List of feature names (for reference or DataFrame 변환 시 사용)
    """
    X, y = [], []
    total_rows = len(df)

    for i in range(total_rows - window_size - predict_horizon + 1):
        # 사용률 슬라이딩 윈도우
        usage_window = df["usagePercent"].iloc[i : i + window_size].tolist()

        # 시간 피처 (t 시점의 시간 정보 사용)
        t_row = df.iloc[i + window_size]
        time_features = [
            t_row.get("hour", 0),
            t_row.get("minute", 0),
            t_row.get("weekday", 0),
            t_row.get("is_weekend", 0),
        ]

        features = usage_window + time_features
        X.append(features)

        # 타깃: t + predict_horizon - 1 시점의 사용률
        y_val = df["usagePercent"].iloc[i + window_size + predict_horizon - 1]
        y.append(y_val)

    # 컬럼 이름 구성
    usage_cols = [f"usage_t-{window_size - i}" for i in range(window_size)]
    time_cols = ["hour", "minute", "weekday", "is_weekend"]
    columns = usage_cols + time_cols

    return X, y, columns





def add_time_features(df: pd.DataFrame) -> pd.DataFrame:
    """
    timestamp 컬럼 기준으로 다양한 시간 기반 피처를 생성
    """

    df["timestamp"] = df["timestamp"].dt.tz_convert("Asia/Seoul")
    df["hour"] = df["timestamp"].dt.hour
    df["minute"] = df["timestamp"].dt.minute
    df["weekday"] = df["timestamp"].dt.weekday
    df["month"] = df["timestamp"].dt.month

    # 주말 여부
    df["is_weekend"] = df["weekday"].apply(lambda x: 1 if x >= 5 else 0)

    # 계절 (한국 기준)
    def get_season(month):
        if month in [3, 4, 5]:
            return 0  # 봄
        elif month in [6, 7, 8]:
            return 1  # 여름
        elif month in [9, 10, 11]:
            return 2  # 가을
        else:
            return 3  # 겨울

    df["season"] = df["month"].apply(get_season)

    # 야간 여부 (0~6시)
    df["is_night"] = df["hour"].apply(lambda h: 1 if 0 <= h <= 6 else 0)

    # 업무 시간 여부 (9~18시)
    df["is_business_hour"] = df["hour"].apply(lambda h: 1 if 9 <= h <= 18 else 0)


def build_training_data(df, window_size=10, predict_horizon=10):
    """
    슬라이딩 윈도우 기반 X, y 재구성 (시간 피처 포함)
    - df: timestamp 포함된 시계열 DataFrame
    - window_size: 과거 몇 개 포인트로 예측할지
    - predict_horizon: 얼마나 미래를 예측할지 (예: 10분 후)
    """
    X, y = [], []
    total = len(df)

    for i in range(total - window_size - predict_horizon + 1):
        # 과거 사용률 시퀀스
        usage_window = df["usagePercent"].iloc[i : i + window_size].tolist()

        # 예측 기준 시점(t)의 시간 피처
        t_row = df.iloc[i + window_size]
        time_features = [
            t_row["hour"],
            t_row["minute"],
            t_row["weekday"],
            t_row["is_weekend"],
            t_row["month"],
            t_row["season"],
            t_row["is_night"],
            t_row["is_business_hour"],
        ]

        # 입력 피처 구성
        features = usage_window + time_features
        X.append(features)

        # 타깃 값: t + predict_horizon - 1 시점의 usage
        target = df["usagePercent"].iloc[i + window_size + predict_horizon - 1]
        y.append(target)




def plot_usage_series(df):
    fig, ax = plt.subplots(figsize=(15, 4))
    ax.plot(df['timestamp'], df['usagePercent'], label='Usage %')
    ax.set_title("System Resource Usage Over Time")
    ax.set_xlabel("Timestamp")
    ax.set_ylabel("Usage (%)")
    ax.grid(True)
    ax.legend()
    plt.tight_layout()

    # 이미지로 변환
    buf = io.BytesIO()
    fig.savefig(buf, format="png")
    buf.seek(0)

    # 리턴 (image/png)
    return StreamingResponse(buf, media_type="image/png")

