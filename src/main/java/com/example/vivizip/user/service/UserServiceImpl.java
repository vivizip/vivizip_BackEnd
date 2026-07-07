package com.example.vivizip.user.service;

import com.example.vivizip.user.dto.UpdateLanguageRequest;
import com.example.vivizip.user.dto.UpdateProfileRequest;
import com.example.vivizip.user.dto.UserProfileResponse;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String email) {
        User user = findActiveUser(email);
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = findActiveUser(email);
        user.updateProfile(request.profileImage());
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateLanguage(String email, UpdateLanguageRequest request) {
        User user = findActiveUser(email);
        user.updateLanguage(request.language());
        return UserProfileResponse.from(user);
    }

    @Override
    @Transactional
    public void withdraw(String email) {
        User user = findActiveUser(email);
        user.withdraw();
    }

    private User findActiveUser(String email) {
        return userRepository.findByEmail(email)
                .filter(u -> u.getStatus().name().equals("ACTIVE"))
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));
    }
}