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
import java.util.Objects;
import java.util.UUID;

import java.text.SimpleDateFormat;
import java.util.Date;

public class SaveEntry extends AppCompatActivity {

    private Survey survey;

    private DataVault vault;

    private ArrayList<SurveyQuestion> questions;

    private LinearLayout questionContainer;
    private List<View> inputFields;
    private double latitude;
    private double longitude;

    private ArrayList<Answer> answers;


    private UUID surveyId;

    private String name;

    private String formattedDate;
    private String formattedTime;

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

        //Get the current time and date
        Date currentTime = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        formattedDate = dateFormat.format(currentTime);
        formattedTime = timeFormat.format(currentTime);

        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        surveyId = (UUID) receivedIntent.getExtras().get("survey_id");
        survey = vault.getSurvey(surveyId).get();

        questions = survey.getQuestions();

        addLatLon();

        TextView dateText = new TextView(this);
        String dateTextString = "Date " + formattedDate + " Time " + formattedTime;
        dateText.setText(dateTextString);
        questionContainer.addView(dateText);


        TextView nameText = new TextView(this);
        nameText.setText("Name");
        EditText editTextName = new EditText(this);
        String nameTextString = "Entry " + vault.getSurveyEntries(surveyId).size();
        editTextName.setText(nameTextString);

        inputFields.add(editTextName);
        questionContainer.addView(nameText);
        questionContainer.addView(editTextName);




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

            if (i == 0 ){
                boolean isAlready = false;
                name = ((EditText)inputField).getText().toString();
                ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);
                for (SurveyDataPoint entry : entries){
                    if (Objects.equals(name, entry.getName())){
                        isAlready = true;

                        break;

                    }
                }
                if (isAlready){
                    Toast.makeText(this, "Entry with name " +name + " already exists.", Toast.LENGTH_SHORT).show();
                    break;
                }
            }

            // Determine the type of input and collect the answer
            else if (inputField instanceof EditText) {
                SurveyQuestionType type = questions.get(i-1).getType();
                String answer = ((EditText) inputField).getText().toString();
                if (!validateAndRegisterAnswer(answer, type, this, i)) {
                   break;

                }
            }else if (inputField instanceof Spinner) {
                    SurveyQuestionType type = questions.get(i-1).getType();
                    String answer = ((Spinner) inputField).getSelectedItem().toString();
                if (!validateAndRegisterAnswer(answer, type, this, i)) {
                    break;
                }
            }
            if(inputFields.size() == i+1){
                SurveyDataPoint entry = new SurveyDataPoint(name, latitude, longitude, formattedDate, formattedTime,  answers);
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
                        BooleanAnswer booleanAnswer = new BooleanAnswer("True");
                        answers.add(booleanAnswer);
                    } else {
                        BooleanAnswer booleanAnswer = new BooleanAnswer("False");
                        answers.add(booleanAnswer);
                    }
                    return true;

                }else if(input.equalsIgnoreCase("undefined")){
                    BooleanAnswer booleanAnswer = new BooleanAnswer("Undefined");
                    answers.add(booleanAnswer);
                    return true;
                } else {
                    Toast.makeText(context, "Please enter 'true' or 'false' for question: "+i+1, Toast.LENGTH_SHORT).show();
                    return false;
                }

            case Integer:
                // Check if the input can be parsed as an integer
                try {

                    if (input.isEmpty()){
                        IntAnswer intAnswer = new IntAnswer(0);
                        answers.add(intAnswer);
                        return true;
                    }
                    else {
                        Integer.parseInt(input);
                        IntAnswer intAnswer = new IntAnswer(Integer.parseInt(input));
                        answers.add(intAnswer);
                        return true;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(context, "Please enter a valid integer for question: "+i+1, Toast.LENGTH_SHORT).show();
                    return false;
                }

            case Float:
                // Check if the input can be parsed as a float
                try {
                if (input.isEmpty()){
                    FloatAnswer floatAnswer = new FloatAnswer(0f);
                    answers.add(floatAnswer);
                    return true;
                }
                else {

                        Float.parseFloat(input);
                        FloatAnswer floatAnswer = new FloatAnswer(Float.parseFloat(input));
                        answers.add(floatAnswer);
                        return true;
                    }
                    }
                catch (NumberFormatException e) {
                    Toast.makeText(context, "Please enter a valid float number for question: " + i + 1, Toast.LENGTH_SHORT).show();
                    return false;
                }

            default:
                Toast.makeText(context, "Unknown question type.", Toast.LENGTH_SHORT).show();
                return false;
        }


        }

}