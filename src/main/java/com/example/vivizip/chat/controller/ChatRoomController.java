package com.example.vivizip.chat.controller;

import com.example.vivizip.chat.dto.ChatMessageResponse;
import com.example.vivizip.chat.dto.ChatMessageSliceResponse;
import com.example.vivizip.chat.dto.ChatRoomCreateRequest;
import com.example.vivizip.chat.dto.ChatRoomResponse;
import com.example.vivizip.chat.service.ChatRoomService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "채팅방 생성 또는 조회", description = "대상 유저(targetId)와의 채팅방을 생성하거나, 이미 존재하면 기존 채팅방을 반환합니다. SUPPORTER ↔ STUDENT 간에만 가능합니다.")
    @PostMapping("/rooms")
    public ChatRoomResponse createRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChatRoomCreateRequest request) {
        return chatRoomService.createOrGetRoom(user.getUserId(), request.targetId());
    }

    @Operation(summary = "내 채팅방 목록 조회", description = "로그인한 유저가 참여 중인 채팅방 목록을 반환합니다.")
    @GetMapping("/rooms")
    public List<ChatRoomResponse> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails user) {
        return chatRoomService.getMyRooms(user.getUserId());
    }

    @Operation(summary = "채팅 메시지 이전 대화 조회", description = "커서 기반 페이징으로 채팅방의 이전 메시지를 조회합니다. cursor가 없으면 최신 메시지부터 반환합니다.")
    @GetMapping("/rooms/{roomId}/messages")
    public ChatMessageSliceResponse getMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @Parameter(description = "마지막으로 받은 메시지 ID (첫 요청 시 생략)") @RequestParam(required = false) Long cursor,
            @Parameter(description = "한 번에 조회할 메시지 수 (기본값: 20)") @RequestParam(defaultValue = "20") int size) {
        return chatRoomService.getMessages(user.getUserId(), roomId, cursor, size);
    }

    @Operation(
            summary = "채팅 이미지 전송",
            description = "이미지를 S3에 업로드하고, 해당 URL을 채팅 메시지(type=IMAGE)로 저장 후 채팅방 구독자에게 실시간 브로드캐스트합니다.\n\n" +
                    "- 허용 형식: JPEG, PNG, WEBP\n" +
                    "- 최대 크기: 10MB\n" +
                    "- form-data key: `file`\n" +
                    "- 응답의 `content` 필드가 S3 이미지 URL입니다.",
            requestBody = @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    )
    @PostMapping(value = "/rooms/{roomId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatMessageResponse uploadImage(
            @AuthenticationPrincipal CustomUserDetails user,
            @Parameter(description = "채팅방 ID") @PathVariable Long roomId,
            @RequestPart("file") MultipartFile file) {
        return chatRoomService.uploadImage(user.getUserId(), roomId, file);
    }
}
