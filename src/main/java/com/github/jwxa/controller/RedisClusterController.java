package com.github.jwxa.controller;

import com.github.jwxa.service.RedisClusterAdminService;
import com.github.jwxa.service.ShellService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/redis-cluster")
@RequiredArgsConstructor
public class RedisClusterController {

    private final RedisClusterAdminService adminService;

    @GetMapping("/status")
    public RedisClusterAdminService.ClusterStatus status() {
        return adminService.status();
    }

    @PostMapping("/start")
    public ResponseEntity<?> start() {
        ShellService.ExecResult r = adminService.start();
        log.info("Start cluster: exit={} bytesOut={} bytesErr={}", r.getExitCode(),
                r.getStdout().length(), r.getStderr().length());
        return ResponseEntity.status(r.getExitCode() == 0 ? 200 : 500)
                .body(Map.of(
                        "exitCode", r.getExitCode(),
                        "stdout", r.getStdout(),
                        "stderr", r.getStderr()
                ));
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stop() {
        ShellService.ExecResult r = adminService.stop();
        log.info("Stop cluster: exit={} bytesOut={} bytesErr={}", r.getExitCode(),
                r.getStdout().length(), r.getStderr().length());
        return ResponseEntity.status(r.getExitCode() == 0 ? 200 : 500)
                .body(Map.of(
                        "exitCode", r.getExitCode(),
                        "stdout", r.getStdout(),
                        "stderr", r.getStderr()
                ));
    }
}

