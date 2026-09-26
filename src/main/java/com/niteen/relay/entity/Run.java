package com.niteen.relay.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "runs")
public class Run {

    @Id
    private String runId;

    @Column(nullable = false)
    private String workflowId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status;

    private String triggerType;

    @Column(columnDefinition = "LONGTEXT")
    private String triggerInput;

    @Column(columnDefinition = "LONGTEXT")
    private String definitionSnapshot;

    private String currentNodeId;

    private int stepsExecuted;

    private Long aiTokensUsed;

    @Column(columnDefinition = "LONGTEXT")
    private String error;

    private LocalDateTime startedAt;

    private LocalDateTime finishedAt;

    public Run() {
    }

    public Run(String runId, String workflowId, RunStatus status, String triggerType,String triggerInput, String definitionSnapshot, String currentNodeId, int stepsExecuted, Long aiTokensUsed, String error, LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.runId = runId;
        this.workflowId = workflowId;
        this.status = status;
        this.triggerType = triggerType;
        this.triggerInput = triggerInput;
        this.definitionSnapshot = definitionSnapshot;
        this.currentNodeId = currentNodeId;
        this.stepsExecuted = stepsExecuted;
        this.aiTokensUsed = aiTokensUsed;
        this.error = error;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getDefinitionSnapshot() {
        return definitionSnapshot;
    }

    public void setDefinitionSnapshot(String definitionSnapshot) {
        this.definitionSnapshot = definitionSnapshot;
    }

    public String getCurrentNodeId() {
        return currentNodeId;
    }

    public void setCurrentNodeId(String currentNodeId) {
        this.currentNodeId = currentNodeId;
    }

    public int getStepsExecuted() {
        return stepsExecuted;
    }

    public void setStepsExecuted(int stepsExecuted) {
        this.stepsExecuted = stepsExecuted;
    }

    public Long getAiTokensUsed() {
        return aiTokensUsed;
    }

    public void setAiTokensUsed(Long aiTokensUsed) {
        this.aiTokensUsed = aiTokensUsed;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getTriggerInput() {
        return triggerInput;
    }

    public void setTriggerInput(String triggerInput) {
        this.triggerInput = triggerInput;
    }
}
