package com.example.my_project_1.auth.provider;

import com.example.my_project_1.auth.exception.UserSuspendedException;
import com.example.my_project_1.auth.exception.WithdrawalCompletedException;
import com.example.my_project_1.auth.exception.WithdrawalPendingException;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.UserStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    public CustomAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        setHideUserNotFoundExceptions(false);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        super.additionalAuthenticationChecks(userDetails, authentication);

        UserDetailsImpl details = (UserDetailsImpl) userDetails;

        validateWithdrawalStatus(details);
        validateAccountStatus(details);
    }

    private void validateAccountStatus(UserDetailsImpl details) {

        if (details.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new UserSuspendedException(
                    "차단된 계정입니다.",
                    details.getSuspendedUntil(),
                    details.getReason()
            );
        }
    }

    private void validateWithdrawalStatus(UserDetailsImpl details) {

        if (details.isDeleted()) {
            throw new WithdrawalCompletedException("탈퇴 처리 된 계정입니다.");
        }

        if (details.getUserStatus() == UserStatus.WITHDRAWN_REQUESTED) {

            if (!details.isCanRestore()) {
                throw new WithdrawalCompletedException("탈퇴 기한이 지나 복구할 수 없는 계정입니다.");
            }

            throw new WithdrawalPendingException(
                    "탈퇴 요청 상태입니다.",
                    details.getScheduledDeletionAt(),
                    details.getRemainingDays(),
                    true
            );
        }
    }
}
