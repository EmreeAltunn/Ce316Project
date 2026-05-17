package com.iae.process;

import com.iae.model.ProcessResult;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ProcessExecutor {

    public static final int DEFAULT_TIMEOUT_SECONDS = 30;

    public ProcessResult execute(String command, File workingDir) throws IOException {
        if (command == null || command.trim().isEmpty()) {
            throw new IOException("Command must not be empty.");
        }

        String[] parts = command.trim().split("\\s+");
        return execute(Arrays.asList(parts), workingDir);
    }

    public ProcessResult execute(List<String> commandParts, File workingDir) throws IOException {
        return execute(commandParts, workingDir, DEFAULT_TIMEOUT_SECONDS);
    }

    public ProcessResult execute(List<String> commandParts, File workingDir, int timeoutSeconds) throws IOException {
        if (commandParts == null || commandParts.isEmpty()) {
            throw new IOException("Command must not be empty.");
        }

        if (commandParts.get(0) == null || commandParts.get(0).trim().isEmpty()) {
            throw new IOException("Executable command must not be empty.");
        }

        if (workingDir == null || !workingDir.exists() || !workingDir.isDirectory()) {
            throw new IOException(
                    "Working directory not found: "
                            + (workingDir == null ? "null" : workingDir.getAbsolutePath())
            );
        }

        long startTime = System.currentTimeMillis();

        ProcessBuilder builder = new ProcessBuilder(commandParts);
        builder.directory(workingDir);
        builder.redirectErrorStream(false);

        Process process = builder.start();

        StringBuilder stdout = new StringBuilder();
        StringBuilder stderr = new StringBuilder();

        Thread outThread = new Thread(() -> readStream(process.getInputStream(), stdout));
        Thread errThread = new Thread(() -> readStream(process.getErrorStream(), stderr));

        outThread.start();
        errThread.start();

        boolean finished = false;
        try {
            finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean timedOut = !finished;

        if (timedOut) {
            process.destroyForcibly();

            try {
                process.waitFor(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        try {
            outThread.join(1000);
            errThread.join(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int exitCode = timedOut ? -1 : process.exitValue();
        long duration = System.currentTimeMillis() - startTime;

        return new ProcessResult(exitCode, stdout.toString(), stderr.toString(), timedOut, duration);
    }

    private void readStream(InputStream is, StringBuilder buffer) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line).append("\n");
            }
        } catch (IOException ignored) {}
    }
}
