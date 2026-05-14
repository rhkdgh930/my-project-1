package com.example.my_project_1.auth.service;

import com.example.my_project_1.auth.exception.JwtAuthenticationException;
import com.example.my_project_1.auth.exception.UserSuspendedException;
import com.example.my_project_1.auth.exception.WithdrawalCompletedException;
import com.example.my_project_1.auth.exception.WithdrawalPendingException;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import com.example.my_project_1.common.exception.CustomException;
import com.example.my_project_1.common.exception.ErrorCode;
import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import com.example.my_project_1.user.domain.UserSuspension;
import com.example.my_project_1.user.domain.UserWithdrawal;
import org.springframework.security.authentication.DisabledException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UserAccountPolicy {

    public void validateLoginAllowed(User user, LocalDateTime now) {
        if (user.isDeleted() || user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new WithdrawalCompletedException("탈퇴 처리된 계정입니다.");
        }

        if (user.getUserStatus() == UserStatus.WITHDRAWN_REQUESTED) {
            UserWithdrawal withdrawal = user.getWithdrawal();

            if (withdrawal == null || !withdrawal.canRestore(now)) {
                throw new WithdrawalCompletedException("탈퇴 기한이 지나 복구할 수 없는 계정입니다.");
            }

            throw new WithdrawalPendingException(
                    "탈퇴 요청 상태입니다.",
                    withdrawal.getScheduledDeletionAt(),
                    withdrawal.getRemainingDays(now),
                    true
            );
        }

        if (user.getUserStatus() == UserStatus.DORMANT) {
            throw new DisabledException("휴면 상태의 계정입니다.");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            UserSuspension suspension = user.getSuspension();

            throw new UserSuspendedException(
                    "차단된 계정입니다.",
                    suspension != null ? suspension.getSuspendedUntil() : null,
                    suspension != null ? suspension.getReason() : null
            );
        }
    }

    public void validateApiAccessAllowed(
            UserStatus userStatus,
            AccountStatus accountStatus,
            boolean deleted
    ) {
        if (deleted || userStatus == UserStatus.WITHDRAWN) {
            throw new JwtAuthenticationException(ErrorCode.USER_NOT_FOUND);
        }

        if (userStatus == UserStatus.WITHDRAWN_REQUESTED) {
            throw new JwtAuthenticationException(ErrorCode.WITHDRAWAL_PENDING);
        }

        if (userStatus == UserStatus.DORMANT) {
            throw new JwtAuthenticationException(ErrorCode.USER_DORMANT);
        }

        if (accountStatus == AccountStatus.SUSPENDED) {
            throw new JwtAuthenticationException(ErrorCode.USER_SUSPENDED);
        }
    }

    public void validateLoginAllowed(UserDetailsImpl details) {
        if (details.isDeleted() || details.getUserStatus() == UserStatus.WITHDRAWN) {
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

        if (details.getUserStatus() == UserStatus.DORMANT) {
            throw new DisabledException("휴면 상태의 계정입니다.");
        }

        if (details.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new UserSuspendedException(
                    "차단된 계정입니다.",
                    details.getSuspendedUntil(),
                    details.getReason()
            );
        }
    }

    public void validateMeReadable(User user) {
        if (user.isDeleted() || user.getUserStatus() == UserStatus.WITHDRAWN) {
            throw new CustomException(ErrorCode.USER_NOT_FOUND);
        }

        if (user.getUserStatus() == UserStatus.WITHDRAWN_REQUESTED) {
            throw new CustomException(ErrorCode.WITHDRAWAL_PENDING);
        }

        if (user.getUserStatus() == UserStatus.DORMANT) {
            throw new CustomException(ErrorCode.USER_DORMANT);
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new CustomException(ErrorCode.USER_SUSPENDED);
        }
    }
}