package com.example.vivizip.movein.dto;

import com.example.vivizip.movein.entity.MoveInRecord;

import java.time.LocalDateTime;
import java.util.List;

public record MoveInRecordResponse(

        Long id,
        Long leaseCaseId,   // TODO: LeaseCase 연결 후 houseName 필드 추가
        String memo,
        List<DefectResponse> defects,
        List<PhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MoveInRecordResponse from(MoveInRecord record) {
        return new MoveInRecordResponse(
                record.getId(),
                record.getLeaseCaseId(),
                record.getMemo(),
                record.getDefects().stream().map(DefectResponse::from).toList(),
                record.getPhotos().stream().map(PhotoResponse::from).toList(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
