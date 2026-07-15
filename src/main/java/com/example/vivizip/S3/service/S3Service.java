package com.example.vivizip.S3.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final List<String> ALLOWED_IMAGE_TYPES =
            List.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> ALLOWED_DOC_TYPES =
            List.of("image/jpeg", "image/png", "application/pdf");
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;  // 10MB
    private static final long MAX_DOC_SIZE = 20 * 1024 * 1024;    // 20MB

    // ── public 업로드 (프로필/채팅/리포트) → 접근 가능한 URL 반환 ──
    public String uploadPublic(MultipartFile file, String folder) {
        validate(file, ALLOWED_IMAGE_TYPES, MAX_IMAGE_SIZE);
        String key = generateKey(folder, file.getOriginalFilename());
        putObject(file, key);
        return toPublicUrl(key);
    }

    // ── private 업로드 (계약서/서류) → key만 반환 (DB에 저장) ──
    public String uploadPrivate(MultipartFile file, String folder) {
        validate(file, ALLOWED_DOC_TYPES, MAX_DOC_SIZE);
        String key = generateKey(folder, file.getOriginalFilename());
        putObject(file, key);
        return key;  // URL 아님! key만. 조회 시 presigned 발급
    }

    // ── 업로드 도중 실패 시 이미 올라간 객체 정리용 ──
    public void delete(String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            log.warn("S3 정리 실패 (key={}): {}", key, e.getMessage());
        }
    }

    // ── presigned URL 발급 (private 파일 조회용, 5분 유효) ──
    public String generatePresignedUrl(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    // ── 내부 유틸 ──
    private void putObject(MultipartFile file, String key) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .contentLength(file.getSize())
                .build();

        try (var inputStream = file.getInputStream()) {
            s3Client.putObject(request, RequestBody.fromInputStream(inputStream, file.getSize()));
            log.info("S3 업로드 완료: {}", key);
        } catch (IOException e) {
            throw new RuntimeException("파일 읽기 실패: " + e.getMessage(), e);
        } catch (SdkException e) {
            throw new RuntimeException("S3 업로드 실패: " + e.getMessage(), e);
        }
    }

    private void validate(MultipartFile file, List<String> allowedTypes, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("파일이 비어있습니다");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("파일 크기 초과 (최대 " + (maxSize / 1024 / 1024) + "MB)");
        }
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("허용되지 않은 파일 형식: " + file.getContentType());
        }
    }

    // folder/UUID.확장자
    private String generateKey(String folder, String originalFilename) {
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return folder + "/" + UUID.randomUUID() + ext;
    }

    private String toPublicUrl(String key) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
