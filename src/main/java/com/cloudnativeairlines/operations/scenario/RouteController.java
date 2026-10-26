package com.cloudnativeairlines.operations.scenario;

import com.cloudnativeairlines.operations.api.Pagination;
import com.cloudnativeairlines.operations.scenario.dto.RouteDto;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/routes")
public class RouteController {

    private final RouteRepository routes;

    public RouteController(RouteRepository routes) {
        this.routes = routes;
    }

    public record RouteListResponse(List<RouteDto> routes, Pagination pagination) {
    }

    @GetMapping
    public RouteListResponse list() {
        List<RouteDto> items = routes.findAll(Sort.by("id")).stream()
                .map(RouteDto::from)
                .toList();
        return new RouteListResponse(items, Pagination.of(items.size()));
    }
}
