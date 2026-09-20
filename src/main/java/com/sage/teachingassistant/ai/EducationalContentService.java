package com.sage.teachingassistant.ai;

import org.springframework.stereotype.Service;

/**
 * High-level service that orchestrates AI research and content generation
 * for Worksheets, Tests, and Notes.
 */
@Service
public class EducationalContentService {

    private final GeminiClient geminiClient;

    public EducationalContentService(GeminiClient geminiClient) {
        this.geminiClient = geminiClient;
    }

    /**
     * Researches curriculum on the live web and proposes a structured outline for a Worksheet.
     */
    public String researchWorksheetOutline(String className, String chapterName, String additionalDetails, String feedback) {
        String sys = "You are Sage, an expert educational curriculum specialist and teaching assistant. "
                + "You MUST search the live web for the latest curriculum guidelines, syllabus topics, and textbook material "
                + "(e.g., NCERT, CBSE, ICSE, or general educational boards) for the specified class, chapter, and details.\n\n"
                + "You MUST organize your response into the following 3 distinct sections:\n\n"
                + "### 1. 🔍 Brief Information Found on the Web\n"
                + "- Syllabus scope, key learning objectives, and core concepts for this chapter.\n"
                + "- Essential formulas, definitions, and problem patterns identified on the web.\n"
                + "- Recommended question types (MCQs, Short Answer, Conceptual, Numericals) and mark distribution.\n\n"
                + "### 2. 🌐 Reference Web Links\n"
                + "- Provide direct, valid web links found on the web (e.g., NCERT official digital textbooks, CBSE curriculum portals, Khan Academy, or standard study portals).\n\n"
                + "### 3. ❓ Confirmation & Next Step\n"
                + "- Conclude by asking: 'This is what I found on the web for " + className + " - " + chapterName + ". Are you sure you want to go with it?\n\n"
                + "Click **Confirm** (or reply \"yes\") to proceed and generate the Word (.docx) document, or reply with what you'd like to adjust.'";

        String prompt = "Target Class/Grade: " + className + "\n"
                + "Chapter / Subject Topic: " + chapterName + "\n";
        if (additionalDetails != null && !additionalDetails.isBlank()) {
            prompt += "Additional Context / Requirements: " + additionalDetails.trim() + "\n";
        }
        if (feedback != null && !feedback.isBlank()) {
            prompt += "User's previous revision request: " + feedback.trim() + "\n";
        }
        prompt += "Search the web and provide the 3 required sections: Brief Information, Reference Web Links, and Confirmation Prompt.";

        return geminiClient.generate(sys, prompt, true);
    }

    public String researchWorksheetOutline(String request, String feedback) {
        return researchWorksheetOutline("Target Class", request, "", feedback);
    }

    /**
     * Researches curriculum on the live web and proposes a blueprint for a Test Paper.
     */
    public String researchTestBlueprint(String className, String chapterName, String additionalDetails, String feedback) {
        String sys = "You are Sage, an expert exam creator and teaching assistant. "
                + "You MUST search the live web for the latest examination patterns, sample papers, and syllabus guidelines "
                + "(e.g., NCERT, CBSE, ICSE, or general educational boards) for the specified class and chapter.\n\n"
                + "You MUST organize your response into the following 3 distinct sections:\n\n"
                + "### 1. 🔍 Brief Information Found on the Web\n"
                + "- Test examination blueprint: Total Marks, Time Duration, and Section Breakdown (Section A: Objective/MCQ, Section B: Short Answer, Section C: Long/Numerical).\n"
                + "- Topic-wise marks weightage and difficulty level (Foundational / Moderate / HOTS).\n\n"
                + "### 2. 🌐 Reference Web Links\n"
                + "- Provide direct, valid web links found on the web (e.g. CBSE sample question papers, NCERT official textbook, standard curriculum portals).\n\n"
                + "### 3. ❓ Confirmation & Next Step\n"
                + "- Conclude by asking: 'This is what I found on the web for " + className + " - " + chapterName + ". Are you sure you want to go with it?\n\n"
                + "Click **Confirm** (or reply \"yes\") to proceed and generate the Word (.docx) document, or reply with what you'd like to adjust.'";

        String prompt = "Target Class/Grade: " + className + "\n"
                + "Chapter / Subject Topic: " + chapterName + "\n";
        if (additionalDetails != null && !additionalDetails.isBlank()) {
            prompt += "Additional Context / Requirements: " + additionalDetails.trim() + "\n";
        }
        if (feedback != null && !feedback.isBlank()) {
            prompt += "User's previous revision request: " + feedback.trim() + "\n";
        }
        prompt += "Search the web and provide the 3 required sections: Brief Information, Reference Web Links, and Confirmation Prompt.";

        return geminiClient.generate(sys, prompt, true);
    }

