package com.slotmind.controller;

import com.slotmind.dto.SuggestionResponse;
import com.slotmind.service.SuggestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/appointments")
public class RescheduleController {
    private final SuggestionService suggestionService;
    public RescheduleController(SuggestionService suggestionService) { this.suggestionService=suggestionService; }
    @PostMapping("/{appointmentId}/suggest")
    public ResponseEntity<SuggestionResponse> suggest(@PathVariable String appointmentId) {
        return ResponseEntity.accepted().body(SuggestionResponse.from(suggestionService.createSuggestion(appointmentId)));
    }
}