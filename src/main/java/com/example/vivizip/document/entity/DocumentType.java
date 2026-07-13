package com.example.vivizip.document.entity;

// 검토 대상 서류 4종 중 이번 단계에서는 LEASE_CONTRACT(계약서)만 지원.
// 나머지(등기부등본/건축물대장/중개대상물확인서)는 값 추가 + 전용 파이프라인 구현으로 확장.
public enum DocumentType {
    LEASE_CONTRACT
}
