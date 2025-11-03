package com.github.jwxa.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class ShellService {

    public ExecResult run(List<String> command) {
        ProcessBuilder pb = new ProcessBuilder(command);
        // Inherit current working directory (project root when running Spring Boot)
        pb.redirectErrorStream(false);
        try {
            Process process = pb.start();
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            int exit = process.waitFor();
            return new ExecResult(exit, stdout, stderr);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ExecResult(1, "", e.getMessage());
        }
    }

    public ExecResult dockerCompose(String composeFile, String... args) {
        // Prefer Docker Compose v2: `docker compose ...`, fallback to legacy `docker-compose ...`
        List<String> v2 = new ArrayList<>();
        v2.add("docker");
        v2.add("compose");
        v2.addAll(Arrays.asList("-f", composeFile));
        v2.addAll(Arrays.asList(args));

        ExecResult result = run(v2);
        if (result.exitCode == 0) {
            return result;
        }

        List<String> legacy = new ArrayList<>();
        legacy.add("docker-compose");
        legacy.addAll(Arrays.asList("-f", composeFile));
        legacy.addAll(Arrays.asList(args));
        return run(legacy);
    }

    private String readStream(java.io.InputStream in) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        }
    }

    @Data
    public static class ExecResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
    }
}

