package com.example.vivizip.matching.service;

import com.example.vivizip.chat.service.ChatRoomService;
import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.matching.dto.MatchResponse;
import com.example.vivizip.matching.dto.RematchRequest;
import com.example.vivizip.matching.dto.StudentOnboardingRequest;
import com.example.vivizip.matching.dto.SupporterOnboardingRequest;
import com.example.vivizip.matching.dto.TimeSlotRequest;
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
        if (!timeSlotRepository.existsByUserId(studentUserId)) {
            throw new GeneralException(ErrorStatus.MATCH_TIME_SLOT_NOT_FOUND);
        }

        List<User> candidates = findMatchableSupporters(student, Set.of());
        if (candidates.isEmpty()) {
            throw new GeneralException(ErrorStatus.MATCH_CANDIDATE_NOT_FOUND);
        }

        User supporter = pickBestSupporter(student, candidates);
        Match match = matchRepository.save(Match.createMatched(student.getId(), supporter.getId()));
        Long chatRoomId = chatRoomService.createForMatch(match.getId(), student.getId(), supporter.getId());
        notifySupporterMatched(match);
        return MatchResponse.of(match, student, supporter, student.getId(), chatRoomId);
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
        return MatchResponse.of(match, student, supporter, userId, chatRoomId);
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
        return MatchResponse.of(newMatch, student, supporter, userId, chatRoomId);
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