package com.example.my_project_1.auth.cache;

import com.example.my_project_1.user.domain.AccountStatus;
import com.example.my_project_1.user.domain.Role;
import com.example.my_project_1.user.domain.User;
import com.example.my_project_1.user.domain.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CachedUserContext {
    private Long userId;
    private String email;
    private Role role;
    private UserStatus userStatus;
    private AccountStatus accountStatus;
    private boolean deleted;

    public static CachedUserContext from(User user) {
        return new CachedUserContext(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getUserStatus(),
                user.getAccountStatus(),
                user.isDeleted()
        );
    }
}
