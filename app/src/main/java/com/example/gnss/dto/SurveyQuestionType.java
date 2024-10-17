package com.example.gnss.dto;

import java.io.Serializable;
import java.util.Optional;

/**
 * Represents all the possible answer types for a survey question, gets converted from values in
 * res/values/question_types.xml
 */
public enum SurveyQuestionType implements Serializable {
    String,
    Float,
    Integer,
    Boolean;

    private SurveyQuestionType() { }

    public static Optional<SurveyQuestionType> fromString(String str) {
        switch (str) {
            case "Text":
                return Optional.of(SurveyQuestionType.String);
            case "Boolean":
                return Optional.of(SurveyQuestionType.Boolean);
            case "Integer":
                return Optional.of(SurveyQuestionType.Integer);
            case "Number":
                return Optional.of(SurveyQuestionType.Float);
            default:
                return Optional.empty();
        }
    }
}