    public String researchTestBlueprint(String request, String feedback) {
        return researchTestBlueprint("Target Class", request, "", feedback);
    }

    /**
     * Researches curriculum on the live web and proposes a structured outline for Revision Notes.
     */
    public String researchNotesOutline(String className, String chapterName, String additionalDetails, String feedback) {
        String sys = "You are Sage, an expert academic author and teaching assistant. "
                + "You MUST search the live web for comprehensive study notes and textbook materials "
                + "(e.g., NCERT, CBSE, ICSE) for the specified class and chapter.\n\n"
                + "You MUST organize your response into the following 3 distinct sections:\n\n"
                + "### 1. 🔍 Brief Information Found on the Web\n"
                + "- Scope of revision notes: Core definitions, laws/theorems, formulas, and comparison tables.\n"
                + "- Key diagrams, memory tips, and common exam traps/misconceptions.\n\n"
                + "### 2. 🌐 Reference Web Links\n"
                + "- Provide direct, valid web links found on the web (e.g. NCERT textbooks, syllabus portals, Khan Academy).\n\n"
                + "### 3. ❓ Confirmation & Next Step\n"
                + "- Conclude by asking: 'This is what I found on the web for " + className + " - " + chapterName + ". Are you sure you want to go with it?\n\n"
                + "Click **Confirm** (or reply \"yes\") to proceed and generate the Word (.docx) document, or reply with what you'd like to adjust.'";

        String prompt = "Target Class/Grade: " + className + "\n"
                + "Chapter / Subject Topic: " + chapterName + "\n";
        if (additionalDetails != null && !additionalDetails.isBlank()) {
            prompt += "Additional Context / Requirements: " + additionalDetails.trim() + "\n";
        }
        if (feedback != null && !feedback.isBlank()) {
            prompt += "User's previous revision request: " + feedback.trim() + "\n";
        }
        prompt += "Search the web and provide the 3 required sections: Brief Information, Reference Web Links, and Confirmation Prompt.";

        return geminiClient.generate(sys, prompt, true);
    }

    public String researchNotesOutline(String request, String feedback) {
        return researchNotesOutline("Target Class", request, "", feedback);
    }

    /**
     * Generates full worksheet content based on the approved outline.
     */
    public String generateWorksheetContent(String request, String approvedOutline, String feedback) {
        String sys = "You are Sage, generating a complete, ready-to-print Alpha Tutor classroom worksheet. "
                + "Generate high quality questions adhering to the approved outline. Format with clear section headers: "
                + "Section A: Multiple Choice Questions (with options A, B, C, D)\n"
                + "Section B: Fill in the Blanks / Matching\n"
                + "Section C: Short & Conceptual Questions (with marks specified [2 Marks])\n"
                + "Provide complete, accurate questions suitable for student practice.";

        String prompt = "Topic & Grade: " + request + "\n"
                + "Approved Outline: " + approvedOutline + "\n";
        if (feedback != null && !feedback.isBlank()) {
            prompt += "Teacher's revision suggestions to incorporate: " + feedback + "\n";
        }
        prompt += "Generate the full printable worksheet content.";

        return geminiClient.generate(sys, prompt, false);
    }

    /**
     * Generates complete test paper and answer key based on the approved blueprint.
     */
    public String generateTestContent(String request, String approvedBlueprint, String feedback) {
        String sys = "You are Sage, generating a formal Alpha Tutor Test Examination Paper. "
                + "Include: General Instructions, Section A (Objective Questions), Section B (Short Answer Questions), "
                + "Section C (Long/Numerical/Application Questions), with marks clearly noted next to each question. "
                + "At the end, include an Answer Key and Marking Scheme for the teacher.";

        String prompt = "Topic & Grade: " + request + "\n"
                + "Approved Blueprint: " + approvedBlueprint + "\n";
        if (feedback != null && !feedback.isBlank()) {
            prompt += "Teacher's revision suggestions to incorporate: " + feedback + "\n";
        }
        prompt += "Generate the complete examination paper and marking rubric.";

        return geminiClient.generate(sys, prompt, false);
    }

    /**
     * Generates complete structured revision notes based on the approved outline.
     */
    public String generateNotesContent(String request, String approvedOutline, String feedback) {
        String sys = "You are Sage, generating comprehensive, student-friendly Alpha Tutor Revision Notes. "
                + "Include clearly defined headings, bullet-point explanations of core concepts, key definitions, "
                + "important formulas, mnemonics/tips, and a 'Summary at a Glance' section.";

        String prompt = "Topic & Grade: " + request + "\n"
                + "Approved Outline: " + approvedOutline + "\n";
        if (feedback != null && !feedback.isBlank()) {
            prompt += "Teacher's revision suggestions to incorporate: " + feedback + "\n";
        }
        prompt += "Generate the full detailed revision notes.";

        return geminiClient.generate(sys, prompt, false);
    }
}
