import time

class TimeUtil:

    @staticmethod
    def get_unix_timestamp_ms() -> str:
        return str(int(time.time() * 1000))