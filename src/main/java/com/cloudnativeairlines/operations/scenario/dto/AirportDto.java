package com.cloudnativeairlines.operations.scenario.dto;

import com.cloudnativeairlines.operations.scenario.Airport;

public record AirportDto(
        String code,
        String name,
        double latitude,
        double longitude,
        String timezone) {

    public static AirportDto from(Airport airport) {
        return new AirportDto(
                airport.getCode(),
                airport.getName(),
                airport.getLatitude(),
                airport.getLongitude(),
                airport.getTimezone());
    }
}
