package com.example.gnss;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyQuestion;
import com.example.gnss.singleton.DataVault;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.LayoutInflater;
import android.view.View;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.WindowCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.gnss.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

        private AppBarConfiguration appBarConfiguration;
        private ActivityMainBinding binding;

        private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            binding = ActivityMainBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            DataVault vault = DataVault.getInstance(this);
            LinearLayout surveyList = this.findViewById(R.id.survey_list);
            LayoutInflater inflater = this.getLayoutInflater();

            for (Survey survey : vault.surveys())  {
                View surveyPreview = inflater.inflate(R.layout.survey_preview, surveyList);
                TextView surveyNameView = surveyPreview.findViewById(R.id.survey_name);
                surveyNameView.setText(survey.getName());

                MaterialButton collectButton = surveyPreview.findViewById(R.id.collect_button);
                collectButton.setOnClickListener(v -> {
                    Intent intent = new Intent(this, DisplayMaps.class);
                    intent.putExtra("survey_id", survey.getId());
                    startActivity(intent);
                });
            }
        }

        public void goToDisplayMaps(View view){
            Intent intent = new Intent(this, DisplayMaps.class);
            startActivity(intent);
        }
        public void goToCreateSurvey(View view) {
            Intent intent = new Intent(this, CreateSurvey.class);
            startActivity(intent);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Location permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
                }
            }
        }


}