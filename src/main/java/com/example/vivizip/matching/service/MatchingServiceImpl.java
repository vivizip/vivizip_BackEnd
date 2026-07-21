package com.example.vivizip.matching.service;

import com.example.vivizip.chat.service.ChatRoomService;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.matching.dto.MatchApplicationStatus;
import com.example.vivizip.matching.dto.MatchResponse;
import com.example.vivizip.matching.dto.MatchStatusResponse;
import com.example.vivizip.matching.dto.RematchRequest;
import com.example.vivizip.matching.dto.StudentOnboardingRequest;
import com.example.vivizip.matching.dto.SupporterOnboardingRequest;
import com.example.vivizip.matching.dto.TimeSlotRequest;
import com.example.vivizip.matching.dto.TimeSlotResponse;
import com.example.vivizip.matching.dto.UpdateTimeSlotsRequest;
import com.example.vivizip.matching.entity.Match;
import com.example.vivizip.matching.entity.MatchStatus;
import com.example.vivizip.matching.entity.TimeSlot;
import com.example.vivizip.matching.repository.MatchRepository;
import com.example.vivizip.matching.repository.TimeSlotRepository;
import com.example.vivizip.notification.service.NotificationService;
import com.example.vivizip.user.entity.Gender;
import com.example.vivizip.user.entity.Role;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.entity.UserStatus;
import com.example.vivizip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchingServiceImpl implements MatchingService {

    private static final int MAX_REMATCH_COUNT = 3;

    private final UserRepository userRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final MatchRepository matchRepository;
    private final ChatRoomService chatRoomService;
    private final NotificationService notificationService;
    private final MatchPendingRecorder matchPendingRecorder;

    @Override
    @Transactional
    public void onboardSupporter(Long userId, SupporterOnboardingRequest request) {
        User user = getUser(userId);
        validateSchoolVerified(user);

        user.updateRole(Role.SUPPORTER);
        user.updateSupporterProfile(request.nationality(), request.gender());
        replaceTimeSlots(userId, request.timeSlots());
    }

    @Override
    @Transactional
    public void onboardStudent(Long userId, StudentOnboardingRequest request) {
        User user = getUser(userId);
        validateSchoolVerified(user);

        user.updateRole(Role.STUDENT);
        user.updateStudentProfile(
                request.nationality(),
                request.gender(),
                request.koreanLevel(),
                request.depositBudget(),
                request.monthlyRentBudget());

        replaceTimeSlots(userId, request.timeSlots());
    }

    @Override
    @Transactional
    public MatchResponse applyMatch(Long studentUserId) {
        User student = getUser(studentUserId);
        if (student.getRole() != Role.STUDENT) {
            throw new GeneralException(ErrorStatus.MATCH_ROLE_NOT_STUDENT);
        }
        validateActive(student);
        validateSchoolVerified(student);
        if (student.getSchoolId() == null) {
            throw new GeneralException(ErrorStatus.MATCH_SCHOOL_ID_REQUIRED);
        }
        if (matchRepository.findByStudentIdAndStatus(studentUserId, MatchStatus.MATCHED).isPresent()) {
            throw new GeneralException(ErrorStatus.MATCH_ALREADY_MATCHED);
        }
        if (matchRepository.findByStudentIdAndStatus(studentUserId, MatchStatus.PENDING).isPresent()) {
            throw new GeneralException(ErrorStatus.MATCH_ALREADY_PENDING);
        }
        if (!timeSlotRepository.existsByUserId(studentUserId)) {
            throw new GeneralException(ErrorStatus.MATCH_TIME_SLOT_NOT_FOUND);
        }

        List<User> candidates = findMatchableSupporters(student, Set.of());
        if (candidates.isEmpty()) {
            // 후보를 못 찾았어도 신청 자체는 PENDING으로 남겨, 상태 조회 시 "대기 중"으로 보이게 한다.
            matchPendingRecorder.savePending(studentUserId);
            throw new GeneralException(ErrorStatus.MATCH_CANDIDATE_NOT_FOUND);
        }

        User supporter = pickBestSupporter(student, candidates);
        Match match = matchRepository.save(Match.createMatched(student.getId(), supporter.getId()));
        Long chatRoomId = chatRoomService.createForMatch(match.getId(), student.getId(), supporter.getId());
        notifySupporterMatched(match);
        return MatchResponse.of(match, student, supporter, student.getId(), chatRoomId,
                counterpartTimeSlots(student.getId(), supporter.getId(), student.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResponse getMatchResult(Long userId) {
        User user = getUser(userId);
        Match match = (user.getRole() == Role.STUDENT
                ? matchRepository.findByStudentIdAndStatus(userId, MatchStatus.MATCHED)
                : matchRepository.findBySupporterIdAndStatus(userId, MatchStatus.MATCHED))
                .orElseThrow(() -> new GeneralException(ErrorStatus.MATCH_RESULT_NOT_FOUND));

        User student = getUser(match.getStudentId());
        User supporter = getUser(match.getSupporterId());
        Long chatRoomId = chatRoomService.findRoomIdByMatchId(match.getId());
        return MatchResponse.of(match, student, supporter, userId, chatRoomId,
                counterpartTimeSlots(student.getId(), supporter.getId(), userId));
    }

    // Match 레코드의 실제 status를 기준으로 판단한다 (레코드 없음/CANCELED만 있음 -> NOT_APPLIED,
    // PENDING -> APPLIED_NOT_MATCHED(대기 중), MATCHED -> MATCHED). "MATCHED가 없다"는 이유만으로
    // 신청 전과 취소된 상태를 같은 값으로 뭉뚱그리지 않는다.
    @Override
    @Transactional(readOnly = true)
    public MatchStatusResponse getMatchStatus(Long userId) {
        User user = getUser(userId);
        if (user.getRole() == null) {
            return MatchStatusResponse.of(MatchApplicationStatus.NOT_APPLIED);
        }

        if (user.getRole() == Role.STUDENT) {
            if (matchRepository.findByStudentIdAndStatus(userId, MatchStatus.MATCHED).isPresent()) {
                return MatchStatusResponse.of(MatchApplicationStatus.MATCHED);
            }
            if (matchRepository.findByStudentIdAndStatus(userId, MatchStatus.PENDING).isPresent()) {
                return MatchStatusResponse.of(MatchApplicationStatus.APPLIED_NOT_MATCHED);
            }
            // 신청한 적이 없거나, 있던 매칭이 취소되고 재신청하지 않은 상태 -> 신청 전과 동일하게 취급
            return MatchStatusResponse.of(MatchApplicationStatus.NOT_APPLIED);
        }

        // 서포터즈는 applyMatch를 직접 호출하지 않아 PENDING 레코드가 생기지 않는다.
        // 온보딩(role=SUPPORTER) 자체가 매칭 후보 풀 등록이므로 MATCHED 여부만으로 판단한다.
        boolean matched = matchRepository.findBySupporterIdAndStatus(userId, MatchStatus.MATCHED).isPresent();
        return MatchStatusResponse.of(matched ? MatchApplicationStatus.MATCHED : MatchApplicationStatus.APPLIED_NOT_MATCHED);
    }

    @Override
    @Transactional
    public MatchResponse rematch(Long userId, Long matchId, RematchRequest request) {
        User requester = getUser(userId);
        if (requester.getRematchCount() >= MAX_REMATCH_COUNT) {
            throw new GeneralException(ErrorStatus.MATCH_REMATCH_LIMIT_EXCEEDED);
        }

        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MATCH_NOT_FOUND));

        boolean requesterIsStudent = match.getStudentId().equals(userId);
        boolean requesterIsSupporter = match.getSupporterId().equals(userId);
        if (!requesterIsStudent && !requesterIsSupporter) {
            throw new GeneralException(ErrorStatus.MATCH_FORBIDDEN);
        }
        if (match.getStatus() != MatchStatus.MATCHED) {
            throw new GeneralException(ErrorStatus.MATCH_STATUS_NOT_MATCHED);
        }

        chatRoomService.closeByMatchId(matchId);

        match.cancel(userId, request != null ? request.reason() : null);
        requester.increaseRematchCount();

        User student;
        User supporter;
        if (requesterIsStudent) {
            student = requester;
            List<User> candidates = findMatchableSupporters(student, Set.of(match.getSupporterId()));
            if (candidates.isEmpty()) {
                throw new GeneralException(ErrorStatus.MATCH_CANDIDATE_NOT_FOUND);
            }
            supporter = pickBestSupporter(student, candidates);
        } else {
            supporter = requester;
            List<User> candidates = findMatchableStudents(supporter, Set.of(match.getStudentId()));
            if (candidates.isEmpty()) {
                throw new GeneralException(ErrorStatus.MATCH_CANDIDATE_NOT_FOUND);
            }
            student = pickBestStudent(candidates, supporter);
        }

        Match newMatch = matchRepository.save(Match.createMatched(student.getId(), supporter.getId()));
        Long chatRoomId = chatRoomService.createForMatch(newMatch.getId(), student.getId(), supporter.getId());
        notifySupporterMatched(newMatch);
        return MatchResponse.of(newMatch, student, supporter, userId, chatRoomId,
                counterpartTimeSlots(student.getId(), supporter.getId(), userId));
    }

    private List<TimeSlotResponse> counterpartTimeSlots(Long studentId, Long supporterId, Long viewerId) {
        Long counterpartId = viewerId.equals(studentId) ? supporterId : studentId;
        return timeSlotRepository.findByUserId(counterpartId).stream()
                .map(TimeSlotResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeSlotResponse> getTimeSlots(Long userId) {
        return timeSlotRepository.findByUserId(userId).stream()
                .map(TimeSlotResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public void updateTimeSlots(Long userId, UpdateTimeSlotsRequest request) {
        replaceTimeSlots(userId, request.timeSlots());
    }

    // 신청자뿐 아니라 매칭된 상대(서포터즈/유학생) 모두에게 매칭 완료를 알린다.
    private void notifySupporterMatched(Match match) {
        notificationService.notifySupporterMatched(match.getStudentId(), match.getId());
        notificationService.notifySupporterMatched(match.getSupporterId(), match.getId());
    }

    private void replaceTimeSlots(Long userId, List<TimeSlotRequest> requestedSlots) {
        timeSlotRepository.deleteByUserId(userId);

        Map<String, TimeSlotRequest> dedupedSlots = new LinkedHashMap<>();
        for (TimeSlotRequest slot : requestedSlots) {
            dedupedSlots.putIfAbsent(slot.day() + "_" + slot.period(), slot);
        }

        List<TimeSlot> slots = dedupedSlots.values().stream()
                .map(slot -> TimeSlot.create(userId, slot.day(), slot.period()))
                .toList();

        timeSlotRepository.saveAll(slots);
    }

    private List<User> findMatchableSupporters(User student, Set<Long> excludeIds) {
        List<User> candidates = userRepository.findBySchoolIdAndRoleAndStatusAndSchoolVerified(
                student.getSchoolId(), Role.SUPPORTER, UserStatus.ACTIVE, true);

        Set<Long> matchedSupporterIds = matchRepository.findByStatus(MatchStatus.MATCHED).stream()
                .map(Match::getSupporterId)
                .collect(Collectors.toSet());

        List<User> filtered = candidates.stream()
                .filter(candidate -> !excludeIds.contains(candidate.getId()))
                .filter(candidate -> !matchedSupporterIds.contains(candidate.getId()))
                .toList();

        return filterByTimeSlotOverlap(filtered, slotKeys(student.getId()));
    }

    private List<User> findMatchableStudents(User supporter, Set<Long> excludeIds) {
        List<User> candidates = userRepository.findBySchoolIdAndRoleAndStatusAndSchoolVerified(
                supporter.getSchoolId(), Role.STUDENT, UserStatus.ACTIVE, true);

        Set<Long> matchedStudentIds = matchRepository.findByStatus(MatchStatus.MATCHED).stream()
                .map(Match::getStudentId)
                .collect(Collectors.toSet());

        List<User> filtered = candidates.stream()
                .filter(candidate -> !excludeIds.contains(candidate.getId()))
                .filter(candidate -> !matchedStudentIds.contains(candidate.getId()))
                .toList();

        return filterByTimeSlotOverlap(filtered, slotKeys(supporter.getId()));
    }

    private List<User> filterByTimeSlotOverlap(List<User> candidates, Set<String> requesterSlotKeys) {
        if (candidates.isEmpty()) {
            return candidates;
        }

        List<Long> candidateIds = candidates.stream().map(User::getId).toList();
        var candidateSlotKeys = timeSlotRepository.findByUserIdIn(candidateIds).stream()
                .collect(Collectors.groupingBy(TimeSlot::getUserId,
                        Collectors.mapping(this::slotKey, Collectors.toSet())));

        return candidates.stream()
                .filter(candidate -> candidateSlotKeys
                        .getOrDefault(candidate.getId(), Set.of())
                        .stream()
                        .anyMatch(requesterSlotKeys::contains))
                .toList();
    }

    private Set<String> slotKeys(Long userId) {
        return timeSlotRepository.findByUserId(userId).stream()
                .map(this::slotKey)
                .collect(Collectors.toSet());
    }

    private String slotKey(TimeSlot slot) {
        return slot.getDay() + "_" + slot.getPeriod();
    }

    private int calculateScore(User student, User supporter) {
        int score = 0;
        if (student.getGender() != Gender.NOT_SPECIFIED
                && student.getGender() == supporter.getGender()) {
            score++;
        }
        if (student.getNationality() != null && student.getNationality() == supporter.getNationality()) {
            score++;
        }
        return score;
    }

    private User pickBestSupporter(User student, List<User> supporterCandidates) {
        return pickHighestScored(supporterCandidates, candidate -> calculateScore(student, candidate));
    }

    private User pickBestStudent(List<User> studentCandidates, User supporter) {
        return pickHighestScored(studentCandidates, candidate -> calculateScore(candidate, supporter));
    }

    private User pickHighestScored(List<User> candidates, ToIntFunction<User> scorer) {
        int maxScore = candidates.stream().mapToInt(scorer).max().orElse(0);
        List<User> bestCandidates = candidates.stream()
                .filter(candidate -> scorer.applyAsInt(candidate) == maxScore)
                .toList();
        return bestCandidates.get(ThreadLocalRandom.current().nextInt(bestCandidates.size()));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
    }

    private void validateActive(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new GeneralException(ErrorStatus.MATCH_USER_STATUS_NOT_ACTIVE);
        }
    }

    private void validateSchoolVerified(User user) {
        if (!Boolean.TRUE.equals(user.getSchoolVerified())) {
            throw new GeneralException(ErrorStatus.MATCH_SCHOOL_NOT_VERIFIED);
        }
    }
}