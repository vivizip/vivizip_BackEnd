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

    // @Transactional 제거 — S3 업로드 동안 DB 커넥션 점유 방지
    public String updateProfileImage(Long userId, MultipartFile file) {
        // 1. 유저 존재 확인 — findById 자체 트랜잭션(readOnly)으로 처리 후 종료
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GeneralException(ErrorStatus.USER_NOT_FOUND));

        // 2. S3 업로드 — 트랜잭션 밖에서 수행
        String imageUrl = s3Service.uploadPublic(file, "profile");

        // 3. DB 갱신 — save()의 자체 @Transactional로 짧은 트랜잭션
        user.updateProfile(imageUrl);
        userRepository.save(user);

        return imageUrl;
    }
}
