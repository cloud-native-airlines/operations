package com.cloudnativeairlines.operations.scenario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "route")
public class Route {

    @Id
    private String id;

    @Column(name = "origin_code", nullable = false)
    private String originCode;

    @Column(name = "destination_code", nullable = false)
    private String destinationCode;

    @Column(name = "block_minutes", nullable = false)
    private int blockMinutes;

    protected Route() {
    }

    public String getId() {
        return id;
    }

    public String getOriginCode() {
        return originCode;
    }

    public String getDestinationCode() {
        return destinationCode;
    }

    public int getBlockMinutes() {
        return blockMinutes;
    }
}
