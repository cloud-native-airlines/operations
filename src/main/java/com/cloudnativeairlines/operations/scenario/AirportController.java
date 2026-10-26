package com.cloudnativeairlines.operations.scenario;

import com.cloudnativeairlines.operations.api.ApiException;
import com.cloudnativeairlines.operations.api.Pagination;
import com.cloudnativeairlines.operations.scenario.dto.AirportDto;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/airports")
public class AirportController {

    private final AirportRepository airports;

    public AirportController(AirportRepository airports) {
        this.airports = airports;
    }

    public record AirportListResponse(List<AirportDto> airports, Pagination pagination) {
    }

    public record AirportResponse(AirportDto airport) {
    }

    @GetMapping
    public AirportListResponse list() {
        List<AirportDto> items = airports.findAll(Sort.by("code")).stream()
                .map(AirportDto::from)
                .toList();
        return new AirportListResponse(items, Pagination.of(items.size()));
    }

    @GetMapping("/{code}")
    public AirportResponse get(@PathVariable String code) {
        return airports.findById(code)
                .map(AirportDto::from)
                .map(AirportResponse::new)
                .orElseThrow(() -> ApiException.notFound("No airport with code " + code));
    }
}
