package com.slotmind.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "providers")
public class Provider {
    @Id
    private String id;
    private String name;
    @Enumerated(EnumType.STRING)
    private ProviderSpecialty specialty;
    @Enumerated(EnumType.STRING)
    private ProviderStatus status;

    protected Provider() { }

    public String getId() { return id; }
    public String getName() { return name; }
    public ProviderSpecialty getSpecialty() { return specialty; }
    public ProviderStatus getStatus() { return status; }

    public void changeStatus(ProviderStatus newStatus) {
        this.status = newStatus;
    }
}