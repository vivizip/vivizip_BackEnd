package com.example.vivizip.chat.controller;

import com.example.vivizip.chat.dto.ChatMessageSliceResponse;
import com.example.vivizip.chat.dto.ChatRoomCreateRequest;
import com.example.vivizip.chat.dto.ChatRoomResponse;
import com.example.vivizip.chat.service.ChatRoomService;
import com.example.vivizip.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 생성 or 조회
    @PostMapping("/rooms")
    public ChatRoomResponse createRoom(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody ChatRoomCreateRequest request) {
        return chatRoomService.createOrGetRoom(user.getUserId(), request.targetId());
    }

    // 내 채팅방 목록
    @GetMapping("/rooms")
    public List<ChatRoomResponse> getMyRooms(
            @AuthenticationPrincipal CustomUserDetails user) {
        return chatRoomService.getMyRooms(user.getUserId());
    }

    // 이전 대화 내역 (커서 페이징)
    @GetMapping("/rooms/{roomId}/messages")
    public ChatMessageSliceResponse getMessages(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {
        return chatRoomService.getMessages(user.getUserId(), roomId, cursor, size);
    }
}
