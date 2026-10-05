package com.niteen.relay.repository;

import com.niteen.relay.entity.Step;
import com.niteen.relay.entity.StepStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StepRepository extends JpaRepository<Step, Long> {

    List<Step> findByRunIdOrderBySequenceNumberAsc(String runId);

    Optional<Step> findByRunIdAndNodeIdAndStatus(
            String runId,
            String nodeId,
            StepStatus status
    );
}