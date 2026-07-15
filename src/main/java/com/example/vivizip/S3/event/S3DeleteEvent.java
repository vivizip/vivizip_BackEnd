package com.example.vivizip.S3.event;

import java.util.List;

/**
 * DB 커밋 확정 후 S3 객체를 삭제하기 위한 이벤트.
 * S3 삭제는 롤백 불가능한 외부 호출이므로 트랜잭션 커밋 이후에 수행해야 한다.
 */
public record S3DeleteEvent(List<String> keys) {}
