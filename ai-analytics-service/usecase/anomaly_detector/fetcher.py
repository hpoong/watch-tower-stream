from elasticsearch import Elasticsearch
from usecase.anomaly_detector.query_builder import build_query
from datetime import datetime, timedelta
import pandas as pd


def fetch_usage(
        es: Elasticsearch,
        server: str,
        resource: str,
        minutes: int = 60,
        index: str = "system_metrics"
) -> list[float]:
    query = build_query(server, resource, minutes)
    # print(query)
    try:
        res = es.search(index=index, body=query)
        usage_list = [
            hit["_source"]["usagePercent"]
            for hit in res["hits"]["hits"]
        ]
        return usage_list
    except Exception as e:
        print(f"[ERROR] fetch_usage failed for {server}/{resource}: {e}")
        return []


def fetch_usage_dataframe(
        es: Elasticsearch,
        server: str,
        resource: str,
        minutes: int = 60,
        index: str = "system_metrics"
):
    # | timestamp           | usagePercent |
    # |---------------------|--------------|
    # | 2025-07-24 10:00:00 | 52.3         |
    # | 2025-07-24 10:01:00 | 53.1         |
    # | ...                 | ...          |

    query = build_query(server, resource, minutes)
    try:
        res = es.search(index=index, body=query)
        print(res)
        usage_list = [
            {
                "timestamp": hit["_source"]["timestamp"],
                "usagePercent": hit["_source"]["usagePercent"]
            }
            for hit in res["hits"]["hits"]
        ]
        print(usage_list)
        df = pd.DataFrame(usage_list)
        df["timestamp"] = pd.to_datetime(df["timestamp"])
        return df
    except Exception as e:
        print(f"[ERROR] fetch_usage_dataframe failed for {server}/{resource}: {e}")
        return pd.DataFrame(columns=["timestamp", "usagePercent"])