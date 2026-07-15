package com.example.vivizip.movein.dto;

import com.example.vivizip.movein.entity.MoveInDefect;
import com.example.vivizip.movein.enums.DefectType;

public record DefectResponse(DefectType type, String label) {

    public static DefectResponse from(MoveInDefect defect) {
        return new DefectResponse(defect.getDefectType(), defect.getDefectType().getLabel());
    }
}
