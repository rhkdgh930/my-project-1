package com.example.my_project_1.auth.userdetails;

import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailAndDeletedAtIsNull(Email.from(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException("Invalid credentials")
                );

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail().getValue(),
                user.getPassword(),
                user.getRole().name(),
                user.getAccountStatus(),
                user.getUserStatus(),
                user.isDeleted()
        );
    }
}
