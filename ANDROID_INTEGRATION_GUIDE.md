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

### STEP 1: Starting a Workflow Run with Structured Inputs
When the user picks an option (Worksheet, Test, or Notes) from the chatbot or dashboard, send the Class, Chapter, and any Additional Context:

- **Endpoint**: `POST /api/v1/workflows/runs`
- **Headers**: `Content-Type: application/json`
- **Request Body (Structured Form or Chatbot)**:
```json
{
  "workflowKey": "worksheet-generation",
  "className": "Class 10",
  "chapterName": "Light - Reflection and Refraction",
  "additionalDetails": "Include ray diagram concepts, mirror formula numericals, and CBSE board pattern questions"
}
```
*(You can also optionally supply `"message"` directly, or use `"className"`, `"chapterName"`, and `"additionalDetails"`).*
*(Valid `workflowKey` values: `"worksheet-generation"`, `"test-generation"`, `"notes-generation"`, `"pdf-print"`).*

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
  "message": "### 1. 🔍 Brief Information Found on the Web\n- CBSE/NCERT curriculum scope...\n\n### 2. 🌐 Reference Web Links\n- https://ncert.nic.in/...\n- https://cbseacademic.nic.in/...\n\n### 3. ❓ Confirmation & Next Step\nThis is what I found on the web for Class 10 - Light. Are you sure you want to go with it?\nClick Confirm to generate the Word document, or reply with what you'd like to adjust.",
  "output": {
    "topic.className": "Class 10",
    "topic.chapterName": "Light - Reflection and Refraction",
    "stage.canConfirm": "true",
    "stage.action": "confirm_outline"
  },
  "completed": false
}
```

**UI Display for Stage 1**:
- Display the 3 items returned in `message`:
  1. **Brief Information** from the web.
  2. **Reference Web Links** (clickable links for user to verify).
  3. **Confirmation Question**.
- Show two primary UI elements:
  1. A **Confirm** button (calls the 1-click `/confirm` endpoint or sends `"confirm"`).
  2. An **Adjust / Feedback** text field (e.g. "change difficulty to hard").

---

### STEP 2: Advancing Stages with the "Confirm" Button or Messages

Each stage has an active confirmation action. When the user reviews the information and is happy, they can confirm to move forward.

#### Option A: 1-Click Confirm Button (Recommended)
Simply hit the dedicated confirm endpoint:
- **Endpoint**: `POST /api/v1/workflows/runs/{runId}/confirm`
- **Method**: `POST` (empty body)

#### Option B: Chatbot Message Approval
Or send a message payload:
- **Endpoint**: `POST /api/v1/workflows/runs/{runId}/messages`
- **Request Body**:
```json
{
  "message": "confirm"
}
```
*(Keywords recognized as approval: `"confirm"`, `"confirmed"`, `"yes"`, `"approve"`, `"proceed"`).*

#### Option C: Asking for Changes / Revisions (Iterative Feedback)
If the user wants adjustments:
- **Endpoint**: `POST /api/v1/workflows/runs/{runId}/messages`
- **Request Body**:
```json
{
  "message": "Please add 5 more numerical questions and include lens power formula."
}
```
Sage will re-execute the stage, incorporate the feedback, and return the revised proposal with the updated details.

---

### STEP 3: Stage 2 — Word Document (.docx) Generation & Review

Once Stage 1 is confirmed, Sage enters Stage 2:
- Generates the `.docx` document and provides the download link.
- Returns a summary of the generated document content.
- Asks the user to review the Word file.

**UI Actions**:
1. Provide a download button for the Word document: `GET /api/v1/workflows/runs/{runId}/files/docx`.
2. Allow user to send changes/revision text if needed.
3. Display the **Confirm** button: clicking it advances to Stage 3 (Final PDF).

---

### STEP 4: Stage 3 — Final Branded Alpha Tutor PDF Export

Once Stage 2 is confirmed:
- Sage compiles the approved Word document into the authentic Alpha Tutor PDF template:
  - Header with Alpha Tutor logo & golden accent line (`#F4B300`).
  - Centered bold titles & organized question tables.
  - Authentic footer on every page (dark green `#075D2A` & red `#D6181F`).
- Endpoint to download or view the PDF:
  `GET /api/v1/workflows/runs/{runId}/files/pdf`
- User can click **Confirm** to complete and close the run.

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

When the user selects the **PDF Print** workflow, you have two options:

#### Option 1: 1-Click Direct Upload & Convert (Recommended)
User picks a `.docx` file from their phone and you upload it in a single call:
- **Endpoint**: `POST /api/v1/workflows/runs/upload`
- **Headers**: `Content-Type: multipart/form-data`
- **Form Fields**:
  - `file`: the `.docx` file
  - `workflowKey`: `"pdf-print"` (optional, defaults to `"pdf-print"`)
- **Response**: Returns the `RunResponse` immediately containing the PDF download link:
  `GET /api/v1/workflows/runs/{runId}/files/pdf`

#### Option 2: 2-Step Flow
1. **Start the run**:
   `POST /api/v1/workflows/runs` with `{"workflowKey": "pdf-print", "message": "Direct print"}`
2. **Upload the Word document**:
   `POST /api/v1/workflows/runs/{runId}/upload` (multipart with part `file`)
3. **Download formatted PDF**:
   `GET /api/v1/workflows/runs/{runId}/files/pdf`

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
    val message: String? = null,
    val className: String? = null,
    val chapterName: String? = null,
    val additionalDetails: String? = null
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

    // 1. Start a new run with topic details
    @POST("/api/v1/workflows/runs")
    suspend fun startRun(
        @Body request: StartRunRequest
    ): Response<RunResponse>

    // 2. 1-Click Confirm current stage (advances to next stage)
    @POST("/api/v1/workflows/runs/{runId}/confirm")
    suspend fun confirmStage(
        @Path("runId") runId: String
    ): Response<RunResponse>

    // 3. Send feedback revision or chat approval
    @POST("/api/v1/workflows/runs/{runId}/messages")
    suspend fun sendMessage(
        @Path("runId") runId: String,
        @Body request: MessageRequest
    ): Response<RunResponse>

    // 4. Check current run status and stage
    @GET("/api/v1/workflows/runs/{runId}")
    suspend fun getRunDetail(
        @Path("runId") runId: String
    ): Response<RunResponse>

    // 5. Download Word .docx file
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

    // 6. Upload Word file for PDF Print (2-step flow)
    @Multipart
    @POST("/api/v1/workflows/runs/{runId}/upload")
    suspend fun uploadDocx(
        @Path("runId") runId: String,
        @Part file: MultipartBody.Part
    ): Response<RunResponse>

    // 7. 1-Click Direct Upload Word doc & Convert to PDF (1-step flow)
    @Multipart
    @POST("/api/v1/workflows/runs/upload")
    suspend fun uploadDocxDirect(
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
