package com.example.projeto2.API.Controllers;

import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthApiController {

    @GetMapping("/api/health")
    public HealthResponse health() {
        return new HealthResponse("ok", OffsetDateTime.now());
    }

    public record HealthResponse(
            String status,
            OffsetDateTime timestamp
    ) {
    }
}
