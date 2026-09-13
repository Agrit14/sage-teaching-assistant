package com.sage.teachingassistant.api;

import com.sage.teachingassistant.api.dto.ApiRequest;
import com.sage.teachingassistant.api.dto.ApiResponse;
import com.sage.teachingassistant.service.ResponseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The endpoint the calling application posts to. */
@RestController
@RequestMapping("/api/v1")
public class RequestController {

    private final ResponseService responseService;

    public RequestController(ResponseService responseService) {
        this.responseService = responseService;
    }

    @PostMapping("/request")
    public ApiResponse handle(@Valid @RequestBody ApiRequest request) {
        return new ApiResponse(responseService.respond(request.message()));
    }
}
