package com.example.my_project_1.auth.userdetails;

import com.example.my_project_1.user.domain.*;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final Clock clock;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(Email.from(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        LocalDateTime now = LocalDateTime.now(clock);
        user.checkAndReleaseSuspension(now);

        return UserDetailsImpl.from(user, now);
    }
}
