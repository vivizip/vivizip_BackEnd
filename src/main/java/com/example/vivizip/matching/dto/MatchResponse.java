package com.example.vivizip.matching.dto;

import com.example.vivizip.matching.entity.Match;
import com.example.vivizip.matching.entity.MatchStatus;
import com.example.vivizip.user.entity.Gender;
import com.example.vivizip.user.entity.KoreanLevel;
import com.example.vivizip.user.entity.Nationality;
import com.example.vivizip.user.entity.User;

import java.util.List;

public record MatchResponse(
        Long matchId,
        Long chatRoomId,
        Long studentId,
        String studentName,
        String studentProfileImage,
        Long supporterId,
        String supporterName,
        String supporterProfileImage,
        MatchStatus status,
        Nationality counterpartNationality,
        Gender counterpartGender,
        KoreanLevel counterpartKoreanLevel,     // 상대가 서포터즈면 null (서포터즈 온보딩엔 한국어 수준이 없음)
        List<TimeSlotResponse> counterpartTimeSlots
) {
    public static MatchResponse of(Match match, User student, User supporter, Long viewerId, Long chatRoomId,
                                    List<TimeSlotResponse> counterpartTimeSlots) {
        User counterpart = viewerId.equals(student.getId()) ? supporter : student;
        return new MatchResponse(
                match.getId(),
                chatRoomId,
                student.getId(),
                student.getName(),
                student.getProfileImage(),
                supporter.getId(),
                supporter.getName(),
                supporter.getProfileImage(),
                match.getStatus(),
                counterpart.getNationality(),
                counterpart.getGender(),
                counterpart.getKoreanLevel(),
                counterpartTimeSlots
        );
    }
}