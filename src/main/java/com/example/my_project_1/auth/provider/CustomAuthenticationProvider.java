package com.example.my_project_1.auth.provider;

import com.example.my_project_1.auth.exception.UserSuspendedException;
import com.example.my_project_1.auth.exception.WithdrawalCompletedException;
import com.example.my_project_1.auth.exception.WithdrawalPendingException;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.domain.UserWithdrawal;
import com.example.my_project_1.user.service.UserQueryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    private final UserQueryService userQueryService;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, UserQueryService userQueryService) {
        super(userDetailsService);
        setPasswordEncoder(passwordEncoder);
        setHideUserNotFoundExceptions(false);
        this.userQueryService = userQueryService;
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        log.info("[CustomAuthenticationProvider called]");
        super.additionalAuthenticationChecks(userDetails, authentication);

        UserDetailsImpl details = (UserDetailsImpl) userDetails;

        User user = userQueryService.getByIdOrThrow(details.getUserId());

        if (user.isDeleted()) {
            throw new WithdrawalCompletedException("탈퇴 처리 된 계정입니다.");
        }

        if (user.getUserStatus() == UserStatus.WITHDRAWN_REQUESTED) {
            UserWithdrawal withdrawal = user.getWithdrawal();

            long remainingDays = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    withdrawal.scheduledDeletionAt().toLocalDate()
            );

            throw new WithdrawalPendingException(
                    "탈퇴 대기중인 계정입니다.",
                    withdrawal.scheduledDeletionAt(),
                    Math.max(remainingDays, 0),
                    withdrawal.canRestore(LocalDateTime.now()));
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new UserSuspendedException(
                    "차단된 계정입니다.",
                    user.getSuspension().getSuspendedUntil(),
                    user.getSuspension().getReason()
            );
        }
    }
}
