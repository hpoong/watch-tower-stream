import numpy as np

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