package com.cloudnativeairlines.operations.scenario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "aircraft")
public class Aircraft {

    @Id
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(name = "base_airport_code", nullable = false)
    private String baseAirportCode;

    protected Aircraft() {
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getBaseAirportCode() {
        return baseAirportCode;
    }
}
