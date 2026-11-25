package com.slotmind.controller;

import com.slotmind.dto.SuggestionResponse;
import com.slotmind.service.SuggestionService;
import com.slotmind.service.SchedulingEngine;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/appointments")
public class SseSuggestionController {
    private final SuggestionService suggestionService;
    private final SchedulingEngine schedulingEngine;
    public SseSuggestionController(SuggestionService suggestionService, SchedulingEngine schedulingEngine) { this.suggestionService=suggestionService; this.schedulingEngine=schedulingEngine; }

    @PostMapping(value="/{appointmentId}/suggest/stream", produces=MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String appointmentId) {
        SseEmitter emitter=new SseEmitter(30_000L);
        try {
            emitter.send(SseEmitter.event().name("reasoning").data("Finding eligible open slots"));
            SuggestionResponse result=SuggestionResponse.from(suggestionService.createSuggestion(appointmentId));
            if (schedulingEngine.wasAiFallbackUsed()) emitter.send(SseEmitter.event().name("error").data("AI unavailable; rule-based fallback used"));
            emitter.send(SseEmitter.event().name("recommendation").data(result));
            emitter.send(SseEmitter.event().name("complete").data("Recommendation ready"));
            emitter.complete();
        } catch (Exception ex) {
            try { emitter.send(SseEmitter.event().name("error").data(ex.getMessage())); } catch (Exception ignored) { }
            emitter.completeWithError(ex);
        }
        return emitter;
    }
}