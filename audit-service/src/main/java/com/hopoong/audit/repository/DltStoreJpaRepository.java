package com.hopoong.audit.repository;


import com.hopoong.audit.persistence.entity.DltStoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DltStoreJpaRepository extends JpaRepository<DltStoreEntity, Long> {

}
