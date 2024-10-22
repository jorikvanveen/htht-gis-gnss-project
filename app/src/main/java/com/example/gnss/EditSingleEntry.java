package com.example.gnss;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.gnss.dto.Answer;
import com.example.gnss.dto.SurveyDataPoint;
import com.example.gnss.singleton.DataVault;

import java.util.ArrayList;
import java.util.UUID;

public class EditSingleEntry extends AppCompatActivity {
    private DataVault vault;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        int entryId = getIntent().getIntExtra("entry_id", 0);
        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        UUID surveyId = (UUID) receivedIntent.getExtras().get("survey_id");

        ArrayList<SurveyDataPoint> entries = vault.getSurveyEntries(surveyId);

        SurveyDataPoint entry = entries.get(entryId);

        double latitude = entry.getLat();
        double longitude = entry.getLon();

        ArrayList<Answer> answers= entry.getAnswers();




        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_edit_single_entry);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}