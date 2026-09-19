# SAGE TEACHING ASSISTANT — ANDROID CLIENT INTEGRATION SPECIFICATION

This document outlines the exact endpoints, request/response models, and conversational flow required to integrate an Android client with the Sage Teaching Assistant backend.

---

## 1. SERVER CONFIGURATION

- **Base URL**: http://<YOUR_ORACLE_VM_IP>:8081
- **Android Manifest Requirement**:
  Because HTTP is used over port 8081 (until an SSL domain is bound), ensure AndroidManifest.xml allows cleartext traffic:
  ```xml
  <application
      android:usesCleartextTraffic="true"
      android:networkSecurityConfig="@xml/network_security_config"
      ...>
  ```

---

## 2. WORKFLOWS SUPPORTED

The app should present 4 primary action cards/buttons:
1. **Worksheet Generator** (`worksheet-generation`): For generating classroom practice questions, MCQs, and fill-in-the-blanks.
2. **Test Paper Generator** (`test-generation`): For generating exams with blueprint sections (A/B/C), marks allocation, and answer keys.
3. **Revision Notes Generator** (`notes-generation`): For creating structured revision study notes.
4. **PDF Print** (`pdf-print`): For uploading an existing Word document (.docx) and exporting a branded Alpha Tutor PDF.

---

## 3. STEP-BY-STEP CONVERSATIONAL WORKFLOW

### STEP 1: Starting a Workflow Run
When the user picks an option and inputs the Class & Topic (e.g., "Class 9 Physics - Laws of Motion"):

- **Endpoint**: `POST /api/v1/workflows/runs`
- **Headers**: `Content-Type: application/json`
- **Request Body**:
```json
{
  "workflowKey": "worksheet-generation",
  "message": "Class 9 Physics - Laws of Motion"
}
```
*(Valid workflowKey values: "worksheet-generation", "test-generation", "notes-generation", "pdf-print")*

- **Response Body (200 OK)**:
```json
{
  "runId": "5766563a-2763-4cb4-91f9-79d73f0c8155",
  "workflowKey": "worksheet-generation",
  "status": "AWAITING_APPROVAL",
  "stageIndex": 0,
  "stageCount": 3,
  "stageKey": "worksheet-research",
  "stageName": "Worksheet Topic & Curriculum Research",
  "message": "I've researched the curriculum and prepared the proposed Worksheet Outline for: ... Is this right? Say 'yes' to proceed...",
  "output": {
    "research.confirmedOutline": "..."
  },
  "completed": false
}
```
**UI Behavior**: Display `message` to the user with two options:
1. An **"Approve / Continue"** button (sends `"yes"`).
2. A **Feedback text box** with a **"Revise"** button (sends custom feedback).

---

### STEP 2: Moving the Workflow (Approval or Revision)
Every interaction from the user after the run starts is sent through this single endpoint:

- **Endpoint**: `POST /api/v1/workflows/runs/{runId}/messages`
- **Headers**: `Content-Type: application/json`

#### Option A: Approving the current stage (advance to next stage)
```json
{
  "message": "yes"
}
```
- When approving Stage 0 (Research) -> advances to Stage 1 (creates Word .docx draft).
- When approving Stage 1 (Word Draft) -> advances to Stage 2 (exports final PDF).
- When approving Stage 2 (PDF Export) -> completes the workflow (`"completed": true`).

#### Option B: Requesting Changes / Revisions (loops back current stage)
```json
{
  "message": "Please add 4 numerical problems on momentum and force."
}
```
Sage will re-execute the stage, incorporate the feedback, update the document, and return the new draft for confirmation.

---

### STEP 3: Downloading Generated Documents

The backend saves both the intermediate Word document (.docx) and the final PDF (.pdf) for each run.

#### A. Download Word Document (.docx)
- **Endpoint**: `GET /api/v1/workflows/runs/{runId}/files/docx`
- **Method**: `GET`
- **Response**: Binary `.docx` file stream (`Content-Type: application/vnd.openxmlformats-officedocument.wordprocessingml.document`).
- **Android Action**: Save to device Downloads or open in Microsoft Word / Google Docs viewer.

#### B. Download / View Final PDF (.pdf)
- **Endpoint**: `GET /api/v1/workflows/runs/{runId}/files/pdf`
- **Method**: `GET`
- **Response**: Binary `.pdf` file stream (`Content-Type: application/pdf`).
- **Android Action**: Display in in-app PDF view or download to storage.

