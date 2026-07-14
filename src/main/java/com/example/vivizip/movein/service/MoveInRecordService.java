package com.example.vivizip.movein.service;

import com.example.vivizip.S3.enums.S3Folder;
import com.example.vivizip.S3.service.S3Service;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.movein.dto.*;
import com.example.vivizip.movein.entity.MoveInPhoto;
import com.example.vivizip.movein.entity.MoveInRecord;
import com.example.vivizip.movein.repository.MoveInRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MoveInRecordService {

    private final MoveInRecordRepository moveInRecordRepository;
    private final S3Service s3Service;

    // 생성 (memo + defects + 사진 multipart 같이)
    public MoveInRecordResponse create(Long userId, MoveInRecordCreateRequest request,
                                       List<MultipartFile> files) {
        if (moveInRecordRepository.existsByLeaseCaseId(request.leaseCaseId())) {
            throw new GeneralException(ErrorStatus.MOVE_IN_RECORD_ALREADY_EXISTS);
        }

        MoveInRecord record = MoveInRecord.create(userId, request.leaseCaseId(), request.memo());

        if (request.defects() != null) {
            record.replaceDefects(request.defects());
        }

        if (files != null) {
            for (MultipartFile file : files) {
                String s3Key = s3Service.uploadPublicReturnKey(file, S3Folder.MOVE_IN.getPath());
                String fileUrl = s3Service.toPublicUrl(s3Key);
                record.addPhoto(fileUrl, s3Key);
            }
        }

        // cascade로 defects, photos 함께 저장
        moveInRecordRepository.save(record);
        return MoveInRecordResponse.from(record);
    }

    // 상세 조회
    @Transactional(readOnly = true)
    public MoveInRecordResponse get(Long userId, Long id) {
        return MoveInRecordResponse.from(findAndValidate(userId, id));
    }

    // 수정 (memo/defects/사진 추가·삭제)
    public MoveInRecordResponse update(Long userId, Long id,
                                       MoveInRecordUpdateRequest request,
                                       List<MultipartFile> addFiles) {
        MoveInRecord record = findAndValidate(userId, id);

        if (request != null) {
            if (request.memo() != null) {
                record.updateMemo(request.memo());
            }
            if (request.defects() != null) {
                record.replaceDefects(request.defects());
            }
            if (request.deletePhotoIds() != null && !request.deletePhotoIds().isEmpty()) {
                List<MoveInPhoto> toDelete = record.getPhotos().stream()
                        .filter(p -> request.deletePhotoIds().contains(p.getId()))
                        .toList();
                toDelete.forEach(photo -> {
                    s3Service.delete(photo.getS3Key());
                    record.removePhoto(photo);
                });
            }
        }

        if (addFiles != null) {
            for (MultipartFile file : addFiles) {
                String s3Key = s3Service.uploadPublicReturnKey(file, S3Folder.MOVE_IN.getPath());
                String fileUrl = s3Service.toPublicUrl(s3Key);
                record.addPhoto(fileUrl, s3Key);
            }
        }

        return MoveInRecordResponse.from(record);
    }

    // 목록 조회
    @Transactional(readOnly = true)
    public List<MoveInRecordListResponse> getList(Long userId, String sort) {
        List<MoveInRecord> records = "oldest".equals(sort)
                ? moveInRecordRepository.findByUserIdOrderByCreatedAtAsc(userId)
                : moveInRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);

        return records.stream().map(MoveInRecordListResponse::from).toList();
    }

    // 삭제 (S3 포함)
    public void delete(Long userId, Long id) {
        MoveInRecord record = findAndValidate(userId, id);

        record.getPhotos().forEach(photo -> s3Service.delete(photo.getS3Key()));
        moveInRecordRepository.delete(record);
    }

    // ── 내부 검증 ──

    private MoveInRecord findAndValidate(Long userId, Long id) {
        MoveInRecord record = moveInRecordRepository.findById(id)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MOVE_IN_RECORD_NOT_FOUND));

        if (!record.getUserId().equals(userId)) {
            throw new GeneralException(ErrorStatus.MOVE_IN_RECORD_FORBIDDEN);
        }

        return record;
    }
}
