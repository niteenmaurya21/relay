package com.niteen.relay.entity;


import jakarta.persistence.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "steps")
public class Step {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String runId;

    @Column(nullable = false)
    private String nodeId;

    @Column(nullable = false)
    private String nodeType;

    @Column(nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StepStatus status;

    @Column(nullable = false)
    private int attempt;

    @Column(columnDefinition = "LONGTEXT")
    private String resolvedInput;

    @Column(columnDefinition = "LONGTEXT")
    private String output;

    private Long tokensPrompt;

    private Long tokensCompletion;

    private String idempotencyKey;

    private LocalDateTime startedAt;

    private Long durationMs;

    public Step() {}

    public Step(Long id, String runId, String nodeId, String nodeType, int sequenceNumber, StepStatus status, int attempt,String resolvedInput ,String output, Long tokensPrompt, Long tokensCompletion, String idempotencyKey, LocalDateTime startedAt, Long durationMs) {
        this.id = id;
        this.runId = runId;
        this.nodeId = nodeId;
        this.nodeType = nodeType;
        this.sequenceNumber = sequenceNumber;
        this.status = status;
        this.attempt = attempt;
        this.resolvedInput = resolvedInput;
        this.output = output;
        this.tokensPrompt = tokensPrompt;
        this.tokensCompletion = tokensCompletion;
        this.idempotencyKey = idempotencyKey;
        this.startedAt = startedAt;
        this.durationMs = durationMs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeType() {
        return nodeType;
    }

    public void setNodeType(String nodeType) {
        this.nodeType = nodeType;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public StepStatus getStatus() {
        return status;
    }

    public void setStatus(StepStatus status) {
        this.status = status;
    }

    public int getAttempt() {
        return attempt;
    }

    public void setAttempt(int attempt) {
        this.attempt = attempt;
    }

    public String getResolvedInput() {
        return resolvedInput;
    }

    public void setResolvedInput(String resolvedInput) {
        this.resolvedInput = resolvedInput;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Long getTokensPrompt() {
        return tokensPrompt;
    }

    public void setTokensPrompt(Long tokensPrompt) {
        this.tokensPrompt = tokensPrompt;
    }

    public Long getTokensCompletion() {
        return tokensCompletion;
    }

    public void setTokensCompletion(Long tokensCompletion) {
        this.tokensCompletion = tokensCompletion;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }
}
