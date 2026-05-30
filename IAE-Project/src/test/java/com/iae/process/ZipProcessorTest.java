package com.iae.process;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZipProcessorTest {

    @TempDir
    Path tempDir;

    private final ZipProcessor zipProcessor = new ZipProcessor();

    @Test
    void extractAllProcessesZipFilesCaseInsensitivelyAndIgnoresZipNamedDirectories() throws IOException {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));

        createZip(submissions.resolve("alpha.zip"), "src/Main.txt", "alpha");
        createZip(submissions.resolve("UPPER.ZIP"), "src/Main.txt", "upper");
        createZip(submissions.resolve("mixed.Zip"), "src/Main.txt", "mixed");
        Files.createDirectory(submissions.resolve("ignored.zip"));
        Files.writeString(submissions.resolve("notes.txt"), "not a zip", StandardCharsets.UTF_8);

        List<File> extracted = zipProcessor.extractAll(submissions.toFile(), working.toFile());

        Set<String> studentDirs = extracted.stream()
                .map(File::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of("alpha", "UPPER", "mixed"), studentDirs);
        assertTrue(Files.notExists(working.resolve("ignored")));
    }

    @Test
    void extractSinglePreservesNestedFoldersAndSourceFiles() throws IOException {
        Path submissions = Files.createDirectory(tempDir.resolve("submissions"));
        Path working = Files.createDirectory(tempDir.resolve("working"));
        Path zip = submissions.resolve("nested.zip");

        createZip(zip, "submission/src/Main.txt", "nested source");

        File extractedDir = zipProcessor.extractSingle(zip.toFile(), working.toFile());

        Path nestedSource = extractedDir.toPath().resolve("submission").resolve("src").resolve("Main.txt");
        assertTrue(Files.isRegularFile(nestedSource));
        assertEquals("nested source", Files.readString(nestedSource, StandardCharsets.UTF_8));
    }

    private void createZip(Path zipPath, String entryName, String content) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }
}
