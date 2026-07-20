package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.LeaseCaseCreateRequest;
import com.example.vivizip.document.dto.LeaseCaseProgressResponse;
import com.example.vivizip.document.dto.LeaseCaseResponse;
import com.example.vivizip.document.dto.LeaseCaseSummaryResponse;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseCaseStatus;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.matching.dto.MatchApplicationStatus;
import com.example.vivizip.matching.service.MatchingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LeaseCaseService {

    private static final String DEFAULT_NAME = "우리집";

    private static final int STEP_MATE_MATCHING = 1;
    private static final int STEP_HOUSE_HUNTING = 2;
    private static final int STEP_CONTRACT_REVIEW = 3;
    private static final String STEP_MATE_MATCHING_LABEL = "메이트 매칭";
    private static final String STEP_HOUSE_HUNTING_LABEL = "집 구하는 중";
    private static final String STEP_CONTRACT_REVIEW_LABEL = "계약서 검토";

    private final LeaseCaseRepository leaseCaseRepository;
    private final MatchingService matchingService;

    @Transactional
    public LeaseCaseResponse create(Long userId, LeaseCaseCreateRequest request) {
        String name = (request.name() == null || request.name().isBlank()) ? DEFAULT_NAME : request.name();
        LeaseCase leaseCase = LeaseCase.create(
                userId,
                name,
                request.roadAddress(),
                request.detailAddress()
        );
        return LeaseCaseResponse.from(leaseCaseRepository.save(leaseCase));
    }

    @Transactional(readOnly = true)
    public List<LeaseCaseSummaryResponse> getMyLeaseCases(Long userId) {
        return leaseCaseRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(LeaseCaseSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public LeaseCaseResponse getMyLeaseCase(Long userId, Long leaseCaseId) {
        LeaseCase leaseCase = leaseCaseRepository.findByIdAndUserId(leaseCaseId, userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.LEASE_CASE_NOT_FOUND));
        return LeaseCaseResponse.from(leaseCase);
    }

    // 부메랑 진행 과정 3단계: 1)메이트 매칭 2)집 구하는 중 3)계약서 검토.
    // 새 테이블 없이 기존 매칭 상태(matching 도메인) + LeaseCase.status로만 계산한다.
    // 여러 케이스를 등록했으면 가장 최근 생성된(DELETED 제외) 케이스 하나만 기준으로 본다.
    @Transactional(readOnly = true)
    public LeaseCaseProgressResponse getProgress(Long userId) {
        if (matchingService.getMatchStatus(userId).status() != MatchApplicationStatus.MATCHED) {
            return new LeaseCaseProgressResponse(STEP_MATE_MATCHING, STEP_MATE_MATCHING_LABEL);
        }

        Optional<LeaseCase> latestCase = leaseCaseRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(leaseCase -> leaseCase.getStatus() != LeaseCaseStatus.DELETED)
                .findFirst();

        boolean contractReviewed = latestCase.isPresent() && latestCase.get().getStatus() == LeaseCaseStatus.COMPLETED;
        if (contractReviewed) {
            return new LeaseCaseProgressResponse(STEP_CONTRACT_REVIEW, STEP_CONTRACT_REVIEW_LABEL);
        }
        return new LeaseCaseProgressResponse(STEP_HOUSE_HUNTING, STEP_HOUSE_HUNTING_LABEL);
    }
}