package com.example.vivizip.chat.repository;

import com.example.vivizip.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    // 페이징 조회는 다음 단계(REST)에서 추가
}