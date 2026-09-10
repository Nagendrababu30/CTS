package com.iispl.cts.service.outward;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import com.iispl.cts.dao.outward.AIChatbotDAO;

public class AIChatbotService {

    private final Client client;

    private final AIChatbotDAO aiChatbotDAO;

    private static final String MODEL = "gemini-3.6-flash";

    public AIChatbotService() {

    	String apiKey = System.getenv("GEMINI_API_KEY");

        client = Client.builder()
                .apiKey(apiKey)
                .build();

        aiChatbotDAO = new AIChatbotDAO();
    }


    // =========================================================
    // MAIN AI METHOD
    // =========================================================

    public String askAI(String question) {

        if (question == null ||
                question.trim().isEmpty()) {

            return "Please enter a question.";
        }

        String userQuestion =
                question.trim();


        // =====================================================
        // DATABASE QUESTION
        // =====================================================

        if (isTotalBatchCountQuestion(userQuestion)) {

            int totalBatches =
                    aiChatbotDAO.getTotalBatchCount();

            return "There are currently "
                    + totalBatches
                    + " batches in the outward batch table.";
        }


        // =====================================================
        // NORMAL AI QUESTION
        // =====================================================

        try {

            GenerateContentResponse response =
                    client.models.generateContent(
                            MODEL,
                            userQuestion,
                            null
                    );

            if (response == null) {

                return "Gemini returned no response.";
            }

            String answer =
                    response.text();

            if (answer == null ||
                    answer.trim().isEmpty()) {

                return "Gemini returned an empty response.";
            }

            return answer;

        } catch (Exception e) {

            e.printStackTrace();

            return "Gemini API error: "
                    + e.getMessage();
        }
    }


    // =========================================================
    // CHECK TOTAL BATCH COUNT QUESTION
    // =========================================================

    private boolean isTotalBatchCountQuestion(
            String question) {

        String q =
                question.toLowerCase().trim();

        return q.equals("how many batches are there?")
                || q.equals("how many batches are there")
                || q.equals("how many batches?")
                || q.equals("how many batches")
                || q.contains("total number of batches")
                || q.contains("total batches");
    }
}