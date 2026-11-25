package com.slotmind.controller;

import com.slotmind.dto.UpdateStatusRequest;
import com.slotmind.model.Slot;
import com.slotmind.model.SlotStatus;
import com.slotmind.repository.SlotRepository;
import com.slotmind.service.SlotStatusService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/slots")
public class SlotController {
    private final SlotStatusService slotStatusService;
    private final SlotRepository repository;
    public SlotController(SlotStatusService service, SlotRepository repository) { this.slotStatusService=service; this.repository=repository; }
    @GetMapping public ResponseEntity<List<Slot>> findAll() { return ResponseEntity.ok(repository.findAll()); }
    @PatchMapping("/{id}/status")
    public ResponseEntity<Slot> updateStatus(@PathVariable String id, @Valid @RequestBody UpdateStatusRequest request) {
        return ResponseEntity.accepted().body(slotStatusService.updateStatus(id, SlotStatus.valueOf(request.status().toUpperCase())));
    }
}