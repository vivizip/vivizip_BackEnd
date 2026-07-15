package com.example.vivizip.movein.dto;

import com.example.vivizip.movein.entity.MoveInPhoto;

public record PhotoResponse(Long id, String fileUrl, int sortOrder) {

    public static PhotoResponse from(MoveInPhoto photo) {
        return new PhotoResponse(photo.getId(), photo.getFileUrl(), photo.getSortOrder());
    }
}
