package com.example.vivizip.user.repository;

import com.example.vivizip.user.entity.Role;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.entity.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByKakaoId(String kakaoId);
    Optional<User> findByEmail(String email);

    List<User> findBySchoolIdAndRoleAndStatusAndSchoolVerified(
            Long schoolId, Role role, UserStatus status, Boolean schoolVerified);

    List<User> findByStatus(UserStatus status);
}