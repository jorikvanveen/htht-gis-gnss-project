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

public class SaveEntry extends AppCompatActivity {

    private Survey survey;

    private DataVault vault;

    private ArrayList<SurveyQuestion> questions;

    private LinearLayout questionContainer;
    private List<View> inputFields;
    double latitude;
    double longitude;

    private ArrayList<Answer> answers;


    private UUID surveyId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_save_entry);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        questionContainer = findViewById(R.id.questionContainer);
        Button submitButton = findViewById(R.id.submitButton);
        inputFields = new ArrayList<>();

        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        surveyId = (UUID) receivedIntent.getExtras().get("survey_id");
        survey = vault.getSurvey(surveyId).get();

        questions = survey.getQuestions();

        addLatLon();

        for(SurveyQuestion question : questions){
            addQuestionToLayout(question);
        }

        submitButton.setOnClickListener(v -> {
            try {
                collectAnswers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }
    private void addLatLon(){

        String formattedLat = String.format("%.6f", latitude);
        String formattedLon = String.format("%.6f", longitude);

        String latLonString = "Latitude: " +formattedLat + "     Longitude: "+formattedLon;


        TextView displayLatLon = new TextView(this);


        displayLatLon.setText(latLonString);



        displayLatLon.setPadding(0, 20, 0, 20);

        questionContainer.addView(displayLatLon);
    }

    private void addQuestionToLayout(SurveyQuestion question) {
        // Create a TextView for the question prompt
        TextView questionPrompt = new TextView(this);
        questionPrompt.setText(question.getPrompt());


        // Add the question prompt to the container
        questionContainer.addView(questionPrompt);

        // Create input field based on the question type
        switch (question.getType()) {
            case String:
                EditText stringInput = new EditText(this);
                stringInput.setHint("Enter text");
                questionContainer.addView(stringInput);
                inputFields.add(stringInput);
                break;
            case Integer:
                EditText integerInput = new EditText(this);
                integerInput.setHint("Enter number");
                integerInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
                questionContainer.addView(integerInput);
                inputFields.add(integerInput);
                break;
            case Float:
                EditText floatInput = new EditText(this);
                floatInput.setHint("Enter decimal number");
                floatInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
                questionContainer.addView(floatInput);
                inputFields.add(floatInput);
                break;
            case Boolean:
                Spinner booleanSpinner = new Spinner(this);
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.boolean_options, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                booleanSpinner.setAdapter(adapter);
                questionContainer.addView(booleanSpinner);
                inputFields.add(booleanSpinner);
                break;
        }
    }

    private void collectAnswers() throws IOException {


        answers = new ArrayList<>();



        // Iterate through all input fields and collect answers
        for (int i = 0; i < inputFields.size(); i++) {
            View inputField = inputFields.get(i);

            SurveyQuestionType type = questions.get(i).getType();

            // Determine the type of input and collect the answer
            if (inputField instanceof EditText) {
                String answer = ((EditText) inputField).getText().toString();
                if (validateAndRegisterAnswer(answer, type, this, i)) {
                    Toast.makeText(this, "Answer to question " + (i + 1) + ": " + answer, Toast.LENGTH_SHORT).show();
                } else {
                    break;
                }
            }else if (inputField instanceof Spinner) {
                    String answer = ((Spinner) inputField).getSelectedItem().toString();
                if (validateAndRegisterAnswer(answer, type, this, i)) {
                    Toast.makeText(this, "Added boolean answer: "+i+1, Toast.LENGTH_SHORT).show();
                } else {
                    break;
                }
            }
            if(inputFields.size() == i+1){
                SurveyDataPoint entry = new SurveyDataPoint(latitude, longitude, answers);
                vault.saveEntry(surveyId, entry);
                DataVault.save(this);
                finish();
            }
        }


    }
    private boolean validateAndRegisterAnswer(String input, SurveyQuestionType type, Context context, int i) {
        switch (type) {
            case String:
                // Any string is valid, so no validation needed
                StringAnswer stringAnswer = new StringAnswer(input);
                answers.add(stringAnswer);

                return true;

            case Boolean:
                // Expecting "true" or "false"
                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false")) {
                    if (input.equalsIgnoreCase("true")) {
                        BooleanAnswer booleanAnswer = new BooleanAnswer(true);
                        answers.add(booleanAnswer);
                    }else{
                        BooleanAnswer booleanAnswer = new BooleanAnswer(false);
                        answers.add(booleanAnswer);
                    }
                    return true;
                } else {
                    Toast.makeText(context, "Please enter 'true' or 'false' for question: "+i+1, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(context, "Please enter a valid integer for question: "+i+1, Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(context, "Please enter a valid float number for question: "+i+1, Toast.LENGTH_SHORT).show();
                    return false;
                }

            default:
                Toast.makeText(context, "Unknown question type.", Toast.LENGTH_SHORT).show();
                return false;
        }


        }

}