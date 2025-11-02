package com.github.jwxa.controller.scenario;

import com.github.jwxa.scenario.playback.ScenarioPlaybackNextResult;
import com.github.jwxa.scenario.playback.ScenarioPlaybackService;
import com.github.jwxa.scenario.playback.ScenarioPlaybackStartResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/scenario/playback")
@RequiredArgsConstructor
public class ScenarioPlaybackController {

    private final ScenarioPlaybackService playbackService;

    @PostMapping("/start")
    public ScenarioPlaybackStartResult start(@RequestBody ScenarioPlaybackStartRequest request) {
        log.info("[ScenarioPlayback] start scenario {}", request.scenario());
        return playbackService.start(request.scenario(), request.parameters());
    }

    @PostMapping("/next")
    public ResponseEntity<ScenarioPlaybackNextResult> next(@RequestBody ScenarioPlaybackTokenRequest request) {
        ScenarioPlaybackNextResult result = playbackService.next(request.token());
        if (result.scenarioCode() == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset")
    public void reset(@RequestBody ScenarioPlaybackTokenRequest request) {
        playbackService.reset(request.token());
    }

    public record ScenarioPlaybackStartRequest(String scenario, Map<String, Object> parameters) {}

    public record ScenarioPlaybackTokenRequest(String token) {}
}
