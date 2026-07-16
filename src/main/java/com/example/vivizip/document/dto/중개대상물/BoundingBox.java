package com.example.vivizip.document.dto.중개대상물;

// CLOVA OCR이 준 픽셀 좌표를 그대로 담는다(정규화하지 않음).
// EXIF 회전 태그가 있는 사진은 저장된 원본 픽셀 크기와 실제 보이는 방향이 달라 0~1 정규화가
// 오히려 왜곡을 만들 수 있어서, 이미지 크기 추정 없이 CLOVA가 이미 해석한 픽셀값을 그대로 내려준다.
public record BoundingBox(
        double x,
        double y,
        double width,
        double height
) {
}