package com.niteen.relay.service;

import com.niteen.relay.entity.*;
import com.niteen.relay.repository.ApprovalRepository;
import com.niteen.relay.repository.RunRepository;
import com.niteen.relay.repository.StepRepository;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final StepRepository stepRepository;
    private final RunRepository runRepository;
    private final QueueJobService queueJobService;
    private final JsonMapper jsonMapper;

    public ApprovalService(
            ApprovalRepository approvalRepository,
            StepRepository stepRepository,
            RunRepository runRepository,
            QueueJobService queueJobService,
            JsonMapper jsonMapper
    ) {
        this.approvalRepository = approvalRepository;
        this.stepRepository = stepRepository;
        this.runRepository = runRepository;
        this.queueJobService = queueJobService;
        this.jsonMapper = jsonMapper;
    }

    public List<Approval> getApprovals(ApprovalStatus status) {
        return approvalRepository.findByStatus(status);
    }

    public Approval approve(Long approvalId, String decidedBy) {

        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() ->
                        new RuntimeException("Approval not found: " + approvalId)
                );

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Approval is already decided");
        }

        Run run = runRepository.findById(approval.getRunId())
                .orElseThrow(() ->
                        new RuntimeException("Run not found: " + approval.getRunId())
                );

        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setDecidedBy(decidedBy);
        approval.setDecidedAt(LocalDateTime.now());

        approvalRepository.save(approval);

        Step approvalStep = stepRepository
                .findByRunIdAndNodeIdAndStatus(
                        approval.getRunId(),
                        approval.getNodeId(),
                        StepStatus.WAITING_APPROVAL
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Approval step not found for run: "
                                        + approval.getRunId()
                        )
                );

        approvalStep.setStatus(StepStatus.SUCCEEDED);

        try {
            String stepOutput = jsonMapper.writeValueAsString(
                    Map.of(
                            "decision", "approved",
                            "decided_by", decidedBy
                    )
            );

            approvalStep.setOutput(stepOutput);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to create approval step output",
                    e
            );
        }

        // Approval node is now a completed step.
        run.setStepsExecuted(run.getStepsExecuted() + 1);

        // Resume workflow timeout after human approval.
        run.setStartedAt(LocalDateTime.now());
        run.setStepsExecuted(run.getStepsExecuted() + 1);

        try {
            JsonNode definition = jsonMapper.readTree(
                    run.getDefinitionSnapshot()
            );

            JsonNode nodes = definition.path("nodes");

            String nextNodeId = null;

            for (JsonNode node : nodes) {
                if (approval.getNodeId().equals(node.path("id").asText())) {
                    nextNodeId = node.path("next").isNull()
                            ? null
                            : node.path("next").asText();
                    break;
                }
            }

            if (nextNodeId == null || nextNodeId.isBlank()) {
                run.setStatus(RunStatus.SUCCEEDED);
                run.setFinishedAt(LocalDateTime.now());
            } else {
                run.setCurrentNodeId(nextNodeId);
                run.setStatus(RunStatus.QUEUED);
                queueJobService.enqueue(run.getRunId());
            }

            runRepository.save(run);

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to resume approved run: " + run.getRunId(),
                    e
            );
        }

        return approval;
    }

    public Approval reject(Long approvalId, String decidedBy) {

        Approval approval = approvalRepository.findById(approvalId)
                .orElseThrow(() ->
                        new RuntimeException("Approval not found: " + approvalId)
                );

        if (approval.getStatus() != ApprovalStatus.PENDING) {
            throw new RuntimeException("Approval is already decided");
        }

        Run run = runRepository.findById(approval.getRunId())
                .orElseThrow(() ->
                        new RuntimeException("Run not found: " + approval.getRunId())
                );

        approval.setStatus(ApprovalStatus.REJECTED);
        approval.setDecidedBy(decidedBy);
        approval.setDecidedAt(LocalDateTime.now());

        approvalRepository.save(approval);

        run.setStatus(RunStatus.CANCELLED);
        run.setFinishedAt(LocalDateTime.now());

        runRepository.save(run);

        return approval;
    }
}