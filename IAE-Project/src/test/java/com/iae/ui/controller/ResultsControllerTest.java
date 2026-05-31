package com.iae.ui.controller;

import com.iae.model.ResultStatus;
import com.iae.model.StudentResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultsControllerTest {

    @Test
    void csvIncludesRunStatusAndFullCompactDetails() {
        StudentResult result = new StudentResult();
        result.setStudentId("100002");
        result.setCompileStatus(ResultStatus.SUCCESS);
        result.setRunStatus(ResultStatus.SUCCESS);
        result.setTestStatus(ResultStatus.FAIL);
        result.setTestDetails("""
                hello-emre: PASS; hello-ali: FAIL

                Failed comparisons:
                - hello-ali: expected "Hello Ali", got "Wrong Ali"
                """.trim());

        assertEquals(
                "Student ID,Compile Status,Run Status,Test Status,Details",
                ResultsController.csvHeader()
        );

        String row = ResultsController.csvRow(result);

        assertTrue(row.startsWith("100002,SUCCESS,SUCCESS,FAIL,"));
        assertTrue(row.contains("hello-emre: PASS; hello-ali: FAIL"));
        assertTrue(row.contains("Failed comparisons:"));
        assertTrue(row.contains("\"\"Hello Ali\"\""));
    }
}
