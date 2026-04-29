package com.example.my_project_1.outbox.controller;

import com.example.my_project_1.outbox.service.AdminOutboxService;
import com.example.my_project_1.outbox.service.OutboxProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@RequestMapping("/api/admin/outbox")
public class AdminOutboxController {

    private final AdminOutboxService adminOutboxService;
    private final OutboxProcessor outboxProcessor;

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long id) {
        adminOutboxService.retry(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/retry-now")
    public ResponseEntity<Void> retryNow(@PathVariable Long id) {
        adminOutboxService.retry(id);
        outboxProcessor.process(id);
        return ResponseEntity.accepted().build();
    }
}
