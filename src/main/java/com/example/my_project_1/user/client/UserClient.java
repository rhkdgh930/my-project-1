package com.example.my_project_1.user.client;

import java.util.List;
import java.util.Map;

public interface UserClient {
    Map<Long, UserSummary> findUsersByIds(List<Long> ids);
}
