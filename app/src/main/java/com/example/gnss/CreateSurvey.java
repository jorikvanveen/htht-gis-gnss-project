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

import com.google.android.material.button.MaterialButton;

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

    public void newQuestion(View view) {
        LinearLayout questionsList = findViewById(R.id.questionsListLayout);

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
}