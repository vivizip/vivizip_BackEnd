package com.example.vivizip.movein.repository;

import com.example.vivizip.movein.entity.MoveInRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MoveInRecordRepository extends JpaRepository<MoveInRecord, Long> {

    Optional<MoveInRecord> findByLeaseCaseId(Long leaseCaseId);

    List<MoveInRecord> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<MoveInRecord> findByUserIdOrderByCreatedAtAsc(Long userId);

    boolean existsByLeaseCaseId(Long leaseCaseId);
}
