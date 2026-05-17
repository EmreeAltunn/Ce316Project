package com.iae;

import com.iae.db.DatabaseManager;
import com.iae.model.Configuration;
import com.iae.model.Project;
import com.iae.model.TestCase;
import com.iae.service.ConfigurationService;
import com.iae.service.ProjectService;
import com.iae.service.TestCaseService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Veritabani + servis katmani uctan uca entegrasyon testleri.
 *
 * <p>Gecici bir dizinde SQLite veritabani olusturur ve CRUD islemlerini dogrular.
 */
class IntegrationTest {

    @TempDir
    static Path tempDir;

    private static DatabaseManager dbManager;
    private static ConfigurationService configurationService;
    private static ProjectService projectService;
    private static TestCaseService testCaseService;

    @BeforeAll
    static void setUp() throws SQLException {
        dbManager = DatabaseManager.getInstance();
        dbManager.initialize(tempDir.toString());

        configurationService = new ConfigurationService(dbManager);
        projectService = new ProjectService(dbManager);
        testCaseService = new TestCaseService(dbManager);
    }

    private Configuration newConfiguration(String name) {
        Configuration config = new Configuration();
        config.setName(name);
        config.setCompilerPath("gcc");
        config.setCompilerArgs("{source} -o {output}");
        config.setRunCommand("./{executable} {args}");
        config.setRequiresCompilation(true);
        config.setSourceFileName("main.c");
        config.setOutputFileName("main");
        config.setFileExtension(".c");
        config.setDescription("Integration test configuration");
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        return config;
    }

    @Test
    void configurationCanBeSavedAndRetrieved() throws SQLException {
        Configuration saved = configurationService.save(newConfiguration("IT-Config"));
        assertTrue(saved.getId() > 0, "Saved configuration should get a generated id");

        Optional<Configuration> found = configurationService.findById(saved.getId());
        assertTrue(found.isPresent(), "Configuration should be retrievable by id");
        assertEquals("IT-Config", found.get().getName());

        Optional<Configuration> byName = configurationService.findByName("IT-Config");
        assertTrue(byName.isPresent(), "Configuration should be retrievable by name");
    }

    @Test
    void projectCanBeSavedAndRetrieved() throws SQLException {
        Configuration config = configurationService.save(newConfiguration("IT-Config-Project"));

        Project project = new Project();
        project.setName("Integration Project");
        project.setDescription("Created by IntegrationTest");
        project.setConfigurationId(config.getId());
        project.setSubmissionsDirectory("/tmp/submissions");
        project.setWorkingDirectory("/tmp/workspace");
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        Project saved = projectService.save(project);
        assertTrue(saved.getId() > 0, "Saved project should get a generated id");

        Optional<Project> found = projectService.findById(saved.getId());
        assertTrue(found.isPresent(), "Project should be retrievable by id");
        assertEquals("Integration Project", found.get().getName());
        assertEquals(config.getId(), found.get().getConfigurationId());
    }

    @Test
    void testCaseCanBeSavedAndDeleted() throws SQLException {
        Configuration config = configurationService.save(newConfiguration("IT-Config-TC"));

        Project project = new Project();
        project.setName("TestCase Owner Project");
        project.setConfigurationId(config.getId());
        project.setSubmissionsDirectory("/tmp/s");
        project.setWorkingDirectory("/tmp/w");
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());
        Project savedProject = projectService.save(project);

        TestCase testCase = new TestCase();
        testCase.setProjectId(savedProject.getId());
        testCase.setName("Test 1");
        testCase.setInputArgs("5 4 3 2 1");
        testCase.setExpectedOutputFile("expected.txt");
        testCase.setOrderIndex(0);
        TestCase savedTestCase = testCaseService.save(testCase);
        assertTrue(savedTestCase.getId() > 0, "Saved test case should get a generated id");

        List<TestCase> afterSave = testCaseService.findByProjectId(savedProject.getId());
        assertEquals(1, afterSave.size(), "Project should have exactly one test case");

        testCaseService.delete(savedTestCase.getId());
        List<TestCase> afterDelete = testCaseService.findByProjectId(savedProject.getId());
        assertTrue(afterDelete.isEmpty(), "Test case should be removed after delete");
    }
}
