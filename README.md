# 🚀 TaskManager & AI Test Case Management System

A full-stack, enterprise-ready Task and QA Test Case Management platform powered by **Spring Boot**, **Angular**, **PostgreSQL**, and **Google Gemini AI**.

---

## 📌 Table of Contents

- [Overview](#-overview)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Architecture & Project Structure](#-architecture--project-structure)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
  - [1. Database Configuration](#1-database-configuration)
  - [2. Backend Setup (Spring Boot)](#2-backend-setup-spring-boot)
  - [3. Frontend Setup (Angular)](#3-frontend-setup-angular)
- [REST API Reference](#-rest-api-reference)
  - [Tasks API (`/api/tasks`)](#tasks-api-apitasks)
  - [Test Cases API (`/api/testcases`)](#test-cases-api-apitestcases)
- [Testing & Quality Assurance](#-testing--quality-assurance)
  - [Backend Tests](#backend-tests)
  - [Frontend Tests](#frontend-tests)
  - [End-to-End (E2E) Testing](#end-to-end-e2e-testing)
  - [API Testing (Postman)](#api-testing-postman)
  - [Performance & Load Testing (JMeter)](#performance--load-testing-jmeter)
- [Environment Variables & Configuration](#-environment-variables--configuration)

---

## 📖 Overview

The **TaskManager** application streamlines project delivery by combining agile task tracking with automated QA workflows. It enables teams to create, prioritize, and track tasks while leveraging generative AI (Google Gemini) to instantly generate comprehensive test cases and scenarios directly from task requirements.

---

## ✨ Key Features

- 📋 **Task Management**:
  - Full CRUD operations with title, description, priority, due date, and status.
  - Priority levels: `LOW`, `MEDIUM`, `HIGH`, `URGENT`.
  - Status lifecycle: `TODO`, `IN_PROGRESS`, `COMPLETED`.

- 🤖 **AI-Powered Test Case Generation**:
  - Automatically generate test cases with steps, expected results, and test types using Google Gemini AI.
  - Support for custom prompts and edge case expansion.

- 🧪 **Test Case Lifecycle & Execution**:
  - Test case review workflow (`DRAFT`, `APPROVED`, `REJECTED`).
  - Execution recording with results (`PASSED`, `FAILED`, `BLOCKED`, `SKIPPED`) and execution notes.
  - Categorization by test types (`FUNCTIONAL`, `INTEGRATION`, `SECURITY`, `PERFORMANCE`, `UI`).

- 📊 **Interactive Dashboard & Analytics**:
  - Real-time task status tracking and priority distribution charts powered by **Chart.js** and **PrimeNG**.

- 🛡️ **Comprehensive Quality Assurance**:
  - Pre-configured Playwright E2E suites, Postman API collections, and Apache JMeter performance test plans.

---

## 🛠️ Tech Stack

### Backend
- **Java 21**
- **Spring Boot 4.x** (Web MVC, Data JPA, Validation)
- **PostgreSQL** (Relational Database)
- **Hibernate / JPA** (ORM)
- **Google Gemini API** (AI Test Case Generation)
- **Lombok** & **Jackson**

### Frontend
- **Angular 22** (Standalone Components, Signals, SSR Support)
- **PrimeNG 22** & **PrimeIcons** (UI Component Library)
- **Chart.js** (Data Visualizations)
- **TypeScript & RxJS**

### Testing & Tooling
- **JUnit 5** & **Mockito** (Backend Unit & Integration Tests)
- **Vitest** / **Jasmine** & **Karma** (Frontend Unit Tests)
- **Playwright** (End-to-End UI Testing)
- **Postman / Newman** (API Testing)
- **Apache JMeter** (Performance & Load Testing)
- **Maven** & **npm**

---

## 📁 Architecture & Project Structure

```text
taskmanager/
├── api-tests/                     # Postman collections & API tests
│   └── TaskManager_Postman_Collection.json
├── e2e/                           # Playwright end-to-end test suites
│   └── task-lifecycle.spec.ts
├── frontend/                      # Angular 22 frontend application
│   ├── src/
│   │   ├── app/
│   │   │   ├── components/        # Dashboard, Task List, Task Form, Task Details
│   │   │   ├── models/            # TypeScript interfaces & types
│   │   │   ├── services/          # Angular HTTP client services
│   │   │   ├── app.routes.ts      # Routing configuration
│   │   │   └── app.ts             # Root application component
│   │   └── index.html
│   └── package.json
├── performance/                   # JMeter load testing plans & reports
│   ├── TaskManagement_LoadTest.jmx
│   └── results.jtl
├── src/                           # Spring Boot backend source code
│   ├── main/
│   │   ├── java/com/example/taskmanager/
│   │   │   ├── controller/        # REST API Controllers (Task, TestCase)
│   │   │   ├── dto/               # Data Transfer Objects & Request Models
│   │   │   ├── entity/            # JPA Entities & Enums
│   │   │   ├── exception/         # Global Exception Handlers
│   │   │   ├── repository/        # Spring Data JPA Repositories
│   │   │   └── service/           # Business Logic & AI Integration Services
│   │   └── resources/
│   │       └── application.properties
│   └── test/                      # Backend unit & integration test suites
├── pom.xml                        # Maven configuration
└── README.md                      # Project documentation
```

---

## ⚙️ Prerequisites

Ensure you have the following installed on your system:

- [Java Development Kit (JDK) 21+](https://adoptium.net/)
- [Node.js 20+](https://nodejs.org/) & [npm](https://www.npmjs.com/)
- [PostgreSQL](https://www.postgresql.org/) (version 14 or later)
- [Git](https://git-scm.com/)

---

## 🚀 Getting Started

### 1. Database Configuration

Create a PostgreSQL database named `taskmanagement_db`:

```sql
CREATE DATABASE taskmanagement_db;
```

Update your database credentials in `src/main/resources/application.properties` if they differ from the defaults:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/taskmanagement_db
spring.datasource.username=postgres
spring.datasource.password=your_password
```

### 2. Backend Setup (Spring Boot)

1. Set your Gemini API key (optional for basic tasks, required for AI generation):
   ```bash
   # Windows PowerShell
   $env:GEMINI_API_KEY="your_actual_gemini_api_key"

   # Linux / macOS
   export GEMINI_API_KEY="your_actual_gemini_api_key"
   ```

2. Build and run the Spring Boot application:
   ```bash
   # Using Maven wrapper (Windows)
   .\mvnw.cmd spring-boot:run

   # Using Maven wrapper (Linux / macOS)
   ./mvnw spring-boot:run
   ```

The backend server will start at: `http://localhost:8080`

### 3. Frontend Setup (Angular)

1. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Launch the development server:
   ```bash
   npm start
   ```

4. Open your browser and navigate to: `http://localhost:4200`

---

## 🔌 REST API Reference

### Tasks API (`/api/tasks`)

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/tasks` | Retrieve all tasks |
| `GET` | `/api/tasks/{id}` | Retrieve a single task by ID |
| `POST` | `/api/tasks` | Create a new task |
| `PUT` | `/api/tasks/{id}` | Update an existing task |
| `PATCH` | `/api/tasks/{id}/status` | Update task status (`TODO`, `IN_PROGRESS`, `COMPLETED`) |
| `DELETE` | `/api/tasks/{id}` | Delete a task |

### Test Cases API (`/api/testcases`)

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/testcases/generate` | Generate test cases using Gemini AI for a task ID |
| `GET` | `/api/testcases/task/{taskId}` | Retrieve all test cases associated with a task |
| `PATCH` | `/api/testcases/{id}/review` | Update test case review status (`DRAFT`, `APPROVED`, `REJECTED`) |
| `POST` | `/api/testcases/{id}/execute` | Execute a test case (`PASSED`, `FAILED`, `BLOCKED`, `SKIPPED`) |
| `DELETE` | `/api/testcases/{id}` | Delete a test case |

---

## 🧪 Testing & Quality Assurance

### Backend Tests
Run Spring Boot unit and integration tests:
```bash
./mvnw test
```

### Frontend Tests
Run Angular unit tests with Vitest:
```bash
cd frontend
npm test
```

### End-to-End (E2E) Testing
Run end-to-end browser tests using Playwright:
```bash
npx playwright test
```

### API Testing (Postman)
Execute the Postman test collection via Newman or import `api-tests/TaskManager_Postman_Collection.json` into Postman:
```bash
npx newman run api-tests/TaskManager_Postman_Collection.json
```

### Performance & Load Testing (JMeter)
Run the load test plan in non-GUI mode:
```bash
jmeter -n -t performance/TaskManagement_LoadTest.jmx -l performance/results.jtl -e -o performance/report/
```

---

## 🔐 Environment Variables & Configuration

| Variable | Description | Default |
|---|---|---|
| `GEMINI_API_KEY` | Google Gemini API Key for AI test case generation | `your_gemini_api_key` |
| `spring.datasource.url` | PostgreSQL JDBC Connection URL | `jdbc:postgresql://localhost:5433/taskmanagement_db` |
| `spring.datasource.username` | Database Username | `postgres` |
| `spring.datasource.password` | Database Password | `1234` |
