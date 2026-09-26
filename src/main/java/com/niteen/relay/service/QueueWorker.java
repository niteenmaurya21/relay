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

        Optional<QueueJob> job = queueJobService.claimNextJob();


        if(job.isEmpty()){
            return;
        }
        QueueJob queueJob = job.get();
        runExecutionService.execute(queueJob.getRunId());


    }

}
