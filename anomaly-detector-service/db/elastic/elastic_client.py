from elasticsearch import Elasticsearch
from functools import lru_cache

@lru_cache()
def get_es_client():
    return Elasticsearch(
        "http://localhost:9200",
        verify_certs=False
    )