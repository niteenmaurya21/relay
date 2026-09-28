package com.niteen.relay.service;


import com.niteen.relay.entity.QueueJob;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class QueueWorker {

    private final RunExecutionService runExecutionService;
    private final QueueJobService queueJobService;

    public  QueueWorker(QueueJobService queueJobService,
                        RunExecutionService runExecutionService)
    {
        this.queueJobService = queueJobService;
        this.runExecutionService= runExecutionService;
    }

    @Scheduled(fixedDelay = 3000)
    public void processQueue() {

        queueJobService.reclaimExpiredJob();

        Optional<QueueJob> job = queueJobService.claimNextJob();

        if (job.isEmpty()) {
            return;
        }

        QueueJob queueJob = job.get();

        try {
            runExecutionService.execute(queueJob.getRunId());
            queueJobService.completeJob(queueJob.getId());

        } catch (Exception e) {
            // The RunExecutionService has already persisted the run as FAILED.
            // Do not mark the queue job as COMPLETED.
        }
    }

}
