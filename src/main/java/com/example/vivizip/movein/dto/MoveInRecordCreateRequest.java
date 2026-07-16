package com.example.vivizip.movein.dto;

import com.example.vivizip.movein.enums.DefectType;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record MoveInRecordCreateRequest(

        @NotNull(message = "leaseCaseId는 필수입니다.")
        Long leaseCaseId,

        String memo,

        List<DefectType> defects
) {}
