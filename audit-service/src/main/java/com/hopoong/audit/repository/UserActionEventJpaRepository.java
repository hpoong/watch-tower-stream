package com.hopoong.audit.repository;

import com.hopoong.audit.persistence.entity.UserActionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;


public interface UserActionEventJpaRepository extends JpaRepository<UserActionEventEntity, Long> {
}
