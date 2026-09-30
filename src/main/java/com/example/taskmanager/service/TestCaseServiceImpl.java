package com.example.taskmanager.service;

import com.example.taskmanager.dto.ExecuteTestCaseRequest;
import com.example.taskmanager.entity.*;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.repository.TaskRepository;
import com.example.taskmanager.repository.TestCaseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class TestCaseServiceImpl implements TestCaseService {

    private final TestCaseRepository testCaseRepository;
    private final TaskRepository taskRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${gemini.api.key:}")
    private String geminiApiKey;

    public TestCaseServiceImpl(TestCaseRepository testCaseRepository, TaskRepository taskRepository) {
        this.testCaseRepository = testCaseRepository;
        this.taskRepository = taskRepository;
    }

    @Override
    public List<TestCase> getTestCasesByTaskId(Long taskId) {
        if (!taskRepository.existsById(taskId)) {
            throw new TaskNotFoundException("Task not found with ID: " + taskId);
        }
        return testCaseRepository.findByTaskId(taskId);
    }

    @Override
    public List<TestCase> generateTestCases(Long taskId, String customPrompt) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException("Task not found with ID: " + taskId));

        System.out.println(">>> Starting Test Case generation for Task ID: " + taskId + " | Title: " + task.getTitle());

        if (geminiApiKey != null && !geminiApiKey.isBlank() && !geminiApiKey.contains("YOUR_GEMINI_API_KEY")) {
            try {
                System.out.println(">>> Calling Gemini API with key prefix: " + geminiApiKey.substring(0, Math.min(6, geminiApiKey.length())) + "...");
                List<TestCase> aiCases = callGeminiForTestCases(task, customPrompt);
                if (aiCases != null && !aiCases.isEmpty()) {
                    System.out.println(">>> Successfully generated " + aiCases.size() + " test cases from Gemini LLM!");
                    return testCaseRepository.saveAll(aiCases);
                }
            } catch (Exception e) {
                System.err.println(">>> [ERROR] Gemini API failed with exception: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println(">>> No valid gemini.api.key found in application.properties. Using local rule generator.");
        }

        System.out.println(">>> Generating fallback test cases.");
        return testCaseRepository.saveAll(generateFallbackTestCases(task));
    }

    private List<TestCase> callGeminiForTestCases(Task task, String customPrompt) throws Exception {
        String prompt = String.format(
            "You are a Senior QA Automation Engineer. Based on the following Task requirements, generate comprehensive and creative test cases specifically tailored to this task.\n" +
            "Task Title: %s\n" +
            "Description: %s\n" +
            "Priority: %s\n" +
            "Assignee: %s\n" +
            "Due Date: %s\n" +
            "Custom Instruction: %s\n\n" +
            "Generate at least 5 distinct test cases covering these exact types: POSITIVE, NEGATIVE, VALIDATION, BOUNDARY, BUSINESS_RULE.\n" +
            "Return ONLY raw JSON strictly in this structure without markdown fences:\n" +
            "{\n" +
            "  \"testCases\": [\n" +
            "    {\n" +
            "      \"title\": \"TC-001: Short summary\",\n" +
            "      \"type\": \"POSITIVE\",\n" +
            "      \"preconditions\": \"Initial system setup\",\n" +
            "      \"steps\": \"1. Step one\\n2. Step two\",\n" +
            "      \"expectedResult\": \"Expected behavior description\"\n" +
            "    }\n" +
            "  ]\n" +
            "}",
            task.getTitle(),
            task.getDescription() != null ? task.getDescription() : "None",
            task.getPriority(),
            task.getAssignee() != null ? task.getAssignee() : "Unassigned",
            task.getDueDate() != null ? task.getDueDate().toString() : "None",
            customPrompt != null ? customPrompt : "None"
        );

        // Uses the current stable Gemini endpoint
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=" + geminiApiKey;

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> requestBody = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        System.out.println(">>> Gemini HTTP Status: " + response.getStatusCode());

        JsonNode root = objectMapper.readTree(response.getBody());
        String textResponse = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
        System.out.println(">>> Raw LLM Response:\n" + textResponse);

        // Sanitize code blocks
        textResponse = textResponse.trim();
        if (textResponse.startsWith("```json")) {
            textResponse = textResponse.substring(7);
        } else if (textResponse.startsWith("```")) {
            textResponse = textResponse.substring(3);
        }
        if (textResponse.endsWith("```")) {
            textResponse = textResponse.substring(0, textResponse.length() - 3);
        }
        textResponse = textResponse.trim();

        JsonNode parsedCases = objectMapper.readTree(textResponse).path("testCases");
        List<TestCase> list = new ArrayList<>();

        if (parsedCases.isArray()) {
            for (JsonNode node : parsedCases) {
                TestCase tc = new TestCase();
                tc.setTask(task);
                tc.setTitle(node.path("title").asText("Generated Test Case"));
                tc.setDescription(node.path("expectedResult").asText());
                tc.setPreconditions(node.path("preconditions").asText("System is operational"));
                tc.setSteps(node.path("steps").asText());
                tc.setExpectedResult(node.path("expectedResult").asText());

                String typeStr = node.path("type").asText("POSITIVE").toUpperCase();
                try {
                    tc.setTestType(TestType.valueOf(typeStr));
                } catch (IllegalArgumentException e) {
                    tc.setTestType(TestType.POSITIVE);
                }

                tc.setStatus(TestCaseStatus.DRAFT);
                tc.setExecutionResult(ExecutionResult.NOT_RUN);
                list.add(tc);
            }
        }
        return list;
    }

    private List<TestCase> generateFallbackTestCases(Task task) {
        List<TestCase> generatedCases = new ArrayList<>();

        TestCase tc1 = new TestCase();
        tc1.setTask(task);
        tc1.setTitle("TC-001: Create task '" + task.getTitle() + "' with valid details");
        tc1.setDescription("Verify successful task creation when all valid details are supplied for " + task.getTitle());
        tc1.setPreconditions("Open task creation page");
        tc1.setSteps("1. Enter valid title\n2. Select priority " + task.getPriority() + "\n3. Select assignee\n4. Select future due date\n5. Click Save");
        tc1.setExpectedResult("Task should be created successfully");
        tc1.setTestType(TestType.POSITIVE);
        tc1.setStatus(TestCaseStatus.DRAFT);
        tc1.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc1);

        TestCase tc2 = new TestCase();
        tc2.setTask(task);
        tc2.setTitle("TC-002: Create task without title");
        tc2.setDescription("Verify validation error when title is empty");
        tc2.setPreconditions("Open task creation page");
        tc2.setSteps("1. Leave title empty\n2. Fill remaining valid fields\n3. Click Save");
        tc2.setExpectedResult("Validation error: Title is mandatory");
        tc2.setTestType(TestType.VALIDATION);
        tc2.setStatus(TestCaseStatus.DRAFT);
        tc2.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc2);

        TestCase tc3 = new TestCase();
        tc3.setTask(task);
        tc3.setTitle("TC-003: Create task without priority");
        tc3.setDescription("Verify validation error when priority is omitted");
        tc3.setPreconditions("Open task creation page");
        tc3.setSteps("1. Enter valid title\n2. Do not select priority\n3. Click Save");
        tc3.setExpectedResult("Validation error: Priority is mandatory");
        tc3.setTestType(TestType.VALIDATION);
        tc3.setStatus(TestCaseStatus.DRAFT);
        tc3.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc3);

        TestCase tc4 = new TestCase();
        tc4.setTask(task);
        tc4.setTitle("TC-004: Create task without assignee");
        tc4.setDescription("Verify task is created successfully even when assignee is left unassigned");
        tc4.setPreconditions("Open task creation page");
        tc4.setSteps("1. Enter title and priority\n2. Leave assignee empty\n3. Click Save");
        tc4.setExpectedResult("Task is created successfully with unassigned state");
        tc4.setTestType(TestType.POSITIVE);
        tc4.setStatus(TestCaseStatus.DRAFT);
        tc4.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc4);

        TestCase tc5 = new TestCase();
        tc5.setTask(task);
        tc5.setTitle("TC-005: Create task with past due date");
        tc5.setDescription("Verify rejection when due date is set in the past");
        tc5.setPreconditions("Open task creation page");
        tc5.setSteps("1. Enter valid title and priority\n2. Select a past calendar date\n3. Click Save");
        tc5.setExpectedResult("Validation error: Due date cannot be in the past");
        tc5.setTestType(TestType.NEGATIVE);
        tc5.setStatus(TestCaseStatus.DRAFT);
        tc5.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc5);

        TestCase tc6 = new TestCase();
        tc6.setTask(task);
        tc6.setTitle("TC-006: Create task with today's due date");
        tc6.setDescription("Verify boundary validation when due date equals current date");
        tc6.setPreconditions("Open task creation page");
        tc6.setSteps("1. Fill valid title\n2. Set due date to today\n3. Click Save");
        tc6.setExpectedResult("Task should be accepted without errors");
        tc6.setTestType(TestType.BOUNDARY);
        tc6.setStatus(TestCaseStatus.DRAFT);
        tc6.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc6);

        TestCase tc7 = new TestCase();
        tc7.setTask(task);
        tc7.setTitle("TC-007: Create task with maximum title length");
        tc7.setDescription("Verify boundary constraint on title character capacity");
        tc7.setPreconditions("Open task creation page");
        tc7.setSteps("1. Enter 100-character string into title\n2. Click Save");
        tc7.setExpectedResult("Task is saved successfully without truncation");
        tc7.setTestType(TestType.BOUNDARY);
        tc7.setStatus(TestCaseStatus.DRAFT);
        tc7.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc7);

        TestCase tc8 = new TestCase();
        tc8.setTask(task);
        tc8.setTitle("TC-008: Verify invalid status transition TODO to COMPLETED");
        tc8.setDescription("Verify business rule blocking direct completion without IN_PROGRESS");
        tc8.setPreconditions("Task in TODO status exists");
        tc8.setSteps("1. Attempt direct status update to COMPLETED\n2. Check response");
        tc8.setExpectedResult("HTTP 400 Bad Request: Task can only be marked COMPLETED when IN_PROGRESS");
        tc8.setTestType(TestType.BUSINESS_RULE);
        tc8.setStatus(TestCaseStatus.DRAFT);
        tc8.setExecutionResult(ExecutionResult.NOT_RUN);
        generatedCases.add(tc8);

        return generatedCases;
    }

    @Override
    public TestCase reviewTestCase(Long testCaseId, TestCaseStatus newStatus) {
        TestCase tc = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test Case not found with ID: " + testCaseId));
        tc.setStatus(newStatus);
        return testCaseRepository.save(tc);
    }

    @Override
    public TestCase executeTestCase(Long testCaseId, ExecuteTestCaseRequest request) {
        TestCase tc = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new RuntimeException("Test Case not found with ID: " + testCaseId));

        if (tc.getStatus() != TestCaseStatus.APPROVED) {
            throw new IllegalStateException("Only APPROVED test cases can be executed. Current status: " + tc.getStatus());
        }

        tc.setExecutionResult(request.getResult());
        tc.setActualResult(request.getActualResult());
        tc.setExecutionNotes(request.getExecutionNotes());
        tc.setExecutedAt(LocalDateTime.now());
        return testCaseRepository.save(tc);
    }

    @Override
    public void deleteTestCase(Long id) {
        if (!testCaseRepository.existsById(id)) {
            throw new RuntimeException("Test Case not found with ID: " + id);
        }
        testCaseRepository.deleteById(id);
    }
}