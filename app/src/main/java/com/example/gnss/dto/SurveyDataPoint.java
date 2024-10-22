package com.example.gnss.dto;

import java.util.ArrayList;

public class SurveyDataPoint {
    public double lon;
    public double lat;
    public ArrayList<Answer> answers;

    public SurveyDataPoint(double lat, double lon, ArrayList<Answer> answers) {
        this.lon = lon;
        this.lat = lat;
        this.answers = answers;
    }
    public SurveyDataPoint(){

    }

    public double getLon() {
        return lon;
    }

    public double getLat() {
        return lat;
    }

    public ArrayList<Answer> getAnswers() {
        return answers;
    }
}
