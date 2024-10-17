package com.example.gnss.dto;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a Survey, which is a list of questions with a name and id. This object should also be
 * used when returning or passing stuff into our storage layer/database. This implements Serializable
 * so that we can (de)serialize this class from/into .json files. Users can export and import these
 * files to easily share survey definitions with eachother.
 */
public class Survey implements Serializable {
    // TODO: Timestamp
    private UUID id;
    private String name;
    private ArrayList<SurveyQuestion> questions;

    private Survey() {};

    public Survey(@NonNull Optional<UUID> id,
                  @NonNull String name,
                  @NonNull Optional<ArrayList<SurveyQuestion>> questions) {
        this.id = id.orElse(UUID.randomUUID());
        this.name = name;
        this.questions = questions.orElse(new ArrayList<>());
    }
}
