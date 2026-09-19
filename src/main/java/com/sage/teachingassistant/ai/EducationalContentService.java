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
     * Researches curriculum and proposes a structured outline for a Worksheet.
     */
    public String researchWorksheetOutline(String request, String feedback) {
        String sys = "You are Sage, an expert educational curriculum specialist and teaching assistant. "
                + "Your task is to analyze the user's requested topic and class/grade, search for standard curriculum guidelines "
                + "(e.g., NCERT, CBSE, ICSE, or general educational standards), and propose a structured worksheet outline. "
                + "Specify target grade level, key concepts tested, and recommended question types (MCQs, Fill in the blanks, Short Answer, Conceptual Questions). "
                + "Keep the outline clear, concise, and structured so the teacher can easily approve or adjust it.";

        String prompt = "User request: \"" + request + "\"\n";
        if (feedback != null && !feedback.isBlank()) {
            prompt += "Previous feedback/adjustment requested: \"" + feedback.trim() + "\"\n";
        }
        prompt += "Provide the curriculum-grounded topic summary and question blueprint for confirmation.";

        return geminiClient.generate(sys, prompt, true);
    }

    /**
     * Researches curriculum and proposes a blueprint for a Test Paper.
     */
    public String researchTestBlueprint(String request, String feedback) {
        String sys = "You are Sage, an expert exam creator and teaching assistant. "
                + "Analyze the topic, target class/grade, and propose a complete Test Paper Blueprint including: "
                + "Total Marks, Time Duration, Section Breakdown (Section A: 1-mark objective/MCQ, Section B: 3-mark short answer, Section C: 5-mark long answer/application), "
                + "and key topics covered in each section according to standard curriculum guidelines.";

        String prompt = "User request: \"" + request + "\"\n";
        if (feedback != null && !feedback.isBlank()) {
            prompt += "Previous feedback/adjustment requested: \"" + feedback.trim() + "\"\n";
        }
        prompt += "Provide the test blueprint and syllabus topics for teacher confirmation.";

        return geminiClient.generate(sys, prompt, true);
    }

    /**
     * Researches curriculum and proposes a structured outline for Revision Notes.
     */
    public String researchNotesOutline(String request, String feedback) {
        String sys = "You are Sage, an expert academic writer and teaching assistant. "
                + "Analyze the topic and class/grade, and propose an outline for comprehensive revision notes: "
                + "Major concepts, core formulas/laws, key diagrams or comparison tables, and summary takeaways. "
                + "Keep it structured so the teacher can confirm whether the scope matches their syllabus.";

        String prompt = "User request: \"" + request + "\"\n";
        if (feedback != null && !feedback.isBlank()) {
            prompt += "Previous feedback/adjustment requested: \"" + feedback.trim() + "\"\n";
        }
        prompt += "Provide the revision notes outline for confirmation.";

        return geminiClient.generate(sys, prompt, true);
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
