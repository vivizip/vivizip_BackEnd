package com.example.vivizip.movein.controller;

import com.example.vivizip.movein.dto.*;
import com.example.vivizip.movein.enums.DefectType;
import com.example.vivizip.movein.service.MoveInRecordService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "입주 기록", description = "입주 상태 기록 CRUD API")
@RestController
@RequestMapping("/api/move-in-records")
@RequiredArgsConstructor
public class MoveInRecordController {

    private final MoveInRecordService moveInRecordService;

    @Operation(
            summary = "입주 기록 생성",
            description = "leaseCaseId(필수), memo(선택), defects(선택), files(선택 이미지)를 multipart로 전송합니다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public MoveInRecordResponse create(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam Long leaseCaseId,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) List<DefectType> defects,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        MoveInRecordCreateRequest request = new MoveInRecordCreateRequest(leaseCaseId, memo, defects);
        return moveInRecordService.create(user.getUserId(), request, files);
    }

    @Operation(summary = "입주 기록 상세 조회")
    @GetMapping("/{id}")
    public MoveInRecordResponse get(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        return moveInRecordService.get(user.getUserId(), id);
    }

    @Operation(
            summary = "입주 기록 수정",
            description = "memo, defects, deletePhotoIds, addFiles 모두 선택입니다. null인 필드는 변경되지 않습니다."
    )
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MoveInRecordResponse update(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id,
            @RequestParam(required = false) String memo,
            @RequestParam(required = false) List<DefectType> defects,
            @RequestParam(required = false) List<Long> deletePhotoIds,
            @RequestParam(value = "addFiles", required = false) List<MultipartFile> addFiles) {
        MoveInRecordUpdateRequest request = new MoveInRecordUpdateRequest(memo, defects, deletePhotoIds);
        return moveInRecordService.update(user.getUserId(), id, request, addFiles);
    }

    @Operation(
            summary = "입주 기록 목록 조회",
            description = "sort=latest(최신순, 기본값) / sort=oldest(오래된순)"
    )
    @GetMapping
    public List<MoveInRecordListResponse> getList(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "latest(최신순) / oldest(오래된순)")
            @RequestParam(defaultValue = "latest") String sort) {
        return moveInRecordService.getList(user.getUserId(), sort);
    }

    @Operation(summary = "입주 기록 삭제", description = "입주 기록, 하자 칩, 사진(DB + S3) 모두 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long id) {
        moveInRecordService.delete(user.getUserId(), id);
    }
}
