package com.example.my_project_1.auth.userdetails;


import com.example.my_project_1.auth.cache.CachedUserContext;
import com.example.my_project_1.user.domain.*;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class UserDetailsImpl implements UserDetails, OAuth2User {
    private final Long userId;
    private final String email;
    private final String password;
    private final String role;
    private final AccountStatus accountStatus;
    private final UserStatus userStatus;

    private final SuspensionReason reason;
    private final LocalDateTime suspendedUntil;

    private final LocalDateTime scheduledDeletionAt;
    private final Long remainingDays;
    private final boolean canRestore;

    private final boolean deleted;

    private final Map<String, Object> attributes;

    public static UserDetailsImpl from(CachedUserContext ctx) {
        return new UserDetailsImpl(
                ctx.getUserId(),
                ctx.getEmail(),
                null,
                ctx.getRole().name(),
                ctx.getAccountStatus(),
                ctx.getUserStatus(),
                ctx.getReason(),
                ctx.getSuspendedUntil(),
                ctx.getScheduledDeletionAt(),
                ctx.getRemainingDays(),
                ctx.isCanRestore(),
                ctx.isDeleted(),
                Map.of()
        );
    }

    public static UserDetailsImpl from(User user, LocalDateTime now) {
        return from(user, now, Map.of());
    }

    public static UserDetailsImpl from(
            User user,
            LocalDateTime now,
            Map<String, Object> attributes
    ) {
        UserWithdrawal withdrawal = user.getWithdrawal();

        LocalDateTime scheduledDeletionAt = null;
        Long remainingDays = null;
        boolean canRestore = false;

        if (withdrawal != null) {
            scheduledDeletionAt = withdrawal.getScheduledDeletionAt();
            remainingDays = withdrawal.getRemainingDays(now);
            canRestore = withdrawal.canRestore(now);
        }

        UserSuspension suspension = user.getSuspension();

        SuspensionReason suspensionReason = null;
        LocalDateTime suspendedUntil = null;

        if (suspension != null) {
            suspensionReason = suspension.getReason();
            suspendedUntil = suspension.getSuspendedUntil();
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail().getValue(),
                user.getPassword(),
                user.getRole().name(),
                user.getAccountStatus(),
                user.getUserStatus(),
                suspensionReason,
                suspendedUntil,
                scheduledDeletionAt,
                remainingDays,
                canRestore,
                user.isDeleted(),
                attributes
        );
    }

    public UserDetailsImpl(Long userId, String email, String password, String role, AccountStatus accountStatus, UserStatus userStatus,
                           SuspensionReason reason, LocalDateTime suspendedUntil,
                           LocalDateTime scheduledDeletionAt, Long remainingDays, boolean canRestore, boolean deleted, Map<String, Object> attributes) {
        this.userId = userId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.accountStatus = accountStatus;
        this.userStatus = userStatus;
        this.reason = reason;
        this.suspendedUntil = suspendedUntil;
        this.scheduledDeletionAt = scheduledDeletionAt;
        this.remainingDays = remainingDays;
        this.canRestore = canRestore;
        this.deleted = deleted;
        this.attributes = (attributes != null) ? attributes : Map.of();
    }

    // --- UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if ("ADMIN".equals(role)) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_ADMIN"),
                    new SimpleGrantedAuthority("ROLE_USER")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    // --- OAuth2User ---

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return email;
    }
}
