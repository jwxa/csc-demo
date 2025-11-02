package com.github.jwxa.controller.scenario;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 简单映射，确保在部分环境中也能访问到可视化页面。
 */
@RestController
public class ScenarioVisualizerController {

    @GetMapping(value = "/scenario/visualizer", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<InputStreamResource> visualizer() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/scenario-visualizer.html");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(new InputStreamResource(resource.getInputStream()));
    }
}
