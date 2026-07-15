package com.example.vivizip.movein.dto;

import com.example.vivizip.movein.enums.DefectType;

import java.util.List;

public record MoveInRecordUpdateRequest(

        // null이면 변경 없음
        String memo,

        // null이면 변경 없음, 빈 리스트면 전체 삭제
        List<DefectType> defects,

        // 삭제할 사진 ID 목록
        List<Long> deletePhotoIds
) {}
