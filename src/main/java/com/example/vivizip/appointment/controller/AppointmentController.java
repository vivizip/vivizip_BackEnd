package com.example.vivizip.appointment.controller;

import com.example.vivizip.appointment.dto.AppointmentCreateRequest;
import com.example.vivizip.appointment.dto.AppointmentResponse;
import com.example.vivizip.appointment.service.AppointmentService;
import com.example.vivizip.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "약속", description = "채팅방 내 약속 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    @Operation(summary = "약속 생성",
            description = "채팅방 안에서 약속을 생성합니다. 생성 즉시 확정(SCHEDULED)되며 수락/거절 절차는 없습니다.")
    @PostMapping("/chat/rooms/{roomId}/appointments")
    public AppointmentResponse create(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId,
            @Valid @RequestBody AppointmentCreateRequest request) {
        return appointmentService.create(user.getUserId(), roomId, request);
    }

    @Operation(summary = "채팅방 약속 목록 조회", description = "해당 채팅방의 약속을 최신 일정순으로 조회합니다.")
    @GetMapping("/chat/rooms/{roomId}/appointments")
    public List<AppointmentResponse> getRoomAppointments(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long roomId) {
        return appointmentService.getRoomAppointments(user.getUserId(), roomId);
    }

    @Operation(summary = "약속 단건 조회")
    @GetMapping("/appointments/{appointmentId}")
    public AppointmentResponse get(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long appointmentId) {
        return appointmentService.get(user.getUserId(), appointmentId);
    }


    @Operation(summary = "약속 취소", description = "약속 상태를 CANCELED로 변경합니다. (기록은 보존)")
    @DeleteMapping("/appointments/{appointmentId}")
    public void cancel(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long appointmentId) {
        appointmentService.cancel(user.getUserId(), appointmentId);
    }
}
