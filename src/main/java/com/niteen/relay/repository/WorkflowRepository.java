package com.niteen.relay.repository;

import com.niteen.relay.entity.Workflow;
import com.niteen.relay.entity.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;


public interface WorkflowRepository extends JpaRepository<Workflow, String> {



}
