package com.medigenius.ai.agents;

import com.medigenius.ai.AgentNode;
import com.medigenius.ai.AgentState;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Direct port of backend/app/agents/planner.py -> PlannerAgent.
 * Decides whether to route to the "retriever" (RAG) or "llm_agent" (direct LLM) node
 * based on a case-insensitive substring match against the exact same medical keyword list
 * used in the Python implementation. Sets state.currentTool accordingly.
 */
@Component
public class PlannerAgent implements AgentNode {

    // ── Medical Keywords — copied verbatim from agents/planner.py MEDICAL_KEYWORDS ──
    private static final List<String> MEDICAL_KEYWORDS = List.of(
            // Symptoms
            "fever", "pain", "headache", "nausea", "vomiting", "diarrhea", "cough",
            "acne", "pimple", "skin", "rash", "itch", "cold", "flu",
            "shortness of breath", "chest pain", "abdominal pain", "back pain",
            "joint pain", "muscle pain", "fatigue", "weakness", "dizziness",
            "confusion", "memory loss", "seizure", "numbness", "tingling", "swelling",
            "bleeding", "bruising", "weight loss", "weight gain",
            "appetite loss", "sleep problems", "insomnia",
            // Conditions
            "cancer", "diabetes", "hypertension", "heart disease", "stroke", "asthma",
            "copd", "pneumonia", "bronchitis", "covid", "coronavirus",
            "infection", "virus", "bacteria", "fungal", "arthritis", "osteoporosis",
            "thyroid", "kidney disease", "liver disease", "hepatitis", "depression",
            "anxiety", "bipolar", "schizophrenia", "alzheimer", "parkinson", "epilepsy",
            // Medical terms
            "treatment", "therapy", "medication", "medicine", "prescription", "dosage",
            "side effects", "diagnosis", "prognosis", "surgery", "operation",
            "procedure", "test", "lab results", "blood test", "x-ray", "mri",
            "ct scan", "ultrasound", "biopsy", "screening", "prevention", "vaccine",
            "immunization", "rehabilitation", "recovery", "chronic", "acute",
            "syndrome", "disorder", "symptom", "cure", "remedy", "doctor", "hospital",
            // Body parts
            "heart", "lung", "kidney", "liver", "brain", "stomach", "intestine",
            "blood", "bone", "muscle", "nerve", "eye", "ear", "throat",
            "neck", "spine", "joint", "head", "chest", "abdomen", "leg", "arm"
    );

    @Override
    public AgentState run(AgentState state) {
        String question = state.getQuestion() == null ? "" : state.getQuestion().toLowerCase();
        boolean containsMedical = MEDICAL_KEYWORDS.stream().anyMatch(question::contains);

        state.setNextTool(containsMedical ? "retriever" : "llm_agent");
        state.setRetryCount(0);
        return state;
    }

    @Override
    public String name() {
        return "planner";
    }
}
