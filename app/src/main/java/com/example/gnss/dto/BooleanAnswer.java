package com.example.gnss.dto;

public final class BooleanAnswer implements Answer {
    public boolean value;

    private BooleanAnswer(boolean value) {
        this.value = value;
    }

    public boolean getValues() {
        return this.value;
    }
}
