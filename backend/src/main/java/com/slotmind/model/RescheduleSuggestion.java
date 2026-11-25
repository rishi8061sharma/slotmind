package com.slotmind.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "reschedule_suggestions")
public class RescheduleSuggestion {
    @Id private String id;
    private String appointmentId;
    private String recommendedSlotId;
    private String alternateProviderId;
    private BigDecimal confidence;
    private String reasoning;
    @Enumerated(EnumType.STRING) private SuggestionStatus status;
    @Enumerated(EnumType.STRING) private TriggerReason triggerReason;

    protected RescheduleSuggestion() { }
    public RescheduleSuggestion(String id, String appointmentId, String slotId,
                                String alternateProviderId, BigDecimal confidence,
                                String reasoning, TriggerReason triggerReason) {
        this.id=id; this.appointmentId=appointmentId; this.recommendedSlotId=slotId;
        this.alternateProviderId=alternateProviderId; this.confidence=confidence;
        this.reasoning=reasoning; this.status=SuggestionStatus.PENDING;
        this.triggerReason=triggerReason;
    }
    public String getId() { return id; }
    public String getAppointmentId() { return appointmentId; }
    public String getRecommendedSlotId() { return recommendedSlotId; }
    public String getAlternateProviderId() { return alternateProviderId; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public SuggestionStatus getStatus() { return status; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public void accept() { if (status != SuggestionStatus.PENDING) throw new IllegalStateException("Suggestion is not pending"); status=SuggestionStatus.ACCEPTED; }
    public void reject() { if (status != SuggestionStatus.PENDING) throw new IllegalStateException("Suggestion is not pending"); status=SuggestionStatus.REJECTED; }
}