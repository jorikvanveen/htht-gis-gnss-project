package com.example.gnss.dto;

import java.util.UUID;

public class SurveyQuestion {
    private UUID id;
    private SurveyQuestionType type;
    private String prompt;

    public SurveyQuestionType getType() {
        return type;
    }

    public void setType(SurveyQuestionType type) {
        this.type = type;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
