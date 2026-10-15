package com.sage.teachingassistant.api;

import com.sage.teachingassistant.workflow.RunFinishedException;
import com.sage.teachingassistant.workflow.RunNotFoundException;
import com.sage.teachingassistant.workflow.WorkflowNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

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

    /** Uploaded file exceeds configured maximum multipart size (HTTP 413 Payload Too Large). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ProblemDetail onMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "The uploaded Word document exceeds the maximum allowed file size (100MB). " +
                        "Please compress embedded images or upload a smaller file."
        );
        problem.setTitle("File Upload Too Large");
        return problem;
    }

    /** Malformed or interrupted multipart request. */
    @ExceptionHandler(MultipartException.class)
    public ProblemDetail onMultipartException(MultipartException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Failed to process uploaded file: " + e.getMessage()
        );
        problem.setTitle("Multipart Upload Error");
        return problem;
    }
}
