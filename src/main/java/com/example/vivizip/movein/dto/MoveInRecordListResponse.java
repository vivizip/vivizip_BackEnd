package com.example.vivizip.movein.dto;

import com.example.vivizip.movein.entity.MoveInRecord;
import com.example.vivizip.movein.enums.DefectType;

import java.time.LocalDateTime;
import java.util.List;

public record MoveInRecordListResponse(

        Long id,
        Long leaseCaseId,   // TODO: LeaseCase 연결 후 houseName으로 대체
        String thumbnailUrl,
        List<DefectType> defects,
        LocalDateTime createdAt
) {
    public static MoveInRecordListResponse from(MoveInRecord record) {
        String thumbnail = record.getPhotos().isEmpty()
                ? null
                : record.getPhotos().get(0).getFileUrl();

        List<DefectType> defectTypes = record.getDefects().stream()
                .map(d -> d.getDefectType())
                .toList();

        return new MoveInRecordListResponse(
                record.getId(),
                record.getLeaseCaseId(),
                thumbnail,
                defectTypes,
                record.getCreatedAt()
        );
    }
}
