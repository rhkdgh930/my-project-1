package com.example.my_project_1.auth.userdetails;

import com.example.my_project_1.user.domain.Email;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserSuspension;
import com.example.my_project_1.user.domain.UserWithdrawal;
import com.example.my_project_1.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final Clock clock;
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(Email.from(email))
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found")
                );

        UserSuspension suspension = user.getSuspension();
        UserWithdrawal withdrawal = user.getWithdrawal();

        LocalDateTime now = LocalDateTime.now(clock);

        LocalDateTime scheduledDeletionAt = null;
        Long remainingDays = null;
        boolean canRestore = false;

        if (withdrawal != null) {
            scheduledDeletionAt = withdrawal.getScheduledDeletionAt();
            remainingDays = withdrawal.getRemainingDays(now);
            canRestore = withdrawal.canRestore(now);
        }

        return new UserDetailsImpl(
                user.getId(),
                user.getEmail().getValue(),
                user.getPassword(),
                user.getRole().name(),
                user.getAccountStatus(),
                user.getUserStatus(),
                suspension != null ? suspension.getReason() : null,
                suspension != null ? suspension.getSuspendedUntil() : null,
                scheduledDeletionAt,
                remainingDays,
                canRestore,
                user.isDeleted()
        );
    }
}
