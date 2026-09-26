package com.niteen.relay.repository;


import com.niteen.relay.entity.QueueJob;
import com.niteen.relay.entity.QueueJobStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDateTime;
import java.util.Optional;

public interface QueueJobRepository extends JpaRepository<QueueJob, Long> {

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        Optional<QueueJob> findFirstByStatusAndAvailableAtLessThanEqualOrderByIdAsc(QueueJobStatus status,
                                                                                    LocalDateTime now);

}
