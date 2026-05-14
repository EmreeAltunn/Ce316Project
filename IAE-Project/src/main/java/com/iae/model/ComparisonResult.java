package com.iae.model;

import java.util.ArrayList;
import java.util.List;

public class ComparisonResult {
    private boolean match;
    private List<String> differences = new ArrayList<>();

    public ComparisonResult() {
    }

    public boolean isMatch() { return match; }
    public void setMatch(boolean match) { this.match = match; }

    public List<String> getDifferences() { return differences; }
    public void setDifferences(List<String> differences) { this.differences = differences; }
}
