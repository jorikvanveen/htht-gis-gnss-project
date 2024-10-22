package com.example.gnss;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
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




        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        UUID surveyId = (UUID) receivedIntent.getExtras().get("survey_id");
        survey = vault.getSurvey(surveyId).get();

        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);




        LinearLayout surveyList = this.findViewById(R.id.survey_list);

        for (int j = 0; j < entries.size(); j++) {
            LayoutInflater inflater = this.getLayoutInflater();
            View surveyPreview = inflater.inflate(R.layout.survey_preview, surveyList, false);
            TextView surveyNameView = surveyPreview.findViewById(R.id.survey_name);
            surveyNameView.setText(survey.getName());
            final int entryIndex = j;
            String entryText = "Entry " +j;
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
                    }


                    return true;
                });
                popupMenu.show();
            });
            surveyList.addView(surveyPreview);

        }


    }
}