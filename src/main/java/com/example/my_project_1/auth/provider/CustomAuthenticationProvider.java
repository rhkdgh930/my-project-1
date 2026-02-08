package com.example.my_project_1.auth.provider;

import com.example.my_project_1.auth.exception.UserSuspendedException;
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
        log.info("[CustomAuthenticationProvider called]");
        super.additionalAuthenticationChecks(userDetails, authentication);

        UserDetailsImpl user = (UserDetailsImpl) userDetails;

        if (user.isDeleted()) {
            throw new WithdrawalPendingException("탈퇴 처리 된 계정입니다.");
        }

        if (user.getUserStatus() == UserStatus.WITHDRAWN_REQUESTED) {
            throw new WithdrawalPendingException("탈퇴 대기중인 계정입니다.");
        }

        if (user.getAccountStatus() == AccountStatus.SUSPENDED) {
            throw new UserSuspendedException(
                    "차단된 계정입니다.",
                    user.getSuspendedUntil(),
                    user.getReason()
            );
        }
    }
}
