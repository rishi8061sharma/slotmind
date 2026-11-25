package com.slotmind.service;

import com.slotmind.exception.ResourceNotFoundException;
import com.slotmind.model.Provider;
import com.slotmind.model.ProviderStatus;
import com.slotmind.repository.ProviderRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProviderStatusService {
    private final ProviderRepository providerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public ProviderStatusService(ProviderRepository providerRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.providerRepository = providerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public Provider updateStatus(String providerId, ProviderStatus status) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Provider not found: " + providerId));
        provider.changeStatus(status);
        Provider saved = providerRepository.save(provider);

        if (status == ProviderStatus.OFFLINE) {
            eventPublisher.publishEvent(new ProviderOfflineEvent(providerId));
        }
        return saved;
    }
}
