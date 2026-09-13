package com.sage.teachingassistant.service;

import org.springframework.stereotype.Service;

/**
 * Turns an incoming request into the response that goes back out.
 *
 * <p>This is the single place the response logic lives. Right now it is a
 * placeholder that echoes the input back, so the wiring can be exercised
 * end to end.
 */
@Service
public class ResponseService {

    public String respond(String message) {
        // TODO: replace with the real response logic.
        return "Received: " + message;
    }
}
