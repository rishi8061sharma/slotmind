package com.slotmind.service;

import com.slotmind.dto.CreateAppointmentRequest;
import com.slotmind.exception.BadRequestException;
import com.slotmind.exception.ResourceNotFoundException;
import com.slotmind.model.Appointment;
import com.slotmind.model.AppointmentStatus;
import com.slotmind.model.Provider;
import com.slotmind.model.Slot;
import com.slotmind.model.SlotStatus;
import com.slotmind.repository.AppointmentRepository;
import com.slotmind.repository.ProviderRepository;
import com.slotmind.repository.SlotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;
    private final SlotRepository slotRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              ProviderRepository providerRepository,
                              SlotRepository slotRepository) {
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
        this.slotRepository = slotRepository;
    }

    @Transactional
    public Appointment create(CreateAppointmentRequest request) {
        Provider provider = providerRepository.findById(request.providerId())
                .orElseThrow(() -> new ResourceNotFoundException("Provider not found: " + request.providerId()));
        Slot slot = slotRepository.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found: " + request.slotId()));

        if (!provider.getId().equals(slot.getProviderId())) {
            throw new BadRequestException("Slot does not belong to provider");
        }
        if (slot.getStatus() == SlotStatus.CANCELLED) {
            throw new BadRequestException("Cannot book a cancelled slot");
        }
        if (slot.getStatus() == SlotStatus.BOOKED || appointmentRepository.findBySlotId(slot.getId()).isPresent()) {
            throw new BadRequestException("Slot is already booked");
        }

        Appointment appointment = new Appointment(
                "APT-" + java.util.UUID.randomUUID(), request.patientName(),
                request.appointmentType(), provider.getId(), slot.getId(),
                AppointmentStatus.CONFIRMED);
        slot.book();
        slotRepository.save(slot);
        return appointmentRepository.save(appointment);
    }

    public List<Appointment> findByStatus(AppointmentStatus status) {
        return status == null ? appointmentRepository.findAll() : appointmentRepository.findByStatus(status);
    }

    @Transactional
    public Appointment complete(String id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + id));
        appointment.complete();
        return appointmentRepository.save(appointment);
    }
}