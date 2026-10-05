package com.niteen.relay.controller;

import com.niteen.relay.entity.Approval;
import com.niteen.relay.entity.ApprovalStatus;
import com.niteen.relay.service.ApprovalService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/approvals")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    public List<Approval> getApprovals(
            @RequestParam ApprovalStatus status
    ) {
        return approvalService.getApprovals(status);
    }

    @PostMapping("/{approvalId}/approve")
    public Approval approve(
            @PathVariable Long approvalId,
            @RequestParam String decidedBy
    ) {
        return approvalService.approve(approvalId, decidedBy);
    }

    @PostMapping("/{approvalId}/reject")
    public Approval reject(
            @PathVariable Long approvalId,
            @RequestParam String decidedBy
    ) {
        return approvalService.reject(approvalId, decidedBy);
    }
}