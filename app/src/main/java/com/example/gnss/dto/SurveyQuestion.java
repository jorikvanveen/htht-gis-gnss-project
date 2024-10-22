package com.example.gnss.dto;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.UUID;
import java.util.Optional;

/**
 * Represents a single survey question.
 */
public class SurveyQuestion implements Serializable {
    private SurveyQuestionType type;
    private String prompt;

    private SurveyQuestion() {}

    public SurveyQuestion(@NonNull SurveyQuestionType type,
                          @NonNull String prompt) {
        this.type = type;
        this.prompt = prompt;
    }

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
}
