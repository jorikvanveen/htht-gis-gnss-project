package com.example.gnss;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EditSingleEntry extends AppCompatActivity {
    private DataVault vault;

    private LinearLayout answerContainer;
    private List<View> inputFields;

    double latitude;
    double longitude;
    private Survey survey;

    private ArrayList<Answer> answers;

    ArrayList<SurveyQuestion> questions;

    UUID surveyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_single_entry);
        answerContainer = findViewById(R.id.answerContainer);
        inputFields = new ArrayList<>();
        answers = new ArrayList<>();

        int entryId = getIntent().getIntExtra("entry_id", 0);
        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        surveyId = (UUID) receivedIntent.getExtras().get("survey_id");

        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);

        SurveyDataPoint entry = entries.get(entryId);
        survey = vault.getSurvey(surveyId).get();
        questions = survey.getQuestions();

        latitude = entry.getLat();
        longitude = entry.getLon();

        Button submitButton = findViewById(R.id.submitButton);
        submitButton.setOnClickListener(v -> {
            try {
                collectAnswers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });



        EditText displayLat = new EditText(this);
        EditText displayLon = new EditText(this);



        TextView lat = new TextView(this);
        lat.setText("Latitude");
        TextView lon = new TextView(this);
        lon.setText("Longitude");
        String doubleLat = Double.toString(latitude);
        String doubleLon = Double.toString(longitude);


        displayLat.setText(doubleLat);
        displayLon.setText(doubleLon);
        answerContainer.addView(lat);
        answerContainer.addView(displayLat);
        answerContainer.addView(lon);
        answerContainer.addView(displayLon);

        inputFields.add(displayLon);
        inputFields.add(displayLat);


        ArrayList<Answer> answers = entry.getAnswers();


        for (int i = 0; i < answers.size(); i++) {
            EditText questionPrompt = new EditText(this);
            questionPrompt.setHint(questions.get(i).getPrompt());
            TextView questionTextView  = new TextView(this);
            questionTextView.setText(questions.get(i).getPrompt());
            Answer answer = answers.get(i);
            questionPrompt.setText(answer.toString());
            Toast.makeText(this,answer.toString() , Toast.LENGTH_SHORT).show();

            answerContainer.addView(questionTextView);
            answerContainer.addView(questionPrompt);
            inputFields.add(questionPrompt);

        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void collectAnswers() throws IOException {


        answers = new ArrayList<>();


        // Iterate through all input fields and collect answers
        for (int i = 0; i < inputFields.size(); i++) {
            View inputField = inputFields.get(i);

            SurveyQuestionType type = questions.get(i).getType();
            if (i==0) {
                try {
                    latitude = Double.parseDouble(((EditText) inputField).getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Enter a double", Toast.LENGTH_SHORT).show();
                    break;

                }
            }else if (i == 1) {
                try {
                    longitude = Double.parseDouble(((EditText) inputField).getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Enter a double", Toast.LENGTH_SHORT).show();
                    break;
                }
            }
            // Determine the type of input and collect the answer
            else if (inputField instanceof EditText) {
                String answer = ((EditText) inputField).getText().toString();
                if (validateAndRegisterAnswer(answer, type, this, i)) {
                    Toast.makeText(this, "Answer to question " + (i + 1) + ": " + answer, Toast.LENGTH_SHORT).show();
                } else {
                    break;
                }
            } else if (inputField instanceof Spinner) {
                String answer = ((Spinner) inputField).getSelectedItem().toString();
                if (validateAndRegisterAnswer(answer, type, this, i)) {
                    Toast.makeText(this, "Answer to question " + (i + 1) + ": " + answer, Toast.LENGTH_SHORT).show();
                } else {
                    break;
                }
            }
            if (inputFields.size() == i + 1) {
                SurveyDataPoint entry = new SurveyDataPoint(latitude, longitude, answers);
                vault.saveEntry(surveyId, entry);
                DataVault.save(this);
                finish();
            }
        }
    }


        private boolean validateAndRegisterAnswer (String input, SurveyQuestionType type, Context
        context,int i){
            switch (type) {
                case String:
                    // Any string is valid, so no validation needed
                    StringAnswer stringAnswer = new StringAnswer(input);
                    answers.add(stringAnswer);

                    return true;

                case Boolean:
                    // Expecting "true" or "false"
                    if (input.equalsIgnoreCase("True") || input.equalsIgnoreCase("False")) {
                        if (input.equals("True")) {
                            BooleanAnswer booleanAnswer = new BooleanAnswer(true);
                            answers.add(booleanAnswer);
                        } else {
                            BooleanAnswer booleanAnswer = new BooleanAnswer(false);
                            answers.add(booleanAnswer);
                        }
                        return true;
                    } else {
                        Toast.makeText(context, "Please enter 'true' or 'false' for question: " + i + 1, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                case Integer:
                    // Check if the input can be parsed as an integer
                    try {
                        Integer.parseInt(input);
                        IntAnswer intAnswer = new IntAnswer(Integer.parseInt(input));
                        answers.add(intAnswer);
                        return true;
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Please enter a valid integer for question: " + i + 1, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                case Float:
                    // Check if the input can be parsed as a float
                    try {
                        Float.parseFloat(input);
                        FloatAnswer floatAnswer = new FloatAnswer(Float.parseFloat(input));
                        answers.add(floatAnswer);
                        return true;
                    } catch (NumberFormatException e) {
                        Toast.makeText(context, "Please enter a valid float number for question: " + i + 1, Toast.LENGTH_SHORT).show();
                        return false;
                    }

                default:
                    Toast.makeText(context, "Unknown question type.", Toast.LENGTH_SHORT).show();
                    return false;
            }

        }

    }