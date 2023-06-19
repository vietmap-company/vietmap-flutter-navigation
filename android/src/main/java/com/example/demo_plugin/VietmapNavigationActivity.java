package com.example.demo_plugin;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;

import com.example.models.Waypoint;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.android.gestures.MoveGestureDetector;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.location.LocationComponent;
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions;
import com.mapbox.mapboxsdk.location.engine.LocationEngine;
import com.mapbox.mapboxsdk.location.engine.LocationEngineRequest;
import com.mapbox.mapboxsdk.location.modes.CameraMode;
import com.mapbox.mapboxsdk.location.modes.RenderMode;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.listeners.NavigationListener;
import com.mapbox.services.android.navigation.ui.v5.listeners.RouteListener;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.ui.v5.route.OnRouteSelectionChangeListener;
import com.mapbox.services.android.navigation.v5.location.engine.LocationEngineProvider;
import com.mapbox.services.android.navigation.v5.location.replay.ReplayRouteLocationEngine;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationEventListener;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class VietmapNavigationActivity extends AppCompatActivity implements
        OnNavigationReadyCallback,
        ProgressChangeListener,
        NavigationListener,
        Callback<DirectionsResponse>,
        OnMapReadyCallback,
        MapboxMap.OnMapClickListener,
        MapboxMap.OnMapLongClickListener,
        MapboxMap.OnMoveListener,
        OnRouteSelectionChangeListener,
        OffRouteListener,
        RouteListener, NavigationEventListener {
    private static final int DEFAULT_CAMERA_ZOOM = 20;
    private ConstraintLayout customUINavigation;
    private NavigationView navigationView;
    private MapView mapView;
    private Point origin = Point.fromLngLat(106.675789, 10.759050);
    private Point destination = Point.fromLngLat(106.686777, 10.775056);
    private DirectionsRoute route;
    private boolean isNavigationRunning;
    private MapboxNavigation mapboxNavigation;
    private LocationEngine locationEngine;
    private NavigationMapRoute mapRoute;
    private MapboxMap mapboxMap;
    private ConstraintSet navigationMapConstraint;
    private ConstraintSet navigationMapExpandedConstraint;
    private boolean[] constraintChanged;
    private LocationComponent locationComponent;
    private ReplayRouteLocationEngine mockLocationEngine;
    private FusedLocationProviderClient fusedLocationClient;
    private int BEGIN_ROUTE_MILESTONE = 1001;
    private boolean reRoute = false;
    private boolean isArrived = false;
    private NavigationViewOptions.Builder mapviewNavigationOptions;

    private FloatingActionButton launchNavigationFab;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Mapbox.getInstance(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vietmap_navigation);
        // Thêm đoạn code sau vào hàm onCreate
        CustomNavigationNotification customNotification = new CustomNavigationNotification(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            customNotification.createNotificationChannel(this);
        }
        MapboxNavigationOptions options = MapboxNavigationOptions.builder()
                .navigationNotification(customNotification)
                .build();
        mapboxNavigation = new MapboxNavigation(this, options);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initializeViews(savedInstanceState);
        navigationView.initialize(this);
        navigationMapConstraint = new ConstraintSet();
        navigationMapConstraint.clone(customUINavigation);
        navigationMapExpandedConstraint = new ConstraintSet();
        navigationMapExpandedConstraint.clone(this, R.layout.vietmap_navigation_expand);
        constraintChanged = new boolean[]{false};
//        navigationView.initViewConfig(true);
    }

    private void initializeViews(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_vietmap_navigation);
        customUINavigation = findViewById(R.id.vietmapNavigation);
        mapView = findViewById(R.id.mapView);
        navigationView = findViewById(R.id.navigationView);
        launchNavigationFab = findViewById(R.id.launchNavigation);
        navigationView.onCreate(savedInstanceState);
        mapView.onCreate(savedInstanceState);
        launchNavigationFab.setOnClickListener(v -> {
            expandCollapse();
            launchNavigation();
        });
        mapView.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull MapboxMap mapboxMap) {
        this.mapboxMap = mapboxMap;
        mapboxMap.setStyle(new Style.Builder().fromUri("https://run.mocky.io/v3/961aaa3a-f380-46be-9159-09cc985d9326"), style -> {
            initLocationEngine();
            getCurrentLocation();
            enableLocationComponent(style);
            initMapRoute();
            Intent intent = getIntent();
            ArrayList waypoints = intent.getParcelableArrayListExtra("waypoints");
            Waypoint start = (Waypoint) waypoints.get(0);
            Waypoint end = (Waypoint) waypoints.get(1);
            destination = end.getPoint();
            fetchRoute(start.getPoint(), end.getPoint());
        });
        this.mapboxMap.addOnMapClickListener(this);
    }

    private void initLocationEngine() {
        mockLocationEngine = new ReplayRouteLocationEngine();
        locationEngine = LocationEngineProvider.getBestLocationEngine(this);
        long DEFAULT_INTERVAL_IN_MILLISECONDS = 5000;
        long DEFAULT_MAX_WAIT_TIME = 30000;
        LocationEngineRequest request = new LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
                .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(DEFAULT_MAX_WAIT_TIME)
                .build();

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mockLocationEngine.assignLastLocation(origin);
            return;
        }
    }

    private void initMapRoute() {

        mapRoute = new NavigationMapRoute(mapView, mapboxMap);
        mapRoute.setOnRouteSelectionChangeListener(this);
        mapRoute.addProgressChangeListener(new MapboxNavigation(this));
    }


    private void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            origin = Point.fromLngLat(location.getLongitude(), location.getLatitude());
                        }
                    });
            return;
        }
    }

    private void enableLocationComponent(Style style) {
        locationComponent = mapboxMap.getLocationComponent();

        if (locationComponent != null) {
            locationComponent.activateLocationComponent(
                    LocationComponentActivationOptions.builder(this, style).build()
            );
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationComponent.setLocationComponentEnabled(true);
            locationComponent.setCameraMode(CameraMode.TRACKING_GPS_NORTH);
            locationComponent.zoomWhileTracking(DEFAULT_CAMERA_ZOOM);
            locationComponent.setRenderMode(RenderMode.GPS);
            locationComponent.setLocationEngine(locationEngine);
        }
    }

    private void expandCollapse() {
        TransitionManager.beginDelayedTransition(customUINavigation);
        ConstraintSet constraint;
        if (constraintChanged[0]) {
            constraint = navigationMapConstraint;
        } else {
            constraint = navigationMapExpandedConstraint;
        }
        constraint.applyTo(customUINavigation);
        constraintChanged[0] = !constraintChanged[0];
    }

    void stopNavigationFunction() {
        navigationView.stopNavigation();
        mapboxNavigation.stopNavigation();
        launchNavigationFab.show();
    }

    @Override
    public void onCancelNavigation() {
        isNavigationRunning = false;
        expandCollapse();
        stopNavigationFunction();
    }

    @Override
    public void onNavigationFinished() {

    }

    @Override
    public void onNavigationRunning() {

    }

    @Override
    public void onRunning(boolean b) {
        isNavigationRunning = b;
    }


    @Override
    public void onNavigationReady(boolean b) {

        isNavigationRunning = b;
    }

    @Override
    public void onNewPrimaryRouteSelected(DirectionsRoute directionsRoute) {
        route = directionsRoute;
    }

    private void fetchRoute(Point origin, Point destination) {
        NavigationRoute builder = NavigationRoute.builder(this)
                .apikey("95f852d9f8c38e08ceacfd456b59059d0618254a50d3854c")
                .origin(origin)
                .destination(destination)
                .alternatives(true)
                .build();
        builder.getRoute(this);
    }

    @Override
    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
        if (validRouteResponse(response)) {
            if (reRoute) {
                route = response.body().routes().get(0);
                initNavigationOptions();
                navigationView.updateCameraRouteOverview();
                mapboxNavigation.addNavigationEventListener(this);
                mapboxNavigation.startNavigation(route);
                navigationView.startNavigation(this.mapviewNavigationOptions.build());
                reRoute = false;
                isArrived = false;
            } else {launchNavigationFab.show();
                route = response.body().routes().get(0);
                mapRoute.addRoutes(response.body().routes());
                if (isNavigationRunning) {
                    launchNavigation();
                }
            }
        }
    }

    @Override
    public void onFailure(Call<DirectionsResponse> call, Throwable t) {
        System.out.println(call);
    }

    void initNavigationOptions() {
        MapboxNavigationOptions navigationOptions = MapboxNavigationOptions.builder()
                .build();
        mapviewNavigationOptions = NavigationViewOptions.builder()
                .navigationListener(this)
                .routeListener(this)
                .navigationOptions(navigationOptions)
                .locationEngine(locationEngine)
                .shouldSimulateRoute(false)
                .progressChangeListener(progressChangeListener)
                .directionsRoute(route)
                .onMoveListener(this);
    }

    private ProgressChangeListener progressChangeListener = (location, routeProgress) -> {
        System.out.println("Progress Changing");
    };

    private boolean validRouteResponse(Response<DirectionsResponse> response) {
        return response.body() != null && !response.body().routes().isEmpty();
    }

    private void launchNavigation() {
        navigationView.setVisibility(View.VISIBLE);
        mapboxNavigation.addOffRouteListener(this);;
        launchNavigationFab.hide();
        initNavigationOptions();
        mapboxNavigation.startNavigation(route);
        navigationView.startNavigation(this.mapviewNavigationOptions.build());
        isArrived = false;navigationView.retrieveRecenterButtonOnClick();
    }

    @Override
    public void userOffRoute(Location location) {
        if (isArrived) return;
        reRoute = true;
        fetchRoute(Point.fromLngLat(location.getLongitude(), location.getLatitude()), destination);
    }

    @Override
    public void onProgressChange(Location location, RouteProgress routeProgress) {

    }

    @Override
    public boolean allowRerouteFrom(Point point) {
        return false;
    }

    @Override
    public void onOffRoute(Point point) {

    }

    @Override
    public void onRerouteAlong(DirectionsRoute directionsRoute) {

    }

    @Override
    public void onFailedReroute(String s) {

    }

    @Override
    public void onArrival() {
        if (isArrived) return;
        //Xử lý thông báo và kết thúc dẫn đường tại đây
        isArrived = true;
    }

    @Override
    public boolean onMapClick(@NonNull LatLng latLng) {
//        getCurrentLocation();
//        destination = Point.fromLngLat(latLng.getLongitude(), latLng.getLatitude());
//        if (origin != null) {
//            fetchRoute(origin, destination);
//        }
if(route==null) {
    Intent intent = getIntent();
    ArrayList waypoints = intent.getParcelableArrayListExtra("waypoints");
    Waypoint start = (Waypoint) waypoints.get(0);
    Waypoint end = (Waypoint) waypoints.get(1);
    destination = end.getPoint();
    fetchRoute(start.getPoint(), end.getPoint());
    System.out.println("-----------------------------------------------");
}
        return false;
    }

    @Override
    public void onStart() {
        super.onStart();
        navigationView.onStart();
        mapView.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        navigationView.onResume();
        mapView.onResume();
        if (locationEngine != null) {
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        navigationView.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onBackPressed() {
        stopNavigationFunction();

        if (!navigationView.onBackPressed()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        navigationView.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        navigationView.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        navigationView.onPause();
        mapView.onPause();
        if (locationEngine != null) {
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        navigationView.onStop();
        mapView.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        navigationView.onDestroy();
        mapView.onDestroy();
        if (locationEngine != null) {
        }
    }


    @Override
    public boolean onMapLongClick(@NonNull LatLng latLng) {
        return false;
    }

    @Override
    public void onMoveBegin(@NonNull MoveGestureDetector moveGestureDetector) {

    }

    @Override
    public void onMove(@NonNull MoveGestureDetector moveGestureDetector) {

    }

    @Override
    public void onMoveEnd(@NonNull MoveGestureDetector moveGestureDetector) {

    }


}