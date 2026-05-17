package com.iae.process;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipProcessor {

    public List<File> extractAll(File zipDirectory, File targetDirectory) throws IOException {
        List<File> extractedDirs = new ArrayList<>();

        if (zipDirectory == null || !zipDirectory.exists() || !zipDirectory.isDirectory()) {
            throw new IOException(
                    "ZIP directory not found: "
                            + (zipDirectory == null ? "null" : zipDirectory.getAbsolutePath())
            );
        }

        File[] zipFiles = zipDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip"));

        if (zipFiles == null || zipFiles.length == 0) {
            System.out.println("Warning: no .zip files found in " + zipDirectory.getAbsolutePath());
            return extractedDirs;
        }

        for (File zipFile : zipFiles) {
            try {
                File studentDir = extractSingle(zipFile, targetDirectory);
                extractedDirs.add(studentDir);
                System.out.println("Extracted ZIP: " + zipFile.getName());
            } catch (Exception e) {
                System.err.println("Could not extract ZIP " + zipFile.getName() + ": " + e.getMessage());
            }
        }

        return extractedDirs;
    }

    public File extractSingle(File zipFile, File targetDirectory) throws IOException {
        validateInputs(zipFile, targetDirectory);

        String studentId = getStudentIdFromZip(zipFile);
        File studentDir = new File(targetDirectory, studentId);
        String targetRootPath = targetDirectory.getCanonicalPath();
        String studentDirPath = studentDir.getCanonicalPath();

        if (studentDirPath.equals(targetRootPath) || !isInsideDirectory(targetRootPath, studentDirPath)) {
            throw new IOException("Student extraction directory escapes working directory: " + studentId);
        }

        createDirectory(studentDir, "student extraction directory");
        clearDirectory(studentDir);

        String studentRootPath = studentDirPath;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                File outFile = resolveZipEntry(studentDir, studentRootPath, entry);

                if (entry.isDirectory() || isDirectoryName(entry.getName())) {
                    createDirectory(outFile, "ZIP entry directory");
                } else {
                    File parent = outFile.getParentFile();
                    createDirectory(parent, "ZIP entry parent directory");

                    if (outFile.exists() && outFile.isDirectory()) {
                        throw new IOException("ZIP entry conflicts with an existing directory: " + entry.getName());
                    }

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        zis.transferTo(fos);
                    }
                }

                zis.closeEntry();
            }
        }

        return studentDir;
    }

    public String getStudentIdFromZip(File zipFile) {
        String name = zipFile.getName();
        if (name.toLowerCase().endsWith(".zip")) {
            name = name.substring(0, name.length() - 4);
        }
        return name;
    }

    private void validateInputs(File zipFile, File targetDirectory) throws IOException {
        if (zipFile == null) {
            throw new IOException("ZIP file must not be null.");
        }

        if (!zipFile.exists() || !zipFile.isFile()) {
            throw new IOException("ZIP file not found: " + zipFile.getAbsolutePath());
        }

        if (targetDirectory == null) {
            throw new IOException("Target extraction directory must not be null.");
        }

        createDirectory(targetDirectory, "target extraction directory");
    }

    private File resolveZipEntry(File studentDir,
                                 String studentRootPath,
                                 ZipEntry entry) throws IOException {

        String entryName = entry.getName();

        if (entryName == null || entryName.trim().isEmpty()) {
            throw new IOException("ZIP contains an empty entry name.");
        }

        if (entryName.indexOf('\0') >= 0) {
            throw new IOException("ZIP entry contains a null character: " + entryName);
        }

        String normalizedEntryName = entryName.replace('\\', '/');

        if (hasParentTraversal(normalizedEntryName)) {
            throw new IOException("ZIP entry contains path traversal: " + entryName);
        }

        if (isAbsoluteEntryName(normalizedEntryName)) {
            throw new IOException("ZIP entry uses an absolute path: " + entryName);
        }

        File outFile = new File(studentDir, normalizedEntryName);
        String outPath = outFile.getCanonicalPath();

        if (!isInsideDirectory(studentRootPath, outPath)) {
            throw new IOException("ZIP entry escapes extraction directory: " + entryName);
        }

        return outFile;
    }

    private boolean isAbsoluteEntryName(String entryName) {
        return entryName.startsWith("/")
                || entryName.startsWith("//")
                || entryName.matches("^[A-Za-z]:.*");
    }

    private boolean hasParentTraversal(String entryName) {
        String[] parts = entryName.split("/");
        for (String part : parts) {
            if ("..".equals(part)) {
                return true;
            }
        }
        return false;
    }

    private boolean isInsideDirectory(String rootPath, String childPath) {
        return childPath.equals(rootPath)
                || childPath.startsWith(rootPath + File.separator);
    }

    private boolean isDirectoryName(String entryName) {
        return entryName != null
                && (entryName.endsWith("/") || entryName.endsWith("\\"));
    }

    private void createDirectory(File directory, String description) throws IOException {
        if (directory == null) {
            throw new IOException("Could not create " + description + ": path is null.");
        }

        if (directory.exists()) {
            if (!directory.isDirectory()) {
                throw new IOException(
                        "Could not create " + description + " because a file already exists: "
                                + directory.getAbsolutePath()
                );
            }
            return;
        }

        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException(
                    "Could not create " + description + ": " + directory.getAbsolutePath()
            );
        }
    }

    private void clearDirectory(File directory) throws IOException {
        if (directory == null || !directory.exists()) {
            return;
        }

        Path root = directory.toPath();

        try (var paths = Files.walk(root)) {
            paths
                    .sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }
    }
}
