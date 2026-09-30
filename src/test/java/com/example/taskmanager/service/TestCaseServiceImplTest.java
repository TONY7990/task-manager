package com.example.taskmanager.service;

import com.example.taskmanager.dto.ExecuteTestCaseRequest;
import com.example.taskmanager.entity.*;
import com.example.taskmanager.exception.BusinessRuleViolationException;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.TestCaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestCaseServiceImplTest {

    @Mock
    private TestCaseRepository testCaseRepository;

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TestCaseServiceImpl testCaseService;

    private Task sampleTask;
    private TestCase sampleTestCase;

    @BeforeEach
    void setUp() {
        sampleTask = new Task();
        sampleTask.setId(1L);
        sampleTask.setTitle("Auth Feature");

        sampleTestCase = new TestCase();
        sampleTestCase.setId(10L);
        sampleTestCase.setTitle("Verify Login");
        sampleTestCase.setTask(sampleTask);
        sampleTestCase.setStatus(TestCaseStatus.DRAFT);
        sampleTestCase.setExecutionResult(ExecutionResult.NOT_RUN);
    }

    @Test
    @DisplayName("AI Generation: Creates Positive, Negative, Boundary, and Edge test cases in DRAFT state")
    void generateTestCases_Success() {
        when(taskRepository.findById(1L)).thenReturn(Optional.of(sampleTask));
        when(testCaseRepository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<TestCase> generated = testCaseService.generateTestCases(1L, "Generate all");
        assertEquals(8, generated.size());
        assertTrue(generated.stream().allMatch(tc -> tc.getStatus() == TestCaseStatus.DRAFT));
        assertTrue(generated.stream().anyMatch(tc -> tc.getTestType() == TestType.POSITIVE));
        assertTrue(generated.stream().anyMatch(tc -> tc.getTestType() == TestType.NEGATIVE));
        assertTrue(generated.stream().anyMatch(tc -> tc.getTestType() == TestType.BOUNDARY));
        assertTrue(generated.stream().anyMatch(tc -> tc.getTestType() == TestType.VALIDATION));
    }

    @Test
    @DisplayName("Review: Transition status from DRAFT to APPROVED")
    void reviewTestCase_Approve() {
        when(testCaseRepository.findById(10L)).thenReturn(Optional.of(sampleTestCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(i -> i.getArgument(0));

        TestCase result = testCaseService.reviewTestCase(10L, TestCaseStatus.APPROVED);
        assertEquals(TestCaseStatus.APPROVED, result.getStatus());
    }

    @Test
    @DisplayName("Governance Rule: Block execution if test case is not APPROVED")
    void executeTestCase_BlockedWhenNotApproved() {
        sampleTestCase.setStatus(TestCaseStatus.DRAFT);
        when(testCaseRepository.findById(10L)).thenReturn(Optional.of(sampleTestCase));

        ExecuteTestCaseRequest req = new ExecuteTestCaseRequest();
        req.setResult(ExecutionResult.PASS);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> testCaseService.executeTestCase(10L, req)
        );
        assertTrue(ex.getMessage().contains("Only APPROVED test cases can be executed"));
    }

    @Test
    @DisplayName("Execution: Record PASS result when test case is APPROVED")
    void executeTestCase_SuccessWhenApproved() {
        sampleTestCase.setStatus(TestCaseStatus.APPROVED);
        when(testCaseRepository.findById(10L)).thenReturn(Optional.of(sampleTestCase));
        when(testCaseRepository.save(any(TestCase.class))).thenAnswer(i -> i.getArgument(0));

        ExecuteTestCaseRequest req = new ExecuteTestCaseRequest();
        req.setResult(ExecutionResult.PASS);
        req.setActualResult("Executed successfully");

        TestCase executed = testCaseService.executeTestCase(10L, req);
        assertEquals(ExecutionResult.PASS, executed.getExecutionResult());
        assertNotNull(executed.getExecutedAt());
    }
}