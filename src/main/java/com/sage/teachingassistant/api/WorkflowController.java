package com.sage.teachingassistant.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sage.teachingassistant.ai.EducationalContentService;
import com.sage.teachingassistant.api.dto.FeedbackRequest;
import com.sage.teachingassistant.api.dto.ImprovementRuleResponse;
import com.sage.teachingassistant.api.dto.RunDetailResponse;
import com.sage.teachingassistant.api.dto.RunMessageRequest;
import com.sage.teachingassistant.api.dto.RunResponse;
import com.sage.teachingassistant.api.dto.StartRunRequest;
import com.sage.teachingassistant.api.dto.WorkflowSummaryResponse;
import com.sage.teachingassistant.document.FileStorageService;
import com.sage.teachingassistant.domain.RunStatus;
import com.sage.teachingassistant.domain.StageExecution;
import com.sage.teachingassistant.domain.WorkflowRun;
import com.sage.teachingassistant.improvement.ImprovementRuleService;
import com.sage.teachingassistant.repository.StageExecutionRepository;
import com.sage.teachingassistant.repository.WorkflowRunRepository;
import com.sage.teachingassistant.workflow.WorkflowEngine;
import com.sage.teachingassistant.workflow.WorkflowRegistry;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Map;
import java.util.Optional;

/**
 * The workflow and file API.
 *
 * <p>Covers a whole run: start it, send it messages, upload docx files, download
 * generated docx / pdf files, and read run history.
 * Routes general questions without document context directly to the LLM.
 * Exposes feedback endpoints for the Improvement Layer.
 */
@RestController
@RequestMapping("/api/v1/workflows")
public class WorkflowController {

    private final WorkflowEngine engine;
    private final WorkflowRegistry registry;
    private final FileStorageService storageService;
    private final EducationalContentService educationalService;
    private final WorkflowRunRepository runRepository;
    private final StageExecutionRepository executionRepository;
    private final ObjectMapper objectMapper;
    private final ImprovementRuleService improvementRuleService;

    public WorkflowController(WorkflowEngine engine,
                              WorkflowRegistry registry,
                              FileStorageService storageService,
                              EducationalContentService educationalService,
                              WorkflowRunRepository runRepository,
                              StageExecutionRepository executionRepository,
                              ObjectMapper objectMapper) {
        this(engine, registry, storageService, educationalService, runRepository, executionRepository, objectMapper, null);
    }

    @Autowired
    public WorkflowController(WorkflowEngine engine,
                              WorkflowRegistry registry,
                              FileStorageService storageService,
                              EducationalContentService educationalService,
                              WorkflowRunRepository runRepository,
                              StageExecutionRepository executionRepository,
                              ObjectMapper objectMapper,
                              @Autowired(required = false) ImprovementRuleService improvementRuleService) {
        this.engine = engine;
        this.registry = registry;
        this.storageService = storageService;
        this.educationalService = educationalService;
        this.runRepository = runRepository;
        this.executionRepository = executionRepository;
        this.objectMapper = objectMapper;
        this.improvementRuleService = improvementRuleService;
    }

    /** Lists the workflows available to start, with their stages in order. */
    @GetMapping
    public List<WorkflowSummaryResponse> list() {
        return registry.all().stream().map(WorkflowSummaryResponse::from).toList();
    }

    /**
     * Starts a run and executes its first stage if an option/context is chosen,
     * or directly routes general questions to the LLM to give the answer immediately.
     */
    @PostMapping("/runs")
    public RunResponse start(@RequestBody(required = false) StartRunRequest request) {
        if (request == null) {
            request = new StartRunRequest(null, "Hello", null, null, null);
        }

        // 1. If an option or class/chapter context is selected: real query to make something
        if (request.isRealDocumentQuery()) {
            String effectiveMessage = request.resolveMessage();
            return RunResponse.from(engine.start(
                    request.resolveWorkflowKey(),
                    effectiveMessage,
                    request.className(),
                    request.chapterName(),
                    request.additionalDetails()));
        }

        // 2. Rest: for general questions without option or context, route directly to LLM to answer!
        String query = request.resolveQuestion();
        String answer = educationalService.answerGeneralQuestion(query);

        WorkflowRun run = runRepository.save(new WorkflowRun("general-qa"));
        run.complete();
        runRepository.save(run);

        StageExecution execution = new StageExecution(run.getId(), "direct-answer", 0, 1);
        String payloadJson = "{}";
        try {
            payloadJson = objectMapper.writeValueAsString(Map.of("query", query, "answer", answer, "type", "direct_llm_response"));
        } catch (Exception ignored) {}

        execution.recordOutcome(answer, payloadJson);
        execution.approve();
        executionRepository.save(execution);

        return new RunResponse(
                run.getId(),
                "general-qa",
                RunStatus.COMPLETED,
                0,
                1,
                "direct-answer",
                "Direct Educational Q&A",
                answer,
                Map.of("answer", answer, "type", "direct_llm_response"),
                true,
                false);
    }

    /**
     * Dedicated direct chat endpoint.
     * Routes queries with option/context to document workflows, and general questions directly to LLM.
     */
    @PostMapping("/chat")
    public RunResponse chat(@RequestBody(required = false) StartRunRequest request) {
        return start(request);
    }

    /**
     * Confirms / approves the current stage of a run and proceeds to the next stage
     * (or completes if on the final stage).
     *
     * <p>Both {@code /confirm} and {@code /approve} are mapped so clients can use
     * whichever naming convention their UI utilizes.
     */
    @PostMapping({"/runs/{runId}/confirm", "/runs/{runId}/approve"})
    public RunResponse confirm(@PathVariable String runId) {
        return RunResponse.from(engine.handleMessage(runId, "approve"));
    }

    /**
     * Sends the run a message. An approval moves to the next stage; anything else
     * is treated as feedback and the current stage runs again.
     */
    @PostMapping("/runs/{runId}/messages")
    public RunResponse send(@PathVariable String runId,
                            @RequestBody(required = false) RunMessageRequest request) {
        String msg = (request != null) ? request.resolveMessage() : "approve";

        Optional<WorkflowRun> runOpt = runRepository.findById(runId);
        if (runOpt.isPresent() && "general-qa".equals(runOpt.get().getWorkflowKey())) {
            String answer = educationalService.answerGeneralQuestion(msg);
            return new RunResponse(
                    runId,
                    "general-qa",
                    RunStatus.COMPLETED,
                    0,
                    1,
                    "direct-answer",
                    "Direct Educational Q&A",
                    answer,
                    Map.of("answer", answer, "type", "direct_llm_response"),
                    true,
                    false);
        }

        return RunResponse.from(engine.handleMessage(runId, msg));
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

    /**
     * Records teacher feedback/suggestion for a specific stage or globally,
     * saving it to the database as an improvement rule that guides future LLM generation.
     */
    @PostMapping("/feedback")
    public ImprovementRuleResponse submitFeedback(@Valid @RequestBody FeedbackRequest request) {
        if (improvementRuleService == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Improvement service is not configured");
        }
        return improvementRuleService.recordFeedback(request);
    }

    /**
     * Lists active improvement rules, optionally filtered by stageKey.
     */
    @GetMapping("/feedback")
    public List<ImprovementRuleResponse> listFeedbackRules(
            @RequestParam(value = "stageKey", required = false) String stageKey) {
        if (improvementRuleService == null) {
            return List.of();
        }
        if (stageKey != null && !stageKey.isBlank()) {
            return improvementRuleService.listRulesForStage(stageKey.trim());
        }
        return improvementRuleService.listActiveRules();
    }
}
