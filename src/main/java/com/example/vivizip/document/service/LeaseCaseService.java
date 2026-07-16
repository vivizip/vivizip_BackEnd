package com.example.vivizip.document.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.dto.LeaseCaseCreateRequest;
import com.example.vivizip.document.dto.LeaseCaseResponse;
import com.example.vivizip.document.dto.LeaseCaseSummaryResponse;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaseCaseService {

    private final LeaseCaseRepository leaseCaseRepository;

    @Transactional
    public LeaseCaseResponse create(Long userId, LeaseCaseCreateRequest request) {
        LeaseCase leaseCase = LeaseCase.create(
                userId,
                request.name(),
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
}
