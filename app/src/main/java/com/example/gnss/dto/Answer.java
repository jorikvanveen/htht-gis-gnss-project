package com.example.gnss.dto;

public sealed interface Answer permits BooleanAnswer, FloatAnswer, IntAnswer, StringAnswer {};

//public abstract class Answer {
//    private float value;
//    private SurveyQuestionType questionType;
//    public Answer(float value, SurveyQuestionType type)  {
//        this.value = value;
//    }
//
//    public T getValue() {
//        return value;
//    }
//
//    public SurveyQuestionType getQuestionType() {
//        return questionType;
//    }
//}
