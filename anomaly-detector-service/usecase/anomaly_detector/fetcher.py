from elasticsearch import Elasticsearch
from usecase.anomaly_detector.query_builder import build_query


def fetch_usage(
        es: Elasticsearch,
        server: str,
        resource: str,
        minutes: int = 60,
        index: str = "system_metrics"
) -> list[float]:
    query = build_query(server, resource, minutes)
    print(query)
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