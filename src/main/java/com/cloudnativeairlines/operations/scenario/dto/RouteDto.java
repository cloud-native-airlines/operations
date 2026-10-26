package com.cloudnativeairlines.operations.scenario.dto;

import com.cloudnativeairlines.operations.scenario.Route;

public record RouteDto(
        String id,
        String originCode,
        String destinationCode,
        int blockMinutes) {

    public static RouteDto from(Route route) {
        return new RouteDto(
                route.getId(),
                route.getOriginCode(),
                route.getDestinationCode(),
                route.getBlockMinutes());
    }
}
