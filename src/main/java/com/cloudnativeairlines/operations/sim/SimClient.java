package com.cloudnativeairlines.operations.sim;

import com.cloudnativeairlines.operations.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Thin read-only client for the Simulator's {@code GET /clock}. */
@Component
public class SimClient {

    private final RestClient client;

    public SimClient(RestClient.Builder builder,
            @Value("${operations.simulator.base-url:http://simulator:8000}") String baseUrl) {
        // Boot's autoconfigured builder carries the app's snake_case ObjectMapper,
        // so run_id / simulated_at bind correctly.
        this.client = builder.baseUrl(baseUrl).build();
    }

    /** Fetch the current clock, or fail with a 502 if the Simulator is unreachable. */
    public SimClock clock() {
        try {
            SimClock clock = client.get().uri("/clock").retrieve().body(SimClock.class);
            if (clock == null || clock.runId() == null || clock.simulatedAt() == null) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "simulator_unavailable",
                        "Simulator clock response was empty or incomplete");
            }
            return clock;
        } catch (RestClientException ex) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "simulator_unavailable",
                    "Could not reach the Simulator clock: " + ex.getMessage());
        }
    }
}
