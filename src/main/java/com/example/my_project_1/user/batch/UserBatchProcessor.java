package com.example.my_project_1.user.batch;

import com.example.my_project_1.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBatchProcessor {

    private final UserBatchWorker worker;

    public void processDormancyChunk(List<User> users, LocalDateTime threshold) {
        for (User user : users) {
            try {
                worker.processSingleUserWithDormancy(user, threshold);
            } catch (Exception e) {
                log.error("[BATCH][Dormancy][USER_FAIL] userId={}",
                        user.getId(), e);
            }
        }
    }

    public void processWithdrawalChunk(List<User> users) {
        for (User user : users) {
            try {
                worker.processSingleWithdrawal(user);
            } catch (Exception e) {
                log.error("[BATCH][Withdrawal][USER_FAIL] userId={}",
                        user.getId(), e);
            }
        }
    }
}