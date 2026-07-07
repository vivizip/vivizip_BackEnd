package com.example.vivizip.user.service;

import com.example.vivizip.common.exception.ErrorStatus;
import com.example.vivizip.common.exception.GeneralException;
import com.example.vivizip.user.dto.UpdateLanguageRequest;
import com.example.vivizip.user.dto.UpdateProfileRequest;
import com.example.vivizip.user.dto.UserProfileResponse;
import com.example.vivizip.user.entity.User;
import com.example.vivizip.user.entity.UserStatus;
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
        return UserProfileResponse.from(findActiveUser(email));
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
        findActiveUser(email).withdraw();
    }

    private User findActiveUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorStatus.MEMBER_NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new GeneralException(ErrorStatus._FORBIDDEN);
        }
        return user;
    }
}
