package com.iae.runner;

import com.iae.model.ComparisonResult;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class OutputComparator {

    public ComparisonResult compare(String actualOutput, String expectedOutput) {
        ComparisonResult result = new ComparisonResult();
        List<String> differences = new ArrayList<>();

        String actual = normalize(actualOutput);
        String expected = normalize(expectedOutput);

        String[] actualLines = splitLines(actual);
        String[] expectedLines = splitLines(expected);

        if (expectedLines.length != actualLines.length) {
            differences.add("Line count differs: expected " + expectedLines.length + ", got " + actualLines.length);
        }

        int commonLineCount = Math.min(expectedLines.length, actualLines.length);

        for (int i = 0; i < commonLineCount; i++) {
            String expectedLine = expectedLines[i];
            String actualLine = actualLines[i];

            if (!expectedLine.equals(actualLine)) {
                differences.add("Line " + (i + 1) + ": expected '" + expectedLine + "' got '" + actualLine + "'");
            }
        }

        result.setDifferences(differences);
        result.setMatch(differences.isEmpty());

        return result;
    }

    public ComparisonResult compareWithFile(String actualOutput, File expectedFile) throws IOException {
        String expectedOutput = Files.readString(expectedFile.toPath(), StandardCharsets.UTF_8);
        return compare(actualOutput, expectedOutput);
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .trim();
    }

    private String[] splitLines(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        return text.split("\n");
    }
}