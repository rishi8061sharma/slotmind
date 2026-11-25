package com.slotmind.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
public class Appointment {
    @Id private String id;
    private String patientName;
    @Enumerated(EnumType.STRING) private AppointmentType appointmentType;
    private String providerId;
    private String slotId;
    @Enumerated(EnumType.STRING) private AppointmentStatus status;

    protected Appointment() { }
    public Appointment(String id, String patientName, AppointmentType type,
                       String providerId, String slotId, AppointmentStatus status) {
        this.id=id; this.patientName=patientName; this.appointmentType=type;
        this.providerId=providerId; this.slotId=slotId; this.status=status;
    }
    public String getId() { return id; }
    public String getPatientName() { return patientName; }
    public AppointmentType getAppointmentType() { return appointmentType; }
    public String getProviderId() { return providerId; }
    public String getSlotId() { return slotId; }
    public AppointmentStatus getStatus() { return status; }
    public void rescheduleTo(String providerId, String slotId) {
        if (status != AppointmentStatus.RESCHEDULE_PENDING) {
            throw new IllegalStateException("Appointment is not pending reschedule");
        }
        this.providerId = providerId;
        this.slotId = slotId;
        this.status = AppointmentStatus.RESCHEDULED;
    }
    public void markReschedulePending() {
        if (status != AppointmentStatus.CONFIRMED) throw new IllegalStateException("Only confirmed appointments can become pending");
        status = AppointmentStatus.RESCHEDULE_PENDING;
    }
    public void complete() {
        if (status != AppointmentStatus.CONFIRMED && status != AppointmentStatus.RESCHEDULED) throw new IllegalStateException("Invalid appointment lifecycle transition");
        status = AppointmentStatus.COMPLETED;
    }
}