import numpy as np
from sklearn.ensemble import IsolationForest

def calculate_z_score(data: list[float]) -> tuple[float, float, float] | None:
    if len(data) < 2:
        return None  # 데이터가 너무 적음

    avg = np.mean(data[:-1])  # 마지막 값 제외하고 기준 계산
    std = np.std(data[:-1])
    current = data[-1]

    if std == 0:
        return None  # 표준편차 0이면 계산 불가

    z = (current - avg) / std
    return z, avg, std



def calculate_isolation_score(
        data: list[float],
        contamination: float = 0.05,
        random_state: int = 42
) -> tuple[int, float] | None:
    """
    Isolation Forest 기반 이상 탐지.
    마지막 값이 이상치인지 판단.

    :param data: 시계열 사용률 리스트 (예: 60개)
    :param contamination: 이상치 비율 (전체 중 몇 %가 이상이라고 볼지)
    :param random_state: 재현 가능성 위한 시드
    :return: (예측값, 결정 함수 점수) 또는 None
             예측값: -1(이상), 1(정상)
             결정 점수: 낮을수록 이상 가능성 높음
    """
    if len(data) < 10:  # 너무 적으면 학습 불안정
        return None

    X = np.array(data).reshape(-1, 1)

    model = IsolationForest(contamination=contamination, random_state=random_state)
    model.fit(X)

    # 마지막 값에 대한 판단만 리턴
    last_value = X[-1].reshape(1, -1)
    prediction = model.predict(last_value)[0]     # -1: 이상, 1: 정상
    score = model.decision_function(last_value)[0]  # 낮을수록 이상

    return prediction, score