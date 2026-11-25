package com.slotmind.dto;

import com.slotmind.model.AppointmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(
        @NotBlank String patientName,
        @NotNull AppointmentType appointmentType,
        @NotBlank String providerId,
        @NotBlank String slotId
) { }
