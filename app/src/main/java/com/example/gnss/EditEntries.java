package com.example.gnss;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyDataPoint;
import com.example.gnss.singleton.DataVault;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.UUID;

public class EditEntries extends AppCompatActivity {

    private Survey survey;

    private DataVault vault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_entries);

        LinearLayout surveyList = this.findViewById(R.id.survey_list);
        LayoutInflater inflater = this.getLayoutInflater();

        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        UUID surveyId = (UUID) receivedIntent.getExtras().get("survey_id");
        survey = vault.getSurvey(surveyId).get();

        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);



        for (int j = 0; j < entries.size(); j++) {



            View surveyPreview = inflater.inflate(R.layout.survey_preview_edit_entry, surveyList);
            TextView surveyNameView = surveyPreview.findViewById(R.id.survey_name);
            String entryText = "Entry " +j;
            surveyNameView.setText(entryText);

            MaterialButton collectButton = surveyPreview.findViewById(R.id.editButton);
            final int entryIndex = j;
            Toast.makeText(this, j, Toast.LENGTH_SHORT).show();

//            collectButton.setOnClickListener(v -> {
//                Intent intent = new Intent(this, EditEntries.class);
//                intent.putExtra("survey_id", survey.getId());
//                intent.putExtra("entry_id", entryIndex);
//
//                startActivity(intent);
//
//            });
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}