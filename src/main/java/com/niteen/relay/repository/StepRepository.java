package com.niteen.relay.repository;

import com.niteen.relay.entity.Step;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepRepository extends JpaRepository<Step, Long> {

    List<Step> findByRunIdOrderBySequenceNumberAsc(String runId);

}
