package com.niteen.relay.service;

import org.springframework.stereotype.Service;

@Service
public class DelayNodeExecutor {

    public long getDelaySeconds(long seconds) {

        if (seconds < 0) {
            throw new IllegalArgumentException(
                    "Delay seconds cannot be negative"
            );
        }

        return seconds;
    }
}