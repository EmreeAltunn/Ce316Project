package com.iae.runner;

import com.iae.model.ComparisonResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutputComparatorTest {

    @TempDir
    Path tempDir;

    private final OutputComparator comparator = new OutputComparator();

    @Test
    void comparePassesWhenNormalizedOutputMatches() {
        ComparisonResult result = comparator.compare("hello\r\nworld\n", "hello\nworld");

        assertTrue(result.isMatch());
        assertTrue(result.getDifferences().isEmpty());
    }

    @Test
    void compareFailsWhenOutputDiffers() {
        ComparisonResult result = comparator.compare("hello\nstudent", "hello\nexpected");

        assertFalse(result.isMatch());
        assertFalse(result.getDifferences().isEmpty());
    }

    @Test
    void compareKeepsInternalSpacesSignificantButIgnoresOuterWhitespace() {
        assertTrue(comparator.compare("  value\n", "value").isMatch());
        assertFalse(comparator.compare("value  here", "value here").isMatch());
        assertFalse(comparator.compare("value \nnext", "value\nnext").isMatch());
    }

    @Test
    void compareWithFileUsesExpectedOutputFile() throws IOException {
        Path expected = tempDir.resolve("expected.txt");
        Files.writeString(expected, "42\n", StandardCharsets.UTF_8);

        ComparisonResult result = comparator.compareWithFile("42", expected.toFile());

        assertTrue(result.isMatch());
    }
}
