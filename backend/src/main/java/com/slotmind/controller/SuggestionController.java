package com.slotmind.controller;

import com.slotmind.dto.SuggestionDecisionRequest;
import com.slotmind.dto.SuggestionResponse;
import com.slotmind.model.SuggestionStatus;
import com.slotmind.repository.RescheduleSuggestionRepository;
import com.slotmind.service.SuggestionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/suggestions")
public class SuggestionController {
    private final SuggestionService suggestionService;
    private final RescheduleSuggestionRepository repository;
    public SuggestionController(SuggestionService suggestionService, RescheduleSuggestionRepository repository) { this.suggestionService=suggestionService; this.repository=repository; }

    @GetMapping
    public ResponseEntity<List<SuggestionResponse>> find(@RequestParam(required=false) SuggestionStatus status) {
        var values=status == null ? repository.findAll() : repository.findByStatus(status);
        return ResponseEntity.ok(values.stream().map(SuggestionResponse::from).toList());
    }
    @PatchMapping("/{id}")
    public ResponseEntity<SuggestionResponse> decide(@PathVariable String id, @Valid @RequestBody SuggestionDecisionRequest request) {
        SuggestionStatus status;
        try { status=SuggestionStatus.valueOf(request.status().toUpperCase()); }
        catch (IllegalArgumentException ex) { throw new com.slotmind.exception.ConflictException("Invalid suggestion transition"); }
        return ResponseEntity.ok(SuggestionResponse.from(suggestionService.decide(id, status)));
    }
}