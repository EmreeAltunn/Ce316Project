package com.iae.model;

public class ProcessResult {
    private int exitCode;
    private String stdout;
    private String stderr;
    private boolean timedOut;
    private long executionTimeMs;

    public ProcessResult(int exitCode, String stdout, String stderr, boolean timedOut, long executionTimeMs) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.timedOut = timedOut;
        this.executionTimeMs = executionTimeMs;
    }

    public int getExitCode() { return exitCode; }
    public void setExitCode(int exitCode) { this.exitCode = exitCode; }

    public String getStdout() { return stdout; }
    public void setStdout(String stdout) { this.stdout = stdout; }

    public String getStderr() { return stderr; }
    public void setStderr(String stderr) { this.stderr = stderr; }

    public boolean isTimedOut() { return timedOut; }
    public void setTimedOut(boolean timedOut) { this.timedOut = timedOut; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}
