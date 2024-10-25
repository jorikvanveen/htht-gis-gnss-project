package com.example.gnss.dto;

import java.util.ArrayList;

public class SurveyDataPoint {
    public double lon;
    public double lat;
    public String date;
    public String time;
    public ArrayList<Answer> answers;


    public String name;
    public SurveyDataPoint(String name, double lat, double lon, String date, String time, ArrayList<Answer> answers) {
        this.lon = lon;
        this.lat = lat;
        this.date = date;
        this.time = time;
        this.name = name;
        this.answers = answers;
    }
    public SurveyDataPoint(){

    }
    public String getDate(){
        return date;
    }
    public String getTime(){
        return time;
    }

    public String getName() {
        return name;
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
