package com.slotmind.ai;

import com.slotmind.model.Provider;
import com.slotmind.model.Slot;
import com.slotmind.service.SchedulingContext;
import com.slotmind.service.SchedulingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.List;

@Component
public class PythonAiGateway implements LlmGateway {
    private final RestClient client;
    public PythonAiGateway(RestClient.Builder builder,
                           @Value("${slotmind.ai.gateway-url:http://localhost:8001}") String gatewayUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(5));
        this.client = builder.requestFactory(factory).baseUrl(gatewayUrl).build();
    }
    @Override
    public SchedulingStrategy.Recommendation recommend(SchedulingContext context) {
        PythonResponse response = client.post().uri("/api/v1/schedule/recommend")
                .body(PythonRequest.from(context)).retrieve().body(PythonResponse.class);
        if (response == null) throw new IllegalStateException("Python gateway returned no response");
        return new SchedulingStrategy.Recommendation(response.slotId(), response.alternateProviderId(), response.confidence(), response.reasoning());
    }
    record PythonRequest(String appointmentId, String appointmentType, String providerId,
                         String patientName, int requiredDurationMinutes, List<PythonSlot> slots,
                         List<PythonProvider> providers, String triggerReason) {
        static PythonRequest from(SchedulingContext c) {
            return new PythonRequest(c.appointment().getId(), c.appointment().getAppointmentType().name(),
                    c.appointment().getProviderId(), c.appointment().getPatientName(), c.requiredDurationMinutes(),
                    c.availableSlots().stream().map(PythonSlot::from).toList(),
                    c.providers().stream().map(PythonProvider::from).toList(), c.triggerReason().name());
        }
    }
    record PythonSlot(String slotId, String providerId, java.time.LocalDateTime startTime, Integer durationMinutes, String status) {
        static PythonSlot from(Slot s) { return new PythonSlot(s.getId(), s.getProviderId(), s.getStartTime(), s.getDurationMinutes(), s.getStatus().name()); }
    }
    record PythonProvider(String providerId, String specialty, String status) {
        static PythonProvider from(Provider p) { return new PythonProvider(p.getId(), p.getSpecialty().name(), p.getStatus().name()); }
    }
    record PythonResponse(String slotId, String alternateProviderId, BigDecimal confidence, String reasoning) { }
}