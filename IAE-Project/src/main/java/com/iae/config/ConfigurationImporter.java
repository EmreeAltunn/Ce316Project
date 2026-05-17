package com.iae.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iae.model.Configuration;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Imports Configuration objects from .iaeconfig JSON files.
 * The imported Configuration has id=0 and null timestamps;
 * the caller must persist it via ConfigurationService if needed.
 */
public class ConfigurationImporter {

    private final Gson gson = new Gson();

    /**
     * Imports a single configuration from the given .iaeconfig file.
     *
     * @param sourceFile the file to read (must be valid JSON)
     * @return a Configuration object populated from the file
     * @throws IOException if the file cannot be read or is malformed
     */
    public Configuration importFromFile(File sourceFile) throws IOException {
        try (FileReader reader = new FileReader(sourceFile)) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            return mapJsonToConfiguration(json);
        }
    }

    /**
     * Imports all .iaeconfig files found in the given directory.
     * Files that fail to parse are skipped and logged to stderr.
     *
     * @param sourceDir the directory to scan
     * @return list of successfully parsed configurations
     * @throws IOException if the directory does not exist or cannot be read
     */
    public List<Configuration> importAllFromDirectory(File sourceDir) throws IOException {
        List<Configuration> configs = new ArrayList<>();

        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            throw new IOException("Directory not found: " + sourceDir.getAbsolutePath());
        }

        File[] files = sourceDir.listFiles(
                (dir, name) -> name.toLowerCase().endsWith(".iaeconfig"));

        if (files == null) {
            return configs;
        }

        for (File file : files) {
            try {
                configs.add(importFromFile(file));
            } catch (Exception e) {
                System.err.println("Failed to import '" + file.getName() + "': " + e.getMessage());
            }
        }

        return configs;
    }

    private Configuration mapJsonToConfiguration(JsonObject json) {
        Configuration config = new Configuration();
        config.setName(getString(json, "name"));
        config.setCompilerPath(getString(json, "compilerPath"));
        config.setCompilerArgs(getString(json, "compilerArgs"));
        config.setRunCommand(getString(json, "runCommand"));
        config.setSourceFileName(getString(json, "sourceFileName"));
        config.setOutputFileName(getString(json, "outputFileName"));
        config.setFileExtension(getString(json, "fileExtension"));
        config.setDescription(getString(json, "description"));

        if (json.has("requiresCompilation") && !json.get("requiresCompilation").isJsonNull()) {
            config.setRequiresCompilation(json.get("requiresCompilation").getAsBoolean());
        }

        return config;
    }

    private String getString(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }
}
