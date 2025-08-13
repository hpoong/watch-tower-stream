DO
$$
DECLARE
current_date date := now();
    partition_name text;
    start_time timestamptz;
    end_time timestamptz;
BEGIN
    -------------------------------------------
    -- 1. 앞으로 2개월치 파티션 미리 생성
    -------------------------------------------
FOR i IN 0..2 LOOP
        start_time := date_trunc('month', current_date + INTERVAL '1 month' * i);
        end_time := start_time + INTERVAL '1 month';
        partition_name := 'user_event_' || to_char(start_time, 'YYYYMM');

        -- 정확한 테이블 존재 여부 확인
        IF NOT EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE n.nspname = 'um'
              AND c.relname = partition_name
        ) THEN
            RAISE NOTICE 'Creating partition % (FROM % TO %)...', partition_name, start_time, end_time;
EXECUTE format(
        'CREATE TABLE um.%I PARTITION OF um.user_event FOR VALUES FROM (%L) TO (%L)',
        partition_name,
        start_time,
        end_time
        );
END IF;
END LOOP;

    -------------------------------------------
    -- 2. 6개월이 지난 오래된 파티션 삭제 (DROP)
    -------------------------------------------
FOR i IN 7..12 LOOP
        start_time := date_trunc('month', current_date - INTERVAL '1 month' * i);
        partition_name := 'user_event_' || to_char(start_time, 'YYYYMM');

        IF EXISTS (
            SELECT 1
            FROM pg_class c
            JOIN pg_namespace n ON c.relnamespace = n.oid
            WHERE n.nspname = 'um'
              AND c.relname = partition_name
        ) THEN
            RAISE NOTICE 'Dropping old partition %...', partition_name;
EXECUTE format('DROP TABLE um.%I', partition_name);
END IF;
END LOOP;
END
$$;