---

### STEP 4: PDF Print Flow (Direct Word Document Upload)

When the user selects the **PDF Print** workflow:

1. **Start the run**:
   `POST /api/v1/workflows/runs` with:
   ```json
   {
     "workflowKey": "pdf-print",
     "message": "Direct print request"
   }
   ```
   Save the returned `runId`.

2. **Upload the user's Word document**:
   - **Endpoint**: `POST /api/v1/workflows/runs/{runId}/upload`
   - **Headers**: `Content-Type: multipart/form-data`
   - **Form Field**: `file` (the `.docx` file from device storage)

   Sage will automatically apply the Alpha Tutor template, header, and margins.

3. **Download the formatted PDF**:
   - **Endpoint**: `GET /api/v1/workflows/runs/{runId}/files/pdf`

---

## 4. KOTLIN RETROFIT IMPLEMENTATION TEMPLATE

Copy this directly into your Android project:

```kotlin
package com.example.sageassistant.network

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// -----------------------------------------------------------------------------
// DTO Models
// -----------------------------------------------------------------------------

data class StartRunRequest(
    val workflowKey: String,
    val message: String
)

data class MessageRequest(
    val message: String
)

data class RunResponse(
    val runId: String,
    val workflowKey: String,
    val status: String,
    val stageIndex: Int,
    val stageCount: Int,
    val stageKey: String,
    val stageName: String,
    val message: String,
    val output: Map<String, String>?,
    val completed: Boolean
)

// -----------------------------------------------------------------------------
// Retrofit API Service
// -----------------------------------------------------------------------------

interface SageApiService {

    // 1. Start a new run
    @POST("/api/v1/workflows/runs")
    suspend fun startRun(
        @Body request: StartRunRequest
    ): Response<RunResponse>

    // 2. Send 'yes' (approval) or user feedback
    @POST("/api/v1/workflows/runs/{runId}/messages")
    suspend fun sendMessage(
        @Path("runId") runId: String,
        @Body request: MessageRequest
    ): Response<RunResponse>

    // 3. Check current run status and stage
    @GET("/api/v1/workflows/runs/{runId}")
    suspend fun getRunDetail(
        @Path("runId") runId: String
    ): Response<RunResponse>

    // 4. Download Word .docx file
    @Streaming
    @GET("/api/v1/workflows/runs/{runId}/files/docx")
    suspend fun downloadDocx(
        @Path("runId") runId: String
    ): Response<ResponseBody>

    // 5. Download Final PDF file
    @Streaming
    @GET("/api/v1/workflows/runs/{runId}/files/pdf")
    suspend fun downloadPdf(
        @Path("runId") runId: String
    ): Response<ResponseBody>

    // 6. Upload Word file for PDF Print
    @Multipart
    @POST("/api/v1/workflows/runs/{runId}/upload")
    suspend fun uploadDocx(
        @Path("runId") runId: String,
        @Part file: MultipartBody.Part
    ): Response<RunResponse>
}
```

---

## 5. COMPLETE EXAMPLE CLIENT CALL FLOW

```kotlin
// Step A: User chooses "Worksheet Generator" for Class 10 Chemistry
val startResp = apiService.startRun(
    StartRunRequest(workflowKey = "worksheet-generation", message = "Class 10 Chemistry Chemical Reactions")
)
val runId = startResp.body()!!.runId
val outlineMessage = startResp.body()!!.message // Display this in UI

// Step B: User clicks "Approve & Generate Word Draft"
val draftResp = apiService.sendMessage(runId, MessageRequest("yes"))
val draftMessage = draftResp.body()!!.message // Display: "Alpha Tutor Worksheet Word doc generated!"

// Step C: (Optional) User downloads intermediate Word doc to inspect
val docxStream = apiService.downloadDocx(runId)

// Step D: User confirms Word draft by saying "yes" to export final PDF
val pdfResp = apiService.sendMessage(runId, MessageRequest("yes"))

// Step E: User downloads ready-to-print Alpha Tutor PDF
val pdfStream = apiService.downloadPdf(runId)
// Save pdfStream.body()!!.byteStream() to a local file or open in PDF viewer
```
