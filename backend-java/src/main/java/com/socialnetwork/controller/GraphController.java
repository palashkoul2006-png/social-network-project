package com.socialnetwork.controller;

import com.socialnetwork.service.SocialGraphService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class GraphController {
    private final SocialGraphService service;

    public GraphController(SocialGraphService service) {
        this.service = service;
    }

    /**
     * Returns a coloring of the friendship graph where adjacent users have different colors.
     * The caller can request the maximum number of colors to try (default 4).
     */
    @GetMapping("/graph/color")
    public Map<String, Integer> getGraphColoring(@RequestParam(defaultValue = "4") int colors) {
        Map<String, Integer> result = service.colorGraph(colors);
        // Return an empty map if coloring not possible with the given number of colors.
        return result != null ? result : Map.of();
    }
}
