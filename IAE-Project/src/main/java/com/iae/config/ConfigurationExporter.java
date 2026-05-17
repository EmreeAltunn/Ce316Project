package com.iae.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.iae.model.Configuration;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Exports Configuration objects to .iaeconfig JSON files.
 * Used for backup and sharing configurations between installations.
 */
public class ConfigurationExporter {

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Exports a single configuration to the given target file.
     * The file will be written in JSON format with .iaeconfig extension.
     *
     * @param config     the configuration to export
     * @param targetFile the destination file (should end with .iaeconfig)
     * @throws IOException if the file cannot be written
     */
    public void exportToFile(Configuration config, File targetFile) throws IOException {
        JsonObject json = buildJsonObject(config);
        try (Writer writer = new FileWriter(targetFile)) {
            gson.toJson(json, writer);
        }
    }

    /**
     * Exports all given configurations to separate files in the target directory.
     * Each file is named after the configuration, with unsafe characters replaced by underscores.
     *
     * @param configs   list of configurations to export
     * @param targetDir the destination directory (will be created if absent)
     * @throws IOException if any file cannot be written
     */
    public void exportAllToDirectory(List<Configuration> configs, File targetDir) throws IOException {
        if (!targetDir.exists()) {
            targetDir.mkdirs();
        }
        for (Configuration config : configs) {
            String safeName = config.getName().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            File file = new File(targetDir, safeName + ".iaeconfig");
            exportToFile(config, file);
        }
    }

    private JsonObject buildJsonObject(Configuration config) {
        JsonObject obj = new JsonObject();
        obj.addProperty("version", "1.0");
        obj.addProperty("name", config.getName());
        obj.addProperty("compilerPath", config.getCompilerPath());
        obj.addProperty("compilerArgs", config.getCompilerArgs());
        obj.addProperty("runCommand", config.getRunCommand());
        obj.addProperty("requiresCompilation", config.isRequiresCompilation());
        obj.addProperty("sourceFileName", config.getSourceFileName());
        obj.addProperty("outputFileName", config.getOutputFileName());
        obj.addProperty("fileExtension", config.getFileExtension());
        obj.addProperty("description", config.getDescription());
        return obj;
    }
}
