package com.iae.runner;

import com.iae.model.StudentResult;

import java.util.List;

public interface RunnerCallback {
    void onStudentStarted(String studentId);
    void onStudentCompleted(StudentResult result);
    void onStudentError(String studentId, String errorMessage);
    void onAllCompleted(List<StudentResult> results);
    void onProgress(int current, int total);
}