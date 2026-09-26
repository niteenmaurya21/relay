package com.niteen.relay.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name="queue_jobs")
public class QueueJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String runId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private QueueJobStatus status;



    @Column(nullable = false)
    private LocalDateTime availableAt;

    private LocalDateTime leaseUntil;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public QueueJob() {}

    public Long getId() {
        return id;
    }

    public String getRunId() {
        return runId;
    }

    public QueueJobStatus getStatus() {
        return status;
    }

    public LocalDateTime getAvailableAt() {
        return availableAt;
    }

    public LocalDateTime getLeaseUntil() {
        return leaseUntil;
    }

    public int getAttempts() {
        return attempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public void setStatus(QueueJobStatus status) {
        this.status = status;
    }

    public void setAvailableAt(LocalDateTime availableAt) {
        this.availableAt = availableAt;
    }

    public void setLeaseUntil(LocalDateTime leaseUntil) {
        this.leaseUntil = leaseUntil;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
