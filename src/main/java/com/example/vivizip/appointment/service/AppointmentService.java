package com.example.vivizip.appointment.service;

import com.example.vivizip.appointment.dto.AppointmentCreateRequest;
import com.example.vivizip.appointment.dto.AppointmentResponse;
import com.example.vivizip.appointment.entity.Appointment;
import com.example.vivizip.appointment.repository.AppointmentRepository;
import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.repository.ChatRoomRepository;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final ChatRoomRepository chatRoomRepository;

    // 약속 생성 (채팅방 안에서, 생성 즉시 확정)
    @Transactional
    public AppointmentResponse create(Long userId, Long chatRoomId, AppointmentCreateRequest request) {
        validateRoomParticipant(userId, chatRoomId);

        Appointment appointment = appointmentRepository.save(
                Appointment.of(chatRoomId, userId, request.scheduledAt(),
                        request.placeName(), request.placeAddress(),
                        request.latitude(), request.longitude())
        );

        return AppointmentResponse.from(appointment);
    }

    // 채팅방의 약속 목록
    @Transactional(readOnly = true)
    public List<AppointmentResponse> getRoomAppointments(Long userId, Long chatRoomId) {
        validateRoomParticipant(userId, chatRoomId);

        return appointmentRepository.findByChatRoomIdOrderByScheduledAtDesc(chatRoomId).stream()
                .map(AppointmentResponse::from)
                .toList();
    }

    // 단건 조회
    @Transactional(readOnly = true)
    public AppointmentResponse get(Long userId, Long appointmentId) {
        Appointment appointment = findAndValidate(userId, appointmentId);
        return AppointmentResponse.from(appointment);
    }

    // 수정은 해커톤 기간에 안 만들 예정

    // 취소 (soft delete — 기록 보존)
    @Transactional
    public void cancel(Long userId, Long appointmentId) {
        Appointment appointment = findAndValidate(userId, appointmentId);
        appointment.cancel();
    }

    // ── 검증 ──

    // 채팅방 존재 + 참여자 여부
    private void validateRoomParticipant(Long userId, Long chatRoomId) {
        ChatRoom room = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.CHAT_ROOM_NOT_FOUND));

        if (!room.hasParticipant(userId)) {
            throw new GeneralException(ErrorStatus.CHAT_ACCESS_DENIED);
        }
    }

    // 약속 존재 + 해당 채팅방 참여자 여부
    private Appointment findAndValidate(Long userId, Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.APPOINTMENT_NOT_FOUND));

        validateRoomParticipant(userId, appointment.getChatRoomId());
        return appointment;
    }
}
