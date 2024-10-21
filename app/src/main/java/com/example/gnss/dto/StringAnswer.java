package com.example.gnss.dto;

public final class StringAnswer implements Answer {
    public String value;

    private StringAnswer(String value) {
        this.value = value;
    }

    public String getValues() {
        return this.value;
    }
}
