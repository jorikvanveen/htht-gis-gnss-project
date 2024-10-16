package com.example.gnss.dto;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.UUID;
import java.util.Optional;

/**
 * Represents a single survey question.
 */
public class SurveyQuestion implements Serializable {
    private UUID id;
    private SurveyQuestionType type;
    private String prompt;

    public SurveyQuestion(@NonNull Optional<UUID> id,
                          @NonNull SurveyQuestionType type,
                          @NonNull String prompt) {
        this.id = id.orElse(UUID.randomUUID());
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

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }
}
