from datetime import datetime, timedelta


def build_query(server_name: str, resource_name: str, minutes: int = 60):
    now = datetime.utcnow()
    start_time = now - timedelta(minutes=minutes)

    return {
        "size": 1000,
        "query": {
            "bool": {
                "must": [
                    {"term": {"serverName": server_name}},
                    {"term": {"resourceName": resource_name}},
                    {"range": {
                        "timestamp": {
                            "gte": start_time.isoformat(),
                            "lte": now.isoformat()
                        }
                    }}
                ]
            }
        },
        "sort": [{"timestamp": "asc"}],
        "_source": ["timestamp", "usagePercent"]
    }
