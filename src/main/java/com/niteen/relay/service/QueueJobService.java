package com.niteen.relay.service;


import com.niteen.relay.entity.QueueJob;
import com.niteen.relay.entity.QueueJobStatus;
import com.niteen.relay.repository.QueueJobRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class QueueJobService {

    private final QueueJobRepository queueJobRepository;

    public QueueJobService(QueueJobRepository queueJobRepository) {
        this.queueJobRepository = queueJobRepository;
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

}
