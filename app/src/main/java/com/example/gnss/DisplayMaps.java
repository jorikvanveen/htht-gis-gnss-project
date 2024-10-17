package com.example.gnss;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import org.mapsforge.core.graphics.Bitmap;

import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.Task;

import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.map.android.view.MapView;


import org.mapsforge.core.model.LatLong;
import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
import org.mapsforge.map.android.util.AndroidUtil;
import org.mapsforge.map.layer.Layer;
import org.mapsforge.map.layer.cache.TileCache;
import org.mapsforge.map.layer.renderer.TileRendererLayer;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.rendertheme.InternalRenderTheme;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;


import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DisplayMaps extends AppCompatActivity {

    // UI elements
    private MapView mapsforgeMapView;

    private boolean isInitialised = false;

    private boolean isInitialisedForge = false;
    private org.osmdroid.views.MapView osmMapView;

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
            Intent intent = new Intent(DisplayMaps.this, AdjustLocationActivity.class);
            intent.putExtra("latitude", latitude); // Pass the current latitude to the new activity
            intent.putExtra("longitude", longitude); // Pass the current longitude to the new activity
            startActivityForResult(intent, LOCATION_ADJUST_REQUEST_CODE); // Start the activity for result
        });
    }


    /**
     * Starts a periodic check if the location services are enabled. If not, prompts the user to enable them.
     * Checks if there is an active internet connection.
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

    /** Method to switch to the online map (Openstreetmap) */
    private void switchToOnlineMap() {
        // Clear the current map from the container
        mapContainer.removeAllViews();
        loadOnlineMap();
        isInitialisedForge = false;
        //Toast.makeText(this, "Switched to online map", Toast.LENGTH_SHORT).show();
    }

    /** Method to switch to the offline map (Mapsforge) */
    private void switchToOfflineMap() {
        mapContainer.removeAllViews();
        loadOfflineMap();
        isInitialised = false;
        //Toast.makeText(this, "Switched to offline map", Toast.LENGTH_SHORT).show();
    }

    /**
     * Initializes and loads an offline map using the Mapsforge library.
     * This method sets up the Mapsforge map view, configures its properties,
     * and adds it to the UI layout once initialized.
     */
    private void loadOfflineMap() {

        // Check if the map has already been initialized. Have to use this for several reasons but mostly null pointer issues.
        if (!isInitialisedForge)  {
            isInitialisedForge = true; // Mark as initialized

            Toast.makeText(getApplicationContext(), "Initializing offline map", Toast.LENGTH_SHORT).show();

            // Proceed to initialize Mapsforge MapView only if it hasn't been created yet
            if (mapsforgeMapView == null) {
                // Initialize the Mapsforge graphics context
                AndroidGraphicFactory.createInstance(this.getApplication());

                // Create a new Mapsforge MapView instance
                mapsforgeMapView = new MapView(this);
                mapsforgeMapView.getMapScaleBar().setVisible(true); // Make the scale bar visible
                mapsforgeMapView.setClickable(true); // Enable user interactions on the map

                // Create a tile cache for the offline map
                org.mapsforge.map.android.util.AndroidUtil.createTileCache(
                        this, "mapcache", // Cache directory name
                        mapsforgeMapView.getModel().displayModel.getTileSize(), // Tile size
                        1f, // Tile cache memory multiplier
                        mapsforgeMapView.getModel().frameBufferModel.getOverdrawFactor() // Overdraw factor for rendering
                );

                // Copy the offline map file from resources to internal storage
                File mapFile = copyMapFileToInternalStorage(this, R.raw.overijssel, "overijssel.map");
                MapFile mapData = new MapFile(mapFile); // Load the map file

                // Create another tile cache for rendering tiles
                TileCache tileCache = AndroidUtil.createTileCache(this, "mapcache",
                        mapsforgeMapView.getModel().displayModel.getTileSize(), 1f,
                        mapsforgeMapView.getModel().frameBufferModel.getOverdrawFactor());

                // Initialize the tile renderer layer with the offline map data
                tileRendererLayer = new TileRendererLayer(
                        tileCache, // Tile cache
                        mapData, // Map data
                        mapsforgeMapView.getModel().mapViewPosition, // Map view position
                        false, // Whether to render background
                        true, // Whether to render tiles
                        true, // Whether to render labels
                        AndroidGraphicFactory.INSTANCE // Graphic factory instance
                );

                // Set the render theme for the tile renderer
                tileRendererLayer.setXmlRenderTheme(org.mapsforge.map.rendertheme.InternalRenderTheme.DEFAULT);

                // Add the tile renderer layer to the Mapsforge MapView
                mapsforgeMapView.getLayerManager().getLayers().add(tileRendererLayer);

                // Set the initial center and zoom level for the offline map
                mapsforgeMapView.setCenter(new LatLong(52.21833, 6.89583)); // Center on Enschede as an example (if location has not loaded yet)
                mapsforgeMapView.setZoomLevel((byte) 16); // Set zoom level to 16
            }

            // Delay the addition of the map view to the container, have to use this because the code keeps running forward even if the mapview hasnt loaded
            new Handler().postDelayed(() -> {
                if (mapsforgeMapView != null) {
                    // Add the initialized Mapsforge map view to the UI layout
                    mapContainer.addView(mapsforgeMapView);

                }
            }, 100); // Delay of 100 milliseconds
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
            isInitialised = true;

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
        }
    }


    /**
     * Copies a map file from the app's raw resources to the device's internal storage.
     * This method is used to ensure that the map file is available for use by the app.
     *Can perhaps be later used for extracting other maps?
     *
     * @param context  The context of the current state of the application or object.
     * @param rawResId The resource ID of the raw map file (e.g., R.raw.map_file).
     * @param filename The name of the file to be saved in the internal storage.
     * @return The map file stored in the app's internal storage.
     **/
    public static File copyMapFileToInternalStorage(Context context, int rawResId, String filename) {
        File mapFile = new File(context.getFilesDir(), filename);

        // If the file already exists, return it
        if (mapFile.exists()) {
            return mapFile;
        }

        // If it doesn't exist, copy it from res/raw
        try (InputStream inputStream = context.getResources().openRawResource(rawResId);
             FileOutputStream outputStream = new FileOutputStream(mapFile)) {

            byte[] buffer = new byte[1024];
            int length;

            // Read from the input stream and write to the output stream
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Ensure all data is flushed to the file
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return mapFile;
    }





    /**
     * Starts a periodic check for whether location services are enabled. If not, prompts the user to enable them.
     */
    private void startLocationCheckRunnable() {
        locationCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isLocationEnabled()) {
                    Toast.makeText(DisplayMaps.this, "Location services are disabled. Please enable them.", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    locationSettingsLauncher.launch(intent);  // Prompt the user to enable location services
                } else {
                    getCurrentLocation();  // Get current location if services are enabled
                }
                locationCheckHandler.postDelayed(this, 3000);  // Repeat check every 3 seconds
            }
        };
        locationCheckHandler.post(locationCheckRunnable);  // Start the check immediately
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
        if (isInitialised){ //Check if an instance of OSM is initialized

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

            if (isInitialisedForge) {
                if (mapsforgeMapView != null) { //Two checks for initialization
                    LatLong newLocation = new LatLong(latitude, longitude);
                    mapsforgeMapView.setCenter(newLocation);

                    //Remove existing marker from Mapsforge if it exists
                    if (currentMarker != null) {
                        mapsforgeMapView.getLayerManager().getLayers().remove(currentMarker);
                    }

                    org.osmdroid.views.MapView viewForIcon = new org.osmdroid.views.MapView(this); //Extract the icon used for OSM.
                    Marker markerForIcon = new Marker(viewForIcon);
                    // Add a new marker for the current location on Mapsforge map
                    Bitmap drawable = AndroidGraphicFactory.convertToBitmap(markerForIcon.getIcon());  // Convert the extracted icon to a usable format


                    currentMarker = new org.mapsforge.map.layer.overlay.Marker(newLocation, drawable, 0, 0);  // Create a new marker
                    
                    mapsforgeMapView.getLayerManager().getLayers().add(currentMarker);  // Add the new marker to the map
                }
            }
        }
    }




    /**
     * Checks if a marker already exists on either offline on online map depending on the connection.
     *
     * @return True if a marker is present, false otherwise. Used this because more than one
     * markers appear during updates of the mapView.
     */
    private boolean hasMarker() {
        if (isInternetAvailable()) {
            for (Overlay overlay : osmMapView.getOverlays()) {
                if (overlay instanceof Marker) {
                    return true;  // A marker exists on the map
                }
            }

        } else {
            for (Layer layer : mapsforgeMapView.getLayerManager().getLayers()) {
                if (layer instanceof org.mapsforge.map.layer.overlay.Marker) {
                    return true;  // Marker found
                }
            }

        }
        return false;
    }
}

