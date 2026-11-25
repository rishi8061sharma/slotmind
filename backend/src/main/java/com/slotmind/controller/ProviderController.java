package com.slotmind.controller;

import com.slotmind.dto.UpdateStatusRequest;
import com.slotmind.model.Provider;
import com.slotmind.model.ProviderStatus;
import com.slotmind.repository.ProviderRepository;
import com.slotmind.service.ProviderStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/providers")
public class ProviderController {
    private final ProviderStatusService providerStatusService;
    private final ProviderRepository repository;
    public ProviderController(ProviderStatusService service, ProviderRepository repository) { this.providerStatusService=service; this.repository=repository; }
    @GetMapping public ResponseEntity<List<Provider>> findAll() { return ResponseEntity.ok(repository.findAll()); }
    @PatchMapping("/{id}/status")
    public ResponseEntity<Provider> updateStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.accepted().body(providerStatusService.updateStatus(id, ProviderStatus.valueOf(request.status().toUpperCase())));
    }
}