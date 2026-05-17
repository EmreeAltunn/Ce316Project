package com.iae.config;

import com.iae.model.Configuration;
import com.iae.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Validates a Configuration object and checks whether the stated compiler
 * is actually reachable on the host machine.
 */
public class ConfigurationValidator {

    /**
     * Validates all required fields of the given configuration.
     * Error messages are in English as required by the contract.
     *
     * @param config the configuration to validate
     * @return a ValidationResult indicating success or listing problems
     */
    public ValidationResult validate(Configuration config) {
        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();

        if (isBlank(config.getName())) {
            errors.add("Configuration name cannot be empty");
        }

        if (isBlank(config.getRunCommand())) {
            errors.add("Run command cannot be empty");
        }

        if (isBlank(config.getSourceFileName())) {
            errors.add("Source file name cannot be empty");
        }

        if (config.isRequiresCompilation()) {
            if (isBlank(config.getCompilerPath())) {
                errors.add("Compiler path required for compiled languages");
            }

            String args = config.getCompilerArgs();
            if (isBlank(args) || !args.contains("{source}")) {
                errors.add("Compiler args must contain {source} placeholder");
            }
        }

        result.setErrors(errors);
        result.setValid(errors.isEmpty());
        return result;
    }

    /**
     * Checks whether the compiler at the given path responds to --version.
     * Uses a 5-second timeout so the UI does not block.
     *
     * @param compilerPath path to the compiler executable (e.g. "gcc" or "C:\\MinGW\\bin\\gcc.exe")
     * @return true if the process started and finished within the timeout
     */
    public boolean isCompilerAccessible(String compilerPath) {
        if (isBlank(compilerPath)) {
            return false;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(compilerPath.trim(), "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Drain output so the process does not block on a full pipe buffer
            process.getInputStream().transferTo(java.io.OutputStream.nullOutputStream());

            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            process.destroyForcibly();
            return finished;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
