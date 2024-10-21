package com.example.gnss.dto;


public final class FloatAnswer implements Answer {
    public float value;

    private FloatAnswer(float value) {
        this.value = value;
    }

    public float getValues() {
        return this.value;
    }
}
