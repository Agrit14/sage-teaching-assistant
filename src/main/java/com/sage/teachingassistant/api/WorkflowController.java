package com.sage.teachingassistant.api;

import com.sage.teachingassistant.api.dto.RunDetailResponse;
import com.sage.teachingassistant.api.dto.RunMessageRequest;
import com.sage.teachingassistant.api.dto.RunResponse;
import com.sage.teachingassistant.api.dto.StartRunRequest;
import com.sage.teachingassistant.api.dto.WorkflowSummaryResponse;
import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.workflow.WorkflowEngine;
import com.sage.teachingassistant.workflow.WorkflowRegistry;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The workflow and file API.
 *
 * <p>Covers a whole run: start it, send it messages, upload docx files, download
 * generated docx / pdf files, and read run history.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowEngine engine;
    private final WorkflowRegistry registry;
    private final FileStorageService storageService;

    public WorkflowController(WorkflowEngine engine,
                              WorkflowRegistry registry,
                              FileStorageService storageService) {
        this.engine = engine;
        this.registry = registry;
        this.storageService = storageService;
    }

    /** Lists the workflows available to start, with their stages in order. */
    @GetMapping
    public List<WorkflowSummaryResponse> list() {
        return registry.all().stream().map(WorkflowSummaryResponse::from).toList();
    }

    /** Starts a run and executes its first stage. */
    @PostMapping("/runs")
    public RunResponse start(@Valid @RequestBody StartRunRequest request) {
        String effectiveMessage = request.resolveMessage();
        return RunResponse.from(engine.start(
                request.workflowKey(),
                effectiveMessage,
                request.className(),
                request.chapterName(),
                request.additionalDetails()));
    }

    /**
     * Confirms the current stage of a run and proceeds to the next stage
     * (or completes if on the final stage).
     */
    @PostMapping("/runs/{runId}/confirm")
    public RunResponse confirm(@PathVariable String runId) {
        return RunResponse.from(engine.handleMessage(runId, "confirm"));
    }

    /**
     * Sends the run a message. An approval moves to the next stage; anything else
     * is treated as feedback and the current stage runs again.
     */
    @PostMapping("/runs/{runId}/messages")
    public RunResponse send(@PathVariable String runId,
                            @Valid @RequestBody RunMessageRequest request) {
        return RunResponse.from(engine.handleMessage(runId, request.message()));
    }

    /** The run's current position plus its full execution history. */
    @GetMapping("/runs/{runId}")
    public RunDetailResponse describe(@PathVariable String runId) {
        return RunDetailResponse.from(engine.describe(runId));
    }

    /** Downloads the generated Word (.docx) document for a given run. */
    @GetMapping("/runs/{runId}/files/docx")
    public ResponseEntity<Resource> downloadDocx(@PathVariable String runId) {
        Optional<File> file = storageService.findDocx(runId);
        if (file.isEmpty() || !file.get().exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Word document not found for run: " + runId);
        }

        Resource resource = new FileSystemResource(file.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.get().getName() + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .body(resource);
    }

    /** Downloads or views the generated Alpha Tutor PDF for a given run. */
    @GetMapping("/runs/{runId}/files/pdf")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String runId) {
        Optional<File> file = storageService.findPdf(runId);
        if (file.isEmpty() || !file.get().exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "PDF document not found for run: " + runId);
        }

        Resource resource = new FileSystemResource(file.get());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.get().getName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }

    /**
     * Uploads a Word (.docx) document to a run (used for direct PDF print or replacement).
     */
    @PostMapping(value = "/runs/{runId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunResponse uploadDocx(@PathVariable String runId,
                                  @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        try {
            storageService.saveDocx(runId, "uploaded", file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded file: " + e.getMessage());
        }

        // Re-execute current stage with notice that file was uploaded
        return RunResponse.from(engine.handleMessage(runId, "File uploaded: " + file.getOriginalFilename()));
    }

    /**
     * Starts a new run and uploads the Word document in a single step (ideal for 1-click PDF Print on Android).
     */
    @PostMapping(value = "/runs/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RunResponse startWithFile(
            @RequestParam(value = "workflowKey", defaultValue = "pdf-print") String workflowKey,
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File cannot be empty");
        }

        // 1. Start run
        RunResponse run = RunResponse.from(engine.start(workflowKey, "Direct upload: " + file.getOriginalFilename()));

        // 2. Save uploaded file for this run
        try {
            storageService.saveDocx(run.runId(), "uploaded", file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save uploaded file: " + e.getMessage());
        }

        // 3. Convert into Alpha Tutor PDF template and return
        return RunResponse.from(engine.handleMessage(run.runId(), "Process uploaded file"));
    }
}
