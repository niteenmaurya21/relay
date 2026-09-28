package com.niteen.relay.service;


import com.niteen.relay.entity.QueueJob;
import com.niteen.relay.entity.QueueJobStatus;
import com.niteen.relay.entity.Run;
import com.niteen.relay.entity.RunStatus;
import com.niteen.relay.repository.QueueJobRepository;
import com.niteen.relay.repository.RunRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class QueueJobService {

    private final QueueJobRepository queueJobRepository;
    private final RunRepository runRepository;

    public QueueJobService(QueueJobRepository queueJobRepository,
                           RunRepository runRepository) {
        this.queueJobRepository = queueJobRepository;
        this.runRepository = runRepository;
    }

    public QueueJob enqueue(String runId) {

        LocalDateTime now = LocalDateTime.now();

        QueueJob queueJob = new QueueJob();

        queueJob.setRunId(runId);
        queueJob.setStatus(QueueJobStatus.QUEUED);
        queueJob.setAvailableAt(now);
        queueJob.setAttempts(0);
        queueJob.setCreatedAt(now);

        return queueJobRepository.save(queueJob);

    }
    public QueueJob enqueueAfter(String runId, long delaySeconds) {

        LocalDateTime now = LocalDateTime.now();

        QueueJob queueJob = new QueueJob();

        queueJob.setRunId(runId);
        queueJob.setStatus(QueueJobStatus.QUEUED);
        queueJob.setAvailableAt(now.plusSeconds(delaySeconds));
        queueJob.setAttempts(0);
        queueJob.setCreatedAt(now);

        return queueJobRepository.save(queueJob);
    }

    @Transactional
    public Optional<QueueJob> claimNextJob() {
        Optional<QueueJob> job = queueJobRepository
                .findFirstByStatusAndAvailableAtLessThanEqualOrderByIdAsc(
                        QueueJobStatus.QUEUED,
                        LocalDateTime.now()
                );
        if(job.isEmpty()) {
            return Optional.empty();
        }
        QueueJob queueJob = job.get();

        LocalDateTime now = LocalDateTime.now();

        queueJob.setStatus(QueueJobStatus.RUNNING);
        queueJob.setLeaseUntil(now.plusSeconds(30));
        queueJob.setAttempts(queueJob.getAttempts() + 1);

        return Optional.of(queueJobRepository.save(queueJob));
    }

    @Transactional
    public Optional<QueueJob> reclaimExpiredJob() {

        Optional<QueueJob> job = queueJobRepository
                .findFirstByStatusAndLeaseUntilBeforeOrderByIdAsc(
                        QueueJobStatus.RUNNING,
                        LocalDateTime.now()
                );

        if (job.isEmpty()) {
            return Optional.empty();
        }

        QueueJob queueJob = job.get();

        Optional<Run> run = runRepository.findById(queueJob.getRunId());

        if (run.isEmpty()) {
            queueJob.setStatus(QueueJobStatus.COMPLETED);
            queueJob.setLeaseUntil(null);
            queueJobRepository.save(queueJob);
            return Optional.empty();
        }

        RunStatus runStatus = run.get().getStatus();

        if (runStatus == RunStatus.FAILED
                || runStatus == RunStatus.SUCCEEDED
                || runStatus == RunStatus.CANCELLED) {

            queueJob.setStatus(QueueJobStatus.COMPLETED);
            queueJob.setLeaseUntil(null);
            queueJobRepository.save(queueJob);

            return Optional.empty();
        }

        queueJob.setStatus(QueueJobStatus.QUEUED);
        queueJob.setAvailableAt(LocalDateTime.now());
        queueJob.setLeaseUntil(null);

        return Optional.of(queueJobRepository.save(queueJob));
    }

    @Transactional
    public void completeJob(Long jobId) {

        QueueJob queueJob = queueJobRepository.findById(jobId)
                .orElseThrow(() ->
                        new RuntimeException("Queue job not found: " + jobId)
                );

        queueJob.setStatus(QueueJobStatus.COMPLETED);
        queueJob.setLeaseUntil(null);

        queueJobRepository.save(queueJob);
    }

}
