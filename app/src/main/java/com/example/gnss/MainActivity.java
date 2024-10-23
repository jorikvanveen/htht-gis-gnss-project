package com.example.gnss;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import com.esotericsoftware.kryo.kryo5.io.Input;
import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyQuestion;
import com.example.gnss.singleton.DataVault;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
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
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 1;
    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private DataVault vault;

    private final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        vault = DataVault.getInstance(this);
        LinearLayout surveyList = this.findViewById(R.id.survey_list);

        for (Survey survey : vault.surveys())  {
            LayoutInflater inflater = this.getLayoutInflater();
            View surveyPreview = inflater.inflate(R.layout.survey_preview, surveyList, false);
            TextView surveyNameView = surveyPreview.findViewById(R.id.survey_name);
            surveyNameView.setText(survey.getName());

            surveyPreview.setOnClickListener(v -> {
                Intent intent = new Intent(this, DisplayMaps.class);
                intent.putExtra("survey_id", survey.getId());
                startActivity(intent);
            });

            MaterialButton optionsButton = surveyPreview.findViewById(R.id.survey_preview_threedot);
            optionsButton.setOnClickListener(v -> {
                var popupMenu = new PopupMenu(this, optionsButton);
                popupMenu.getMenuInflater().inflate(R.menu.survey_options, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    if (item.getItemId() == R.id.action_delete_survey) {
                        vault.deleteSurvey(survey.getId());
                        try {
                            DataVault.save(this);
                        } catch (IOException e) {
                            // TODO: Handle this more elegantly
                            throw new RuntimeException(e);
                        }
                        // Refresh the list
                        recreate();
                    }

                    if (item.getItemId() == R.id.action_export_questions) {
                        try {
                            vault.exportSurvey(this, survey.getId());
                            Toast.makeText(this, "Saved survey to Downloads", Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (item.getItemId() == R.id.action_view_survey_entries) {
                        Intent intent = new Intent(this, EditEntries.class);
                        intent.putExtra("survey_id", survey.getId());
                        startActivity(intent);
                    }

                    return true;
                });
                popupMenu.show();
            });
            surveyList.addView(surveyPreview);

        }
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

    public void importSurvey(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/octet-stream"); // Specify the file type you want
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select a File"), PICK_FILE_REQUEST);
    }

    public void goToEditEntries(View view){
        Intent intent = new Intent(this, EditEntries.class);
        startActivity(intent);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                // Handle the selected file URI
                String filePath = fileUri.getPath();
                // Process the file as needed
                Log.d("GNSS", filePath);
                InputStream in;
                try {
                    in = getContentResolver().openInputStream(fileUri);
                } catch (FileNotFoundException e) {
                    Toast.makeText(this, "Failed to load file", Toast.LENGTH_SHORT);
                    return;
                }

                vault.importSurvey(new Input(in));
                try {
                    DataVault.save(this);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                // Rerender the UI to show the new survey
                recreate();
            }
        }
    }
}