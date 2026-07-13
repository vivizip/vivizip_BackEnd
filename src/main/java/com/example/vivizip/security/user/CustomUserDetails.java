package com.example.vivizip.security.user;

import com.example.vivizip.user.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String role;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.email = user.getEmail();
        // 온보딩 전에는 role이 아직 정해지지 않아 null일 수 있음
        this.role = user.getRole() != null ? "ROLE_" + user.getRole().name() : null;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return role != null ? List.of(new SimpleGrantedAuthority(role)) : List.of();
    }

    @Override
    public String getUsername() {
        return email; // JWT subject와 일치
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }
}