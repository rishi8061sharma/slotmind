package com.slotmind.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "slots")
public class Slot {
    @Id private String id;
    private String providerId;
    private LocalDateTime startTime;
    private Integer durationMinutes;
    @Enumerated(EnumType.STRING) private SlotStatus status;
    protected Slot() { }
    public String getId() { return id; }
    public String getProviderId() { return providerId; }
    public LocalDateTime getStartTime() { return startTime; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public SlotStatus getStatus() { return status; }
    public void book() { if (status != SlotStatus.OPEN) throw new IllegalStateException("Slot is not open"); status=SlotStatus.BOOKED; }
    public void open() { if (status == SlotStatus.CANCELLED) throw new IllegalStateException("Cancelled slot cannot be reopened"); status=SlotStatus.OPEN; }
    public void cancel() { status=SlotStatus.CANCELLED; }
}