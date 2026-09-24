package com.niteen.relay.exception;

public class WorkflowValidationException extends RuntimeException {
    public final String code;
    public WorkflowValidationException(String message, String code) {
        super(message);
        this.code = code;
    }
    public String getCode() {
        return code;
    }

}
