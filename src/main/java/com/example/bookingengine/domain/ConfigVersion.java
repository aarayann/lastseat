package com.example.bookingengine.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class ConfigVersion {

    @Id
    private String clientId;

    private long version;

    protected ConfigVersion() {
    }

    public ConfigVersion(String clientId, long version) {
        this.clientId = clientId;
        this.version = version;
    }

    public void increment() {
        version++;
    }

    public String getClientId() {
        return clientId;
    }

    public long getVersion() {
        return version;
    }
}
