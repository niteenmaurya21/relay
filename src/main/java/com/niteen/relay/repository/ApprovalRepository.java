package com.niteen.relay.repository;

import com.niteen.relay.entity.Approval;
import com.niteen.relay.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {

    List<Approval> findByStatus(ApprovalStatus status);

    Optional<Approval> findByRunIdAndNodeIdAndStatus(
            String runId,
            String nodeId,
            ApprovalStatus status
    );
}