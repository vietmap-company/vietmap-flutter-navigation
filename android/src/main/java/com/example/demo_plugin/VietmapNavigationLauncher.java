package com.example.demo_plugin;

import android.app.Activity;
import android.content.Intent;


import com.example.models.Waypoint;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public class VietmapNavigationLauncher {
    public static final String KEY_ADD_WAYPOINTS = "com.my.mapbox.broadcast.ADD_WAYPOINTS";
    public static final String KEY_STOP_NAVIGATION = "com.my.mapbox.broadcast.STOP_NAVIGATION";
    public static void startNavigation(Activity activity, List<Waypoint> wayPoints) {
        Intent navigationIntent = new Intent(activity, VietmapNavigationActivity.class);
        navigationIntent.putParcelableArrayListExtra("waypoints", (ArrayList) wayPoints);
        activity.startActivity(navigationIntent);
    }

    public static void addWayPoints(Activity activity, List<Waypoint> wayPoints) {
        Intent navigationIntent = new Intent(activity, VietmapNavigationActivity.class);
        navigationIntent.setAction(KEY_ADD_WAYPOINTS);
        navigationIntent.putExtra("isAddingWayPoints", true);
        navigationIntent.putExtra("waypoints", (Serializable) wayPoints);
        activity.sendBroadcast(navigationIntent);
    }

    public static void stopNavigation(Activity activity) {
        Intent stopIntent = new Intent();
        stopIntent.setAction(KEY_STOP_NAVIGATION);
        activity.sendBroadcast(stopIntent);
    }
}
