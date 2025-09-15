package com.hopoong.audit.repository;


import com.hopoong.audit.persistence.entity.OutboxEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, Long> {


    // 미발행 + 재시도 한도 미만만 조회 (낙관: 오래된 것부터)
    @Query(value = """
        SELECT * 
          FROM um.outbox_event 
         WHERE published_at IS NULL
           AND error_count < :maxRetry
         ORDER BY id ASC
         FOR UPDATE SKIP LOCKED
         LIMIT :limit
        """, nativeQuery = true)
    List<OutboxEventEntity> lockBatchForPublish(@Param("limit") int limit, @Param("maxRetry") int maxRetry);



    @Modifying
    @Query(value = "UPDATE um.outbox_event SET published_at = :publishedAt WHERE id = :id", nativeQuery = true)
    int markPublished(@Param("id") Long id, @Param("publishedAt") OffsetDateTime publishedAt);



    @Modifying
    @Query(value = """
        UPDATE um.outbox_event 
           SET error_count = error_count + 1, last_error = :lastError, published_at = NULL 
         WHERE id = :id
        """, nativeQuery = true)
    int markFailed(@Param("id") Long id, @Param("lastError") String lastError);

}
