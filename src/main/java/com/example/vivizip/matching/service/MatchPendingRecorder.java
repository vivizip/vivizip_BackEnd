package com.example.vivizip.matching.service;

import com.example.vivizip.matching.entity.Match;
import com.example.vivizip.matching.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// applyMatch()가 후보를 못 찾아 MATCH_CANDIDATE_NOT_FOUND를 던지기 직전에 호출된다.
// 같은 트랜잭션 안에서 저장하면 뒤이은 예외로 롤백되어 PENDING 기록이 사라지므로,
// DocumentAnalysisRecorder와 같은 이유로 REQUIRES_NEW로 독립 커밋한다.
@Component
@RequiredArgsConstructor
class MatchPendingRecorder {

    private final MatchRepository matchRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savePending(Long studentId) {
        matchRepository.save(Match.createPending(studentId));
    }
}