package com.example.vivizip.matching.repository;

import com.example.vivizip.matching.entity.Match;
import com.example.vivizip.matching.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchRepository extends JpaRepository<Match, Long> {
    Optional<Match> findByStudentIdAndStatus(Long studentId, MatchStatus status);

    Optional<Match> findBySupporterIdAndStatus(Long supporterId, MatchStatus status);

    List<Match> findByStatus(MatchStatus status);
}