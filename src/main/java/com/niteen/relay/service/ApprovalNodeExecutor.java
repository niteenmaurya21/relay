package com.niteen.relay.service;

import org.springframework.stereotype.Service;

@Service
public class ApprovalNodeExecutor {

    public String resolveMessage(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Approval message is required"
            );
        }

        return message;
    }
}