package com.example.gnss;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gnss.dto.Answer;
import com.example.gnss.dto.BooleanAnswer;
import com.example.gnss.dto.FloatAnswer;
import com.example.gnss.dto.IntAnswer;
import com.example.gnss.dto.StringAnswer;
import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyDataPoint;
import com.example.gnss.dto.SurveyQuestion;
import com.example.gnss.dto.SurveyQuestionType;
import com.example.gnss.singleton.DataVault;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

public class EditEntries extends AppCompatActivity {

    private Survey survey;

    private DataVault vault;

    private UUID surveyId;

    private ArrayList<String> answerStrings;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_entries);





        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        surveyId = (UUID) receivedIntent.getExtras().get("survey_id");
        survey = vault.getSurvey(surveyId).get();

        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);


        LinearLayout surveyList = this.findViewById(R.id.survey_list);

        Button saveLocationButton = findViewById(R.id.saveButton); // Button to save the location
        saveLocationButton.setOnClickListener(v -> saveLocationToCSVMultiple());


        for (int j = 0; j < entries.size(); j++) {

            LayoutInflater inflater = this.getLayoutInflater();
            View surveyPreview = inflater.inflate(R.layout.survey_preview, surveyList, false);
            TextView surveyNameView = surveyPreview.findViewById(R.id.survey_name);
            surveyNameView.setText(survey.getName());
            final int entryIndex = j;
            String entryText = entries.get(entryIndex).getName();
            surveyNameView.setText(entryText);

            surveyPreview.setOnClickListener(v -> {
                Intent intent = new Intent(this, EditSingleEntry.class);
                intent.putExtra("survey_id", survey.getId());
                intent.putExtra("entry_id", entryIndex);

                startActivity(intent);


            });

            MaterialButton optionsButton = surveyPreview.findViewById(R.id.survey_preview_threedot);
            optionsButton.setOnClickListener(v -> {
                var popupMenu = new PopupMenu(this, optionsButton);
                popupMenu.getMenuInflater().inflate(R.menu.entry_options, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete_survey) {
                        // TODO
                       vault.getSurveyEntries(surveyId).remove(entries.get(entryIndex));
                        try {
                            DataVault.save(this);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        recreate();


                    }else if (item.getItemId() == R.id.action_export) {
                        saveLocationToCSVSingle(entryIndex);
                    }

                    return true;
                });
                popupMenu.show();
            });
            surveyList.addView(surveyPreview);

        }

    }
    private void saveLocationToCSVSingle(int entryIndex) {
        // Prepare the CSV data as a string
        SurveyDataPoint entry = vault.getSurveyEntries(surveyId).get(entryIndex);
        double latitude = entry.getLat();
        double longitude = entry.getLon();
        String name = entry.getName();
        String date = entry.getDate();
        String time = entry.getTime();
        ArrayList<String> answerStrings = new ArrayList<>();

        ArrayList<SurveyQuestion> questions = vault.getSurvey(surveyId).get().getQuestions();
        ArrayList<Answer> answers = entry.getAnswers();

        String csvData = "Name,Latitude,Longitude,Date,Time";
        String csvAnswers = name+ "," + latitude + "," + longitude + "," + date + "," + time;


        for (int i = 0; i < questions.size(); i++) {
            var question = questions.get(i);
            var questionType = question.getType();
            Answer genAnswer = answers.get(i);
            switch (questionType) {
                case String -> {
                    StringAnswer answer = (StringAnswer) genAnswer;
                    answerStrings.add(answer.value);
                }
                case Float -> {
                    FloatAnswer answer = (FloatAnswer) genAnswer;
                    answerStrings.add(Float.toString(answer.value));
                }
                case Integer -> {
                    IntAnswer answer = (IntAnswer) genAnswer;
                    answerStrings.add(Integer.toString(answer.value));
                }
                case Boolean -> {
                    BooleanAnswer answer = (BooleanAnswer) genAnswer;
                    answerStrings.add(Boolean.toString(answer.value));
                }
            }
        }
        for (SurveyQuestion question : questions){
            csvData = csvData + "," + question.getPrompt();
        }
        csvData = csvData + "\n";
        for(String answer : answerStrings){
            csvAnswers = csvAnswers + "," + answer;
        }
        csvAnswers = csvAnswers + "\n";

        csvData = csvData + csvAnswers;

        // Define the content values for the CSV file
        String filename = name + "_coordinates.csv";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);  // File name
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");  // File type (CSV)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");  // Save the file in the Downloads directory

        // Get the ContentResolver to handle the file insertion
        ContentResolver resolver = getContentResolver();
        Uri uri = null;

        // For Android 10 (API level 29) and above, use MediaStore to create the file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        // If the URI was successfully created, write the CSV data to the file
        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(csvData.getBytes());  // Write the CSV data to the file
                    Toast.makeText(this, "CSV saved to Downloads folder", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to create output", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // Handle any errors that occur during the file writing process
                e.printStackTrace();
                Toast.makeText(this, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Error creating CSV file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveLocationToCSVMultiple() {
        // Prepare the CSV data as a string
        StringBuilder csvData = new StringBuilder("Name,Latitude,Longitude,Date,Time");
        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);
        ArrayList<SurveyQuestion> questions = vault.getSurvey(surveyId).get().getQuestions();
        for (int i = 0; i < questions.size(); i++) {
            csvData.append(","+ questions.get(i).getPrompt());

        }
        csvData.append("\n");
        for(SurveyDataPoint entry : entries) {

            double latitude = entry.getLat();
            double longitude = entry.getLon();
            String name = entry.getName();
            String date = entry.getDate();
            String time = entry.getTime();
            ArrayList<Answer> answers = entry.getAnswers();
            answerStrings = new ArrayList<>();

            for (int i = 0; i < questions.size(); i++) {
                var question = questions.get(i);
                var questionType = question.getType();
                Answer genAnswer = answers.get(i);
                switch (questionType) {
                    case String -> {
                        StringAnswer answer = (StringAnswer) genAnswer;
                        answerStrings.add(answer.value);
                    }
                    case Float -> {
                        FloatAnswer answer = (FloatAnswer) genAnswer;
                        answerStrings.add(Float.toString(answer.value));
                    }
                    case Integer -> {
                        IntAnswer answer = (IntAnswer) genAnswer;
                        answerStrings.add(Integer.toString(answer.value));
                    }
                    case Boolean -> {
                        BooleanAnswer answer = (BooleanAnswer) genAnswer;
                        answerStrings.add(Boolean.toString(answer.value));
                    }
                }
            }
            csvData.append(name)
                    .append(",")
                    .append(latitude)
                    .append(",")
                    .append(longitude)
                    .append(",")
                    .append(date)
                    .append(",")
                    .append(time);



            for (String string : answerStrings){

                csvData.append(",")
                        .append(string);
            }
            csvData.append("\n");
        }
        //have to clear



        // Define the content values for the CSV file
        String filename = survey.getName() + "_coordinates.csv";
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);  // File name
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");  // File type (CSV)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");  // Save the file in the Downloads directory

        // Get the ContentResolver to handle the file insertion
        ContentResolver resolver = getContentResolver();
        Uri uri = null;

        // For Android 10 (API level 29) and above, use MediaStore to create the file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        // If the URI was successfully created, write the CSV data to the file
        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(csvData.toString().getBytes());  // Write the CSV data to the file
                    Toast.makeText(this, "CSV saved to Downloads folder", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to create output", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // Handle any errors that occur during the file writing process
                e.printStackTrace();
                Toast.makeText(this, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Error creating CSV file", Toast.LENGTH_SHORT).show();
        }
    }
//  private void saveLocationToCSVMultiple() {
//        // Prepare the CSV data as a string
//
//        StringBuilder csvData = new StringBuilder("Name,Latitude,Longitude,Date,Time\n");
//
//        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);
//
//        for(SurveyDataPoint entry : entries) {
//
//            double latitude = entry.getLat();
//            double longitude = entry.getLon();
//            String name = entry.getName();
//
//            String date = entry.getDate();
//            String time = entry.getTime();
//            csvData.append(name)
//                    .append(",")
//                    .append(latitude)
//                    .append(",")
//                    .append(longitude)
//                    .append(",")
//                    .append(date)
//                    .append(",")
//                    .append(time)
//                    .append("\n");
//
//            for (String string : answerStrings){
//
//                csvData.append(",")
//                        .append(string);
//            }
//            csvData.append("\n");
//        }
//        // Define the content values for the CSV file
//        String filename = survey.getName() + "_coordinates.csv";
//        ContentValues values = new ContentValues();
//        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);  // File name
//        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");  // File type (CSV)
//        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");  // Save the file in the Downloads directory



    @Override
    protected void onRestart() {
        super.onRestart();
        recreate();
    }
}