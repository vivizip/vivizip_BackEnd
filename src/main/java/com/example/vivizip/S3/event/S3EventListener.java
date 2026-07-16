package com.example.vivizip.S3.event;

import com.example.vivizip.S3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3EventListener {

    private final S3Service s3Service;

    /**
     * DB 커밋이 완료된 이후에만 S3 삭제를 수행한다.
     * 트랜잭션 롤백 시에는 이 메서드가 호출되지 않으므로 데이터 불일치가 발생하지 않는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onS3Delete(S3DeleteEvent event) {
        event.keys().forEach(key -> {
            try {
                s3Service.delete(key);
            } catch (Exception e) {
                log.error("[S3] 커밋 후 삭제 실패 (수동 정리 필요): key={}, error={}", key, e.getMessage());
            }
        });
    }
}
