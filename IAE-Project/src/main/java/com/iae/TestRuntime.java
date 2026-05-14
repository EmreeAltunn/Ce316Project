package com.iae;

import com.iae.db.DatabaseManager;
import com.iae.model.*;
import com.iae.service.*;

import java.io.File;
import java.time.LocalDateTime;

public class TestRuntime {
    public static void main(String[] args) {
        try {
            File dbFile = new File("test_iae.db");
            if (dbFile.exists()) dbFile.delete();

            DatabaseManager dbManager = DatabaseManager.getInstance();
            dbManager.initialize("."); // will create iae.db if DB_FILE_NAME is hardcoded, wait let me check

            System.out.println("Initialized DB");
            
            ConfigurationService cs = new ConfigurationService(dbManager);
            ProjectService ps = new ProjectService(dbManager);
            TestCaseService ts = new TestCaseService(dbManager);
            StudentResultService srs = new StudentResultService(dbManager);

            // Configuration
            Configuration c = new Configuration();
            c.setName("Test Config");
            c.setCompilerPath("javac");
            c.setCompilerArgs("");
            c.setRunCommand("java");
            c.setRequiresCompilation(true);
            c.setSourceFileName("Main.java");
            c.setFileExtension(".java");
            cs.save(c);
            System.out.println("Saved config ID: " + c.getId());

            Configuration fetchedC = cs.findById(c.getId()).orElse(null);
            System.out.println("Fetched config name: " + (fetchedC != null ? fetchedC.getName() : "null"));

            // Project
            Project p = new Project();
            p.setName("Test Proj");
            p.setConfigurationId(c.getId());
            p.setSubmissionsDirectory("subs/");
            p.setWorkingDirectory("work/");
            ps.save(p);
            System.out.println("Saved proj ID: " + p.getId());

            // TestCase
            TestCase tc = new TestCase();
            tc.setProjectId(p.getId());
            tc.setName("TC1");
            tc.setInputArgs("hello");
            tc.setExpectedOutputFile("expected.txt");
            ts.save(tc); // using TestCaseService
            System.out.println("Saved TestCase ID: " + tc.getId());
            
            // StudentResult
            StudentResult sr = new StudentResult();
            sr.setProjectId(p.getId());
            sr.setStudentId("123");
            sr.setZipFilePath("test.zip");
            sr.setCompileStatus(ResultStatus.SUCCESS);
            srs.save(sr); // using StudentResultService
            System.out.println("Saved StudentResult ID: " + sr.getId());
            
            System.out.println("TestCases for proj: " + ts.findByProjectId(p.getId()).size());
            System.out.println("Results for proj: " + srs.findByProjectId(p.getId()).size());

            dbManager.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
