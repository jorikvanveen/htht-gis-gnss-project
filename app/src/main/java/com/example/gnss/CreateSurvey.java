package com.example.gnss;

import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.LayoutDirection;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyQuestion;
import com.example.gnss.dto.SurveyQuestionType;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Optional;

import javax.annotation.Nullable;

public class CreateSurvey extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_survey);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Create first question
        newQuestion(null);
    }

    /**
     * Listens to onClick events from the "add question" button, creates the view hierarchy
     * for a new question and adds it to the vertical layout in the activity.
     * <p>
     * There is probably an easier way to to this with something like a fragment or include, but I
     * don't know how and I figured this would be a good learning exercise for the android
     * API.
     * @param view the view of the button that was clicked
     */
    public void newQuestion(@Nullable View view) {
        LinearLayout questionsList = findViewById(R.id.questionsListLayout);

        // WARNING: The order in which these are added in the view hierarchy is
        // depended on on the `deserialize()` function to find the different views
        // in the questionLayout. Any changes here should be reflected in `deserialize()`
        LinearLayout questionLayout = new LinearLayout(this);
        questionLayout.setOrientation(LinearLayout.HORIZONTAL);
        questionLayout.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                150
        ));
        questionsList.addView(questionLayout);

        EditText questionPromptInput = new EditText(this);
        questionPromptInput.setHint(R.string.survey_creation_question_name);
        questionPromptInput.setSingleLine(true);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.4f
        );
        questionPromptInput.setLayoutParams(params);
        questionLayout.addView(questionPromptInput);

        Spinner questionTypeInput = new Spinner(this);
        LinearLayout.LayoutParams spinnerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                0.6f
        );
        questionTypeInput.setLayoutParams(spinnerParams);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.question_types,
                android.R.layout.simple_spinner_dropdown_item
        );
        questionTypeInput.setAdapter(adapter);
        questionLayout.addView(questionTypeInput);

        MaterialButton deleteBtn = new MaterialButton(this);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                170,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        deleteBtn.setLayoutParams(btnParams);
        deleteBtn.setIcon(AppCompatResources.getDrawable(this, R.drawable.delete_outline));
        deleteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                questionsList.removeView(questionLayout);
            }
        });
        questionLayout.addView(deleteBtn);
    }

    /**
     * Listens to onClick events from the save button, gets all the data from the form, serializes
     * it into a Survey object, and saves the data somewhere.
     */
    public void onSave(View view) {
        Survey survey = this.deserialize();

        // TODO: Save this somewhere, somehow, and update javadoc after adding it
    }

    public Survey deserialize() {
        ArrayList<SurveyQuestion> questions = new ArrayList<>();
        LinearLayout questionsList = findViewById(R.id.questionsListLayout);

        int childCount = questionsList.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LinearLayout questionLayout = (LinearLayout) questionsList.getChildAt(i);

            EditText promptInput = (EditText) questionLayout.getChildAt(0);
            Spinner typeInput = (Spinner) questionLayout.getChildAt(1);

            String prompt = promptInput.getText().toString();
            String typeStr = typeInput.getSelectedItem().toString();

            Optional<SurveyQuestionType> maybeQuestionType = SurveyQuestionType.fromString(typeStr);
            assert maybeQuestionType.isPresent();
            SurveyQuestionType questionType = maybeQuestionType.get();

            questions.add(new SurveyQuestion(Optional.empty(), questionType, prompt));
        }

        String name = ((EditText) findViewById(R.id.createSurveyNameInput)).getText().toString();

        return new Survey(Optional.empty(), name, Optional.of(questions));
    }
}