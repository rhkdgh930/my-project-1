package com.example.my_project_1.user.batch;

import com.example.my_project_1.common.logging.BatchTraceHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserBatchScheduler {

    private final DormantUserJob dormantUserJob;
    private final WithdrawalCleanupJob withdrawalCleanupJob;

    @Scheduled(cron = "0 29 1 * * *")
    public void runDormantBatch() {
        BatchTraceHelper.start();
        try {
            log.info("[BATCH][DormantUserScheduler][START]");

            dormantUserJob.run();

            log.info("[BATCH][DormantUserScheduler][FINISH]");

        } finally {
            BatchTraceHelper.clear();
        }
    }

    @Scheduled(cron = "0 30 1 * * *")
    public void runWithdrawalBatch() {
        BatchTraceHelper.start();
        try {
            log.info("[BATCH][WithdrawalCleanupScheduler][START]");

            withdrawalCleanupJob.run();

            log.info("[BATCH][WithdrawalCleanupScheduler][FINISH]");

        } finally {
            BatchTraceHelper.clear();
        }
    }

}