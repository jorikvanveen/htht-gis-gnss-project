package com.example.gnss;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.OutputStream;

public class AdjustLocationActivity extends AppCompatActivity {

    // UI elements and location variables
    private MapView adjustMapView;
    private Marker draggableMarker;
    private double latitude;
    private double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        setContentView(R.layout.activity_adjust_location);

        // Retrieve the latitude and longitude passed from the MainActivity
        latitude = getIntent().getDoubleExtra("latitude", 0.0);
        longitude = getIntent().getDoubleExtra("longitude", 0.0);

        // Set up the map view for location adjustment
        adjustMapView = findViewById(R.id.adjustMap);
        adjustMapView.setMultiTouchControls(true);
        adjustMapView.getController().setZoom(20);  // Zoom in for precise location adjustments

        // Create a GeoPoint object to center the map on the initial location
        GeoPoint initialPoint = new GeoPoint(latitude, longitude);
        adjustMapView.getController().setCenter(initialPoint);

        // Add a draggable marker to the map
        draggableMarker = new Marker(adjustMapView);
        draggableMarker.setTitle("DRAG");  // Tooltip for the marker
        draggableMarker.setPosition(initialPoint);  // Set initial position
        draggableMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);  // Anchor the marker at the bottom center
        draggableMarker.setDraggable(true);  // Make the marker draggable by the user
        adjustMapView.getOverlays().add(draggableMarker);  // Add the marker to the map

        // Set up a button to confirm the adjusted location
        Button confirmButton = findViewById(R.id.confirm_button);
        confirmButton.setOnClickListener(v -> {
            // Get the adjusted position of the marker
            GeoPoint adjustedPoint = (GeoPoint) draggableMarker.getPosition();
            double adjustedLatitude = adjustedPoint.getLatitude();
            double adjustedLongitude = adjustedPoint.getLongitude();

            // Remove the marker from the map once the location is confirmed
            adjustMapView.getOverlays().remove(draggableMarker);

            // Save the adjusted location to a CSV file
            saveLocationToCSV(adjustedLongitude, adjustedLatitude);

            finish();  // Close the activity and return to MainActivity
        });
    }

    /**
     * Saves the adjusted location (latitude and longitude) to a CSV file in the Downloads folder.
     *
     * @param longitude The longitude of the adjusted location.
     * @param latitude  The latitude of the adjusted location.
     */
    private void saveLocationToCSV(double longitude, double latitude) {
        // Prepare the CSV data as a string
        String csvData = "Latitude,Longitude\n" + latitude + "," + longitude;

        // Define the content values for the CSV file
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "coordinates.csv");  // File name
        values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");  // File type (CSV)
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/");  // Save the file in the Downloads directory

        // Get the ContentResolver to handle the file insertion
        ContentResolver resolver = getContentResolver();
        Uri uri = null;

        // For Android 10 (API level 29) and above, use MediaStore to create the file
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        }

        // If the URI was successfully created, write the CSV data to the file
        if (uri != null) {
            try (OutputStream outputStream = resolver.openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(csvData.getBytes());  // Write the CSV data to the file
                    Toast.makeText(this, "CSV saved to Downloads folder", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to create output", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // Handle any errors that occur during the file writing process
                e.printStackTrace();
                Toast.makeText(this, "Error saving CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "Error creating CSV file", Toast.LENGTH_SHORT).show();
        }
    }


}