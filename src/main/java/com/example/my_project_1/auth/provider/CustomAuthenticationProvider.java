package com.example.my_project_1.auth.provider;

import com.example.my_project_1.auth.service.UserAccountPolicy;
import com.example.my_project_1.auth.userdetails.UserDetailsImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
public class CustomAuthenticationProvider extends DaoAuthenticationProvider {

    private final UserAccountPolicy userAccountPolicy;

    public CustomAuthenticationProvider(UserDetailsService userDetailsService, PasswordEncoder passwordEncoder, UserAccountPolicy userAccountPolicy) {
        super(userDetailsService);
        this.userAccountPolicy = userAccountPolicy;
        setPasswordEncoder(passwordEncoder);
        setHideUserNotFoundExceptions(false);
    }

    @Override
    protected void additionalAuthenticationChecks(UserDetails userDetails, UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        super.additionalAuthenticationChecks(userDetails, authentication);

        userAccountPolicy.validateLoginAllowed((UserDetailsImpl) userDetails);
    }

}
