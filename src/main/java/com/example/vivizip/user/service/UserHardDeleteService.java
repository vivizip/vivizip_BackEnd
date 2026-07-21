package com.example.vivizip.user.service;

import com.example.vivizip.S3.event.S3DeleteEvent;
import com.example.vivizip.chat.entity.ChatRoom;
import com.example.vivizip.chat.repository.ChatMessageRepository;
import com.example.vivizip.chat.repository.ChatRoomRepository;
import com.example.vivizip.appointment.repository.AppointmentRepository;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.document.entity.LeaseCase;
import com.example.vivizip.document.entity.LeaseDocument;
import com.example.vivizip.document.repository.DocumentAnalysisRepository;
import com.example.vivizip.document.repository.LeaseCaseRepository;
import com.example.vivizip.document.repository.LeaseDocumentFileRepository;
import com.example.vivizip.document.repository.LeaseDocumentRepository;
import com.example.vivizip.document.repository.ReferenceBaselineRepository;
import com.example.vivizip.matching.repository.MatchRepository;
import com.example.vivizip.matching.repository.TimeSlotRepository;
import com.example.vivizip.movein.entity.MoveInPhoto;
import com.example.vivizip.movein.entity.MoveInRecord;
import com.example.vivizip.movein.repository.MoveInRecordRepository;
import com.example.vivizip.notification.repository.FcmTokenRepository;
import com.example.vivizip.notification.repository.NotificationRepository;
import com.example.vivizip.ocr.repository.OcrResultRepository;
import com.example.vivizip.security.jwt.service.TokenService;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// 회원 탈퇴(하드 삭제): users row 자체를 지우고, 그 유저가 소유/참여한 데이터를 함께 정리한다.
// FK 제약이 없는 프로젝트 관례상 참조 컬럼(Long)들을 직접 조회해서 지운다.
// 매칭/채팅방/채팅메시지/약속은 상대방과 공유하는 데이터라도 함께 완전히 삭제한다(정책 확정).
// S3 파일은 DB 커밋 후에만 지워야 하므로 S3DeleteEvent로 모아서 트랜잭션 커밋 후 일괄 삭제한다.
@Component
@RequiredArgsConstructor
class UserHardDeleteService {

    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AppointmentRepository appointmentRepository;
    private final MatchRepository matchRepository;
    private final LeaseCaseRepository leaseCaseRepository;
    private final LeaseDocumentRepository leaseDocumentRepository;
    private final LeaseDocumentFileRepository leaseDocumentFileRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final ReferenceBaselineRepository referenceBaselineRepository;
    private final MoveInRecordRepository moveInRecordRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final OcrResultRepository ocrResultRepository;
    private final TokenService tokenService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void execute(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));

        List<String> s3KeysToDelete = new ArrayList<>();

        deleteChatAndMatchData(userId);
        deleteLeaseCaseData(userId, s3KeysToDelete);
        deleteMoveInRecords(userId, s3KeysToDelete);

        timeSlotRepository.deleteByUserId(userId);
        notificationRepository.deleteByUserId(userId);
        fcmTokenRepository.deleteByUserId(userId);
        ocrResultRepository.deleteByUserId(userId);

        userRepository.delete(user);
        tokenService.revokeAllTokens(user.getEmail());

        if (!s3KeysToDelete.isEmpty()) {
            eventPublisher.publishEvent(new S3DeleteEvent(s3KeysToDelete));
        }
    }

    // 매칭/채팅방(+메시지, 약속)은 상대방과 공유하는 데이터라도 함께 완전히 삭제한다.
    private void deleteChatAndMatchData(Long userId) {
        List<ChatRoom> chatRooms = chatRoomRepository.findAllBySupporterIdOrStudentId(userId, userId);
        if (!chatRooms.isEmpty()) {
            List<Long> chatRoomIds = chatRooms.stream().map(ChatRoom::getId).toList();
            chatMessageRepository.deleteByRoomIdIn(chatRoomIds);
            appointmentRepository.deleteByChatRoomIdIn(chatRoomIds);
            chatRoomRepository.deleteAll(chatRooms);
        }
        matchRepository.deleteByStudentIdOrSupporterId(userId, userId);
    }

    private void deleteLeaseCaseData(Long userId, List<String> s3KeysToDelete) {
        List<LeaseCase> leaseCases = leaseCaseRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        for (LeaseCase leaseCase : leaseCases) {
            referenceBaselineRepository.findByLeaseCaseId(leaseCase.getId())
                    .ifPresent(referenceBaselineRepository::delete);

            List<LeaseDocument> documents = leaseDocumentRepository.findAllByLeaseCaseId(leaseCase.getId());
            for (LeaseDocument document : documents) {
                var files = leaseDocumentFileRepository.findAllByLeaseDocumentId(document.getId());
                files.forEach(file -> s3KeysToDelete.add(file.getS3Key()));
                leaseDocumentFileRepository.deleteAll(files);
                documentAnalysisRepository.deleteByDocumentId(document.getId());
            }
            leaseDocumentRepository.deleteAll(documents);
        }
        leaseCaseRepository.deleteAll(leaseCases);
    }

    private void deleteMoveInRecords(Long userId, List<String> s3KeysToDelete) {
        List<MoveInRecord> records = moveInRecordRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (MoveInRecord record : records) {
            for (MoveInPhoto photo : record.getPhotos()) {
                s3KeysToDelete.add(photo.getS3Key());
            }
        }
        // cascade(ALL)+orphanRemoval로 MoveInDefect/MoveInPhoto도 함께 삭제된다.
        moveInRecordRepository.deleteAll(records);
    }
}