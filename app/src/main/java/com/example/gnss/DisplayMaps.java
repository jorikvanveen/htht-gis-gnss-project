package com.example.gnss;


import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Toast;



import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.example.gnss.dto.Survey;
import com.example.gnss.dto.SurveyQuestion;
import com.example.gnss.singleton.DataVault;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import org.mapsforge.map.android.view.MapView;


import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.modules.IArchiveFile;
import org.osmdroid.tileprovider.modules.OfflineTileProvider;
import org.osmdroid.tileprovider.tilesource.FileBasedTileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.tileprovider.util.SimpleRegisterReceiver;
import org.osmdroid.util.GeoPoint;


import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class DisplayMaps extends AppCompatActivity {

    // UI elements
    private MapView mapsforgeMapView;

    private boolean isInitialised = false;

    private boolean isInitialisedOffline = false;
    private org.osmdroid.views.MapView osmMapView;

    private org.osmdroid.views.MapView MBTilesMapView;

    private TileRendererLayer tileRendererLayer;

    private FusedLocationProviderClient fusedLocationClient;
    private Marker marker;

    private FrameLayout mapContainer;

    private double latitude;
    private double longitude;

    private org.mapsforge.map.layer.overlay.Marker currentMarker;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private static final int LOCATION_ADJUST_REQUEST_CODE = 100;

    private final Handler locationCheckHandler = new Handler();
    private Runnable locationCheckRunnable;

    private boolean isPaused = false;

    private Survey survey;

    private DataVault vault;




    // Launcher for location settings activity
    private final ActivityResultLauncher<Intent> locationSettingsLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (isLocationEnabled()) {
                            getCurrentLocation();
                        } else {
                            Toast.makeText(this, "Location services are still disabled.", Toast.LENGTH_SHORT).show();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Call the superclass method to perform the default initialization

        vault = DataVault.getInstance(this);
        Intent receivedIntent = getIntent();
        UUID surveyId = (UUID) receivedIntent.getExtras().get("survey_id");
        survey = vault.getSurvey(surveyId).get();

        ArrayList<SurveyQuestion> questions = survey.getQuestions();

        for(SurveyQuestion question : questions){
            Toast.makeText(this, question.getPrompt(), Toast.LENGTH_SHORT).show();
        }



        // Initialize OSMDroid configuration for map functionality
        Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
        setContentView(R.layout.display_maps); // Set the content view to the display_maps layout

        // Create an instance of AndroidGraphicFactory for the Mapsforge library
        AndroidGraphicFactory.createInstance(getApplication());

        // Set up the toolbar for the activity
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initialize UI elements from the layout
        mapContainer = findViewById(R.id.map); // Container for the map view
        Button saveLocationButton = findViewById(R.id.saveLocationButton); // Button to save the location

        // Set accessibility heading for map container for improved accessibility on newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mapContainer.setAccessibilityHeading(true);
        }

        // Initialize FusedLocationProviderClient to handle location services
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Start checking if location services are enabled
        startLocationCheckRunnable();

        // Register a network callback to listen for changes in connectivity
        registerNetworkCallback();

        // Load either the online or offline map based on the current internet availability
        if (isInternetAvailable()) {
            loadOnlineMap(); // Load the online map if internet is available
        } else {
            loadOfflineMap(); // Load the offline map if internet is not available
        }

        // Set up the save location button to launch an activity for adjusting the marker
        saveLocationButton.setOnClickListener(v -> {
           //saveLocationToCSV(longitude, latitude);
           Intent intent = new Intent(DisplayMaps.this, SaveEntry.class);
           intent.putExtra("latitude", latitude);
           intent.putExtra("longitude", longitude);
           intent.putExtra("survey_id", survey.getId());
           startActivity(intent);

        });
    }


    /**
     * Starts a periodic check if the location services are enabled. If not, prompts the user to enable them.
     * Checks if there is an active internet connection.
     *
     * @return true if internet is available, false otherwise.
     */

    private boolean isInternetAvailable() {
        // Get the ConnectivityManager instance to check network state
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Retrieve the capabilities of the currently active network
        NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());

        // Return true if capabilities are not null and the network has internet capability
        return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    /**
     * Registers a network callback to monitor network connectivity changes.
     * This method listens for changes in the internet connectivity status,
     * switching between online and offline maps accordingly.
     */
    private void registerNetworkCallback() {
        // Get the ConnectivityManager instance
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Build a NetworkRequest to listen for networks that provide internet capability
        NetworkRequest networkRequest = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) // Listen for internet capabilities
                .build();

        // Register the network callback to receive updates about network availability
        cm.registerNetworkCallback(networkRequest, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                super.onAvailable(network); // Call the superclass method
                // Switch to online map when internet becomes available
                runOnUiThread(() -> switchToOnlineMap());
            }

            @Override
            public void onLost(@NonNull Network network) {
                super.onLost(network); // Call the superclass method
                // Switch to offline map when internet is lost
                runOnUiThread(() -> switchToOfflineMap());
            }
        });
    }

    /**
     * Method to switch to the online map (Openstreetmap)
     */
    private void switchToOnlineMap() {
        // Clear the current map from the container
        mapContainer.removeAllViews();
        loadOnlineMap();
        isInitialisedOffline = false;
        //Toast.makeText(this, "Switched to online map", Toast.LENGTH_SHORT).show();
    }

    /**
     * Method to switch to the offline map (Mapsforge)
     */
    private void switchToOfflineMap() {
        mapContainer.removeAllViews();
        loadOfflineMap();
        isInitialised = false;
        //Toast.makeText(this, "Switched to offline map", Toast.LENGTH_SHORT).show();
    }

    /**
     * Initializes and loads an offline map (world.mbtiles).
     * This method sets up the view for the offline map of the whole world, configures its properties,
     * and adds it to the UI layout once initialized.
     */
    private void loadOfflineMap() {

        Toast.makeText(getApplicationContext(), "Initializing offline map", Toast.LENGTH_SHORT).show();
        // Create a new MapView instance for offline map
        MBTilesMapView = new org.osmdroid.views.MapView(this);

        // Disable data connection to prevent online tile requests
        MBTilesMapView.setUseDataConnection(false);

        try {
            // Copy the world.mbtiles file from the raw resource folder to the internal storage
            File MapFile = copyFileToInternalStorage(R.raw.world, "world.mbtiles");

            // Check if the map file exists in internal storage
            if (MapFile.exists()) {
                try {
                    // Notify the user that the map file was found


                    // Initialize an OfflineTileProvider using the copied map file
                    OfflineTileProvider OfflineTileProvider = new OfflineTileProvider(
                            new SimpleRegisterReceiver(this),
                            new File[]{MapFile}
                    );

                    // Set the tile provider to the offline map provider
                    MBTilesMapView.setTileProvider(OfflineTileProvider);

                    String tileSource; // Variable to store the tile source

                    // Get the archives from the offline tile provider (i.e., the .mbtiles file)
                    IArchiveFile[] archives = OfflineTileProvider.getArchives();

                    // If archives are available, retrieve the tile sources
                    if (archives.length > 0) {
                        Set<String> tileSources = archives[0].getTileSources();

                        // If the tile sources are available, use the first one as the source for the map
                        if (!tileSources.isEmpty()) {
                            tileSource = tileSources.iterator().next();
                            MBTilesMapView.setTileSource(FileBasedTileSource.getSource(tileSource));
                        } else {
                            // If no specific tile source is found, use the default tile source
                            MBTilesMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                        }
                    } else {
                        // If no archives are found, use the default tile source
                        MBTilesMapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
                    }

                    // Again, disable the data connection to ensure the map remains offline
                    MBTilesMapView.setUseDataConnection(false);

                    // Enable multi-touch controls (zoom, pan, etc.)
                    MBTilesMapView.setMultiTouchControls(true);

                    // Set up the map controller to adjust zoom level and map center (location)
                    IMapController mapController = MBTilesMapView.getController();
                    mapController.setZoom(11); // Set initial zoom level to 11
                    mapController.setCenter(new GeoPoint(52.2215, 6.8937)); // Set the center to specific coordinates (Enschede)

                    // Refresh the map view
                    MBTilesMapView.invalidate();

                    // Delay the addition of the map to the layout to ensure it is fully initialized
                    new Handler().postDelayed(() -> {
                        if (MBTilesMapView != null) {
                            // Add the initialized offline map view to the UI container
                            mapContainer.addView(MBTilesMapView);
                        }
                    }, 100); // Delay by 100 milliseconds to ensure smooth UI rendering

                } catch (Exception ex) {
                    // Catch and print any exceptions that occur during the offline map setup
                    ex.printStackTrace();
                }

            }

            // Mark the offline map as initialized
            isInitialisedOffline = true;

        } catch (IOException e) {
            // If there is an IO exception (file-related error), throw a runtime exception
            throw new RuntimeException(e);
        }
    }

    /**
     * Initializes and loads an online map using the OSMDroid library.
     * This method sets up the OSMDroid map view, configures its properties,
     * and adds it to the UI layout once initialized.
     * Need to figure out how to add
     */

    private void loadOnlineMap() {

        if (!isInitialised) {
            // Mark the map as initialized. Have to use this for several reasons but mostly null pointer issues.


            // Load the OSMDroid configuration from preferences
            Configuration.getInstance().load(this, getPreferences(MODE_PRIVATE));
            Toast.makeText(getApplicationContext(), "Initializing OSMDroid map", Toast.LENGTH_SHORT).show();

            // Initialize osmMapView (OSMDroid MapView)
            osmMapView = new org.osmdroid.views.MapView(this);
            osmMapView.setTileSource(TileSourceFactory.MAPNIK); // Use the online Mapnik tile source
            osmMapView.setMultiTouchControls(true);             // Enable multi-touch controls for zoom and panning
            osmMapView.getController().setZoom(16);             // Set the initial zoom level
            osmMapView.getController().setCenter(new org.osmdroid.util.GeoPoint(52.21833, 6.89583)); // Center the map to a specific GeoPoint (example: Enschede)

            // Use a Handler to delay the addition of the map view to the layout, have to use this because the code keeps running forward even if the mapview hasn't loaded
            new Handler().postDelayed(() -> {
                if (osmMapView != null) {
                    // Add the initialized map view to the container (UI layout)

                    mapContainer.addView(osmMapView);
                }
            }, 100); // Delay of 100 milliseconds
            isInitialised = true;
        }
    }


    /**
     * This method copies a file from the app's raw resources to the internal storage of the app.
     * It takes a resource ID and a desired file name as parameters. The method reads the resource
     * from the raw folder, writes it to a new file in internal storage, and returns the file object
     * representing the copied file.
     *
     * @param resourceId   The resource ID of the raw file to be copied.
     * @param resourceName The desired name for the copied file in internal storage.
     * @return The File object representing the copied file in internal storage.
     * @throws IOException If an error occurs during file input/output operations.
     */
    private File copyFileToInternalStorage(int resourceId, String resourceName) throws IOException {
        // Open an InputStream to read the raw resource specified by resourceId
        InputStream inputStream;
        inputStream = getResources().openRawResource(resourceId);

        // Create a new File object in the app's internal storage with the specified resourceName
        File file = new File(getFilesDir(), resourceName); // create file in internal storage

        // Use a FileOutputStream to write the data from the InputStream to the new file
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024]; // Buffer for reading data
            int length; // Variable to hold the number of bytes read
            // Read from the InputStream and write to the FileOutputStream until the end of the resource is reached
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length); // Write bytes to the output file
            }
        }

        // Return the File object representing the copied file
        return file;
    }


    /**
     * This method initializes and starts a runnable that periodically checks if the location services
     * are enabled on the device. If the location services are disabled, it prompts the user to enable them.
     * If the services are enabled, it retrieves the current location. The check is repeated every 3 seconds.
     */
    private void startLocationCheckRunnable() {
        // Create a new Runnable to check location services status
        locationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    // Check if the location services are enabled on the device
                    if (!isLocationEnabled()) {
                        // If location services are disabled, show a toast message to the user
                        Toast.makeText(DisplayMaps.this, "Location services are disabled. Please enable them.", Toast.LENGTH_SHORT).show();

                        // Create an intent to open the location settings on the device
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);

                        // Launch the location settings intent to prompt the user
                        locationSettingsLauncher.launch(intent);
                    } else {
                        // If location services are enabled, call the method to get the current location
                        getCurrentLocation();
                    }
                }
                // Schedule the runnable to run again after 3 seconds
                locationCheckHandler.postDelayed(this, 3000);
            }

        };

        // Start the location check runnable immediately
        locationCheckHandler.post(locationCheckRunnable);
    }

    /**
     * Stops the location check when the activity is destroyed to avoid memory leaks.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationCheckHandler.removeCallbacks(locationCheckRunnable);
        if (tileRendererLayer != null) {
            tileRendererLayer.onDestroy();
        }
        AndroidGraphicFactory.clearResourceMemoryCache();
    }

    /**
     * Checks if location services (GPS or network-based) are enabled.
     *
     * @return True if either GPS or network location provider is enabled, false otherwise.
     */
    private boolean isLocationEnabled() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * Fetches the user's current location using FusedLocationProviderClient. I believe this is the
     * most precise way (using google play services)
     * If permission is not granted, it requests the necessary permissions.
     */
    private void getCurrentLocation() {
        // Check if the ACCESS_FINE_LOCATION permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request the necessary location permission if not granted
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // If permission is granted, get the last known location
            Task<Location> locationResult = fusedLocationClient.getLastLocation();

            // Add a listener to handle the location result
            locationResult.addOnSuccessListener(location -> {
                if (location != null) {
                    // If the location is not null, extract latitude and longitude
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();

                    // Update the map location with the current coordinates
                    updateMapLocation(latitude, longitude);
                } else {

                    Toast.makeText(DisplayMaps.this, "Location not found", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    /**
     * Updates the map location and adds a marker at the specified coordinates.
     *
     * @param latitude  The latitude of the location.
     * @param longitude The longitude of the location.
     */
    private void updateMapLocation(double latitude, double longitude) {

        //OSMdroid map update
        if (isInitialised) { //Check if an instance of OSM is initialized

            if (isInternetAvailable()) { //Check if internet is available

                if (osmMapView != null) { //Another check for initialization.

                    // Update map center to current location
                    osmMapView.getController().setCenter(new GeoPoint(latitude, longitude));

                    // Remove any existing markers
                    if (marker != null && hasMarker()) {
                        osmMapView.getOverlays().remove(marker);
                    }

                    osmMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {  //Wrapper used to delay the app until a new update is made. Fixes null pointer issues
                        @Override
                        public void onGlobalLayout() {
                            // Remove the listener to prevent multiple calls
                            osmMapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                            // Update the map center to the current location
                            osmMapView.getController().setCenter(new GeoPoint(latitude, longitude));

                            // Remove any existing marker if present
                            if (marker != null && hasMarker()) {
                                osmMapView.getOverlays().remove(marker);
                            }

                            // Create a new marker for the current location
                            marker = new Marker(osmMapView);
                            marker.setPosition(new GeoPoint(latitude, longitude));
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setTitle("Current Location");
                            marker.setDraggable(true);
                            marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                                @Override
                                public void onMarkerDragStart(Marker marker) {
                                    isPaused = true;
                                }
                                @Override
                                public void onMarkerDrag(Marker marker) {
                                }
                                @Override
                                public void onMarkerDragEnd(Marker marker) {
                                    GeoPoint markerPos = marker.getPosition();
                                    setLatLong(markerPos.getLatitude(), markerPos.getLongitude());
                                    locationCheckHandler.postDelayed(() -> {

                                        isPaused = false;
                                    }, 10000);
                                }
                            });

                            // Add the marker to the map
                            osmMapView.getOverlays().add(marker);
                            // Toast.makeText(getApplicationContext(), "Marker added after map load", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    Toast.makeText(this, "osmMapView is null", Toast.LENGTH_SHORT).show();
                }
            }


        } else {

// Mapsforge map update

            if (isInitialisedOffline) {
                if (!isInternetAvailable()) { //Check if internet is available

                    if (MBTilesMapView != null) { //Another check for initialization.

                        // Update map center to current location
                        MBTilesMapView.getController().setCenter(new GeoPoint(latitude, longitude));

                        // Remove any existing markers
                        if (marker != null && hasMarker()) {
                            MBTilesMapView.getOverlays().remove(marker);
                        }

                        MBTilesMapView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {  //Wrapper used to delay the app until a new update is made. Fixes null pointer issues
                            @Override
                            public void onGlobalLayout() {
                                // Remove the listener to prevent multiple calls
                                MBTilesMapView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                                // Update the map center to the current location
                                MBTilesMapView.getController().setCenter(new GeoPoint(latitude, longitude));

                                // Remove any existing marker if present
                                if (marker != null && hasMarker()) {
                                    MBTilesMapView.getOverlays().remove(marker);
                                }

                                // Create a new marker for the current location
                                marker = new Marker(MBTilesMapView);
                                marker.setPosition(new GeoPoint(latitude, longitude));
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                marker.setTitle("Current Location");
                                marker.setDraggable(true);
                                marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                                    @Override
                                    public void onMarkerDragStart(Marker marker) {
                                        isPaused = true;
                                    }
                                    @Override
                                    public void onMarkerDrag(Marker marker) {
                                    }
                                    @Override
                                    public void onMarkerDragEnd(Marker marker) {
                                        GeoPoint markerPos = marker.getPosition();
                                        setLatLong(markerPos.getLatitude(), markerPos.getLongitude());

                                        locationCheckHandler.postDelayed(() -> {

                                            isPaused = false;
                                        }, 10000);
                                    }
                                });
                                // Add the marker to the map
                                MBTilesMapView.getOverlays().add(marker);
                                // Toast.makeText(getApplicationContext(), "Marker added after map load", Toast.LENGTH_SHORT).show();
                            }
                        });

                    } else {
                        Toast.makeText(this, "osmMapView is null", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }
    private void setLatLong(double lat, double lon){
        latitude = lat;
        longitude = lon;
    }
//                if (mapsforgeMapView != null) { //Two checks for initialization
//                    LatLong newLocation = new LatLong(latitude, longitude);
//                    mapsforgeMapView.setCenter(newLocation);
//
//                    //Remove existing marker from Mapsforge if it exists
//                    if (currentMarker != null) {
//                        mapsforgeMapView.getLayerManager().getLayers().remove(currentMarker);
//                    }
//
//                    org.osmdroid.views.MapView viewForIcon = new org.osmdroid.views.MapView(this); //Extract the icon used for OSM.
//                    Marker markerForIcon = new Marker(viewForIcon);
//                    // Add a new marker for the current location on Mapsforge map
//                    Bitmap drawable = AndroidGraphicFactory.convertToBitmap(markerForIcon.getIcon());  // Convert the extracted icon to a usable format
//
//
//                    currentMarker = new org.mapsforge.map.layer.overlay.Marker(newLocation, drawable, 0, 0);  // Create a new marker
//
//                    mapsforgeMapView.getLayerManager().getLayers().add(currentMarker);  // Add the new marker to the map
//                }
//            }


    /**
     * Checks if a marker already exists on either offline on online map depending on the connection.
     *
     * @return True if a marker is present, false otherwise. Used this because more than one
     * markers appear during updates of the mapView.
     */
    private boolean hasMarker() {
        if (isInternetAvailable()) {
            if (osmMapView != null) {
                for (Overlay overlay : osmMapView.getOverlays()) {
                    if (overlay instanceof Marker) {
                        return true;  // A marker exists on the map
                    }
                }
            }

        } else {
            if (MBTilesMapView != null) {
                for (Overlay overlay : MBTilesMapView.getOverlays()) {
                    if (overlay instanceof Marker) {
                        return true;  // A marker exists on the map
                    }
                }
            }

        }
        return false;
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
//private void loadOfflineMap() {

//       //  Check if the map has already been initialized. Have to use this for several reasons but mostly null pointer issues.
//        if (!isInitialisedOffline) {
//            isInitialisedOffline = true; // Mark as initialized
//
//            Toast.makeText(getApplicationContext(), "Initializing offline map", Toast.LENGTH_SHORT).show();
//
//            // Proceed to initialize Mapsforge MapView only if it hasn't been created yet
//            if (mapsforgeMapView == null) {
//                // Initialize the Mapsforge graphics context
//                AndroidGraphicFactory.createInstance(this.getApplication());
//
//                // Create a new Mapsforge MapView instance
//                mapsforgeMapView = new MapView(this);
//                mapsforgeMapView.getMapScaleBar().setVisible(true); // Make the scale bar visible
//                mapsforgeMapView.setClickable(true); // Enable user interactions on the map
//
//                // Create a tile cache for the offline map
//                org.mapsforge.map.android.util.AndroidUtil.createTileCache(
//                        this, "mapcache", // Cache directory name
//                        mapsforgeMapView.getModel().displayModel.getTileSize(), // Tile size
//                        1f, // Tile cache memory multiplier
//                        mapsforgeMapView.getModel().frameBufferModel.getOverdrawFactor() // Overdraw factor for rendering
//                );
//
//                // Copy the offline map file from resources to internal storage
//                //File mapFile = copyMapFileToInternalStorage(this, R.raw.overijsse, "overijsse.map");

//                MapFile mapData = new MapFile(mapFile); // Load the map file
//
//                // Create another tile cache for rendering tiles
//                TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
//                        mapsforgeMapView.getModel().displayModel.getTileSize(), 1f,
//                        mapsforgeMapView.getModel().frameBufferModel.getOverdrawFactor());
//
//                // Initialize the tile renderer layer with the offline map data
//                tileRendererLayer = new TileRendererLayer(
//                        tileCache, // Tile cache
//                        mapData, // Map data
//                        mapsforgeMapView.getModel().mapViewPosition, // Map view position
//                        false, // Whether to render background
//                        true, // Whether to render tiles
//                        true, // Whether to render labels
//                        AndroidGraphicFactory.INSTANCE // Graphic factory instance
//                );
//
//                // Set the render theme for the tile renderer
//                tileRendererLayer.setXmlRenderTheme(org.mapsforge.map.rendertheme.InternalRenderTheme.DEFAULT);
//
//                // Add the tile renderer layer to the Mapsforge MapView
//                mapsforgeMapView.getLayerManager().getLayers().add(tileRendererLayer);
//
//                // Set the initial center and zoom level for the offline map
//                mapsforgeMapView.setCenter(new LatLong(52.21833, 6.89583)); // Center on Enschede as an example (if location has not loaded yet)
//                //mapsforgeMapView.setZoomLevel((byte) 16); // Set zoom level to 16
//            }
//
//            // Delay the addition of the map view to the container, have to use this because the code keeps running forward even if the mapview hasnt loaded
//            new Handler().postDelayed(() -> {
//                if (mapsforgeMapView != null) {
//                    // Add the initialized Mapsforge map view to the UI layout
//                    mapContainer.addView(mapsforgeMapView);
//
//                }
//            }, 100); // Delay of 100 milliseconds
//        }


