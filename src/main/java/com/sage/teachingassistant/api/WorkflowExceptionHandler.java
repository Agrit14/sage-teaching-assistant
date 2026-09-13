package com.sage.teachingassistant.api;

import com.sage.teachingassistant.workflow.RunFinishedException;
import com.sage.teachingassistant.workflow.RunNotFoundException;
import com.sage.teachingassistant.workflow.WorkflowNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps workflow failures onto sensible HTTP responses. */
@RestControllerAdvice
public class WorkflowExceptionHandler {

    @ExceptionHandler(WorkflowNotFoundException.class)
    public ProblemDetail onUnknownWorkflow(WorkflowNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(RunNotFoundException.class)
    public ProblemDetail onUnknownRun(RunNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** The run is already finished, so there is nothing left to advance. */
    @ExceptionHandler(RunFinishedException.class)
    public ProblemDetail onFinishedRun(RunFinishedException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }
}
