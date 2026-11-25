package com.slotmind.controller;

import com.slotmind.dto.CreateAppointmentRequest;
import com.slotmind.model.Appointment;
import com.slotmind.model.AppointmentStatus;
import com.slotmind.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    // DM-01, DM-02, DM-03, DM-04
    @PostMapping
    public ResponseEntity<Appointment> create(@Valid @RequestBody CreateAppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(appointmentService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> find(
            @RequestParam(required = false) AppointmentStatus status) {
        return ResponseEntity.ok(appointmentService.findByStatus(status));
    }

    // DM-05
    @PostMapping("/{id}/complete")
    public ResponseEntity<Appointment> complete(@PathVariable String id) {
        return ResponseEntity.ok(appointmentService.complete(id));
    }
}