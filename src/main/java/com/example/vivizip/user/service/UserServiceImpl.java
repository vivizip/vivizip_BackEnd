package com.example.vivizip.user.service;

import com.example.vivizip.S3.service.S3Service;
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
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final S3Service s3Service;

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
            throw new GeneralException(ErrorStatus.MEMBER_WITHDRAWN);
        }
        return user;
    }

    @Transactional
    public String updateProfileImage(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 갤러리에서 고른 이미지를 S3에 업로드 (public)
        String imageUrl = s3Service.uploadPublic(file, "profile");

        // User 엔티티의 profileImage 갱신 (카카오 이미지 → 새 이미지로 교체됨)
        user.updateProfile(imageUrl);

        return imageUrl;
    }
}
