/**
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location.sample.geofencing;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Listener for geofence transition changes.
 *
 * Receives geofence transition events from Location Services in the form of an Intent containing
 * the transition type and geofence id(s) that triggered the transition. Creates a notification
 * as the output.
 */
public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "GeofenceTransitionsIS";
    NotificationManager notificationMgr;

    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public GeofenceTransitionsIntentService( ) {
        // Use the TAG to name the worker thread.
        super(TAG);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationMgr = (NotificationManager)getSystemService(
                NOTIFICATION_SERVICE);
    }

    /**
     * Handles incoming intents.
     * @param intent sent by Location Services. This Intent is provided to Location
     *               Services (inside a PendingIntent) when addGeofences() is called.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent);
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    geofencingEvent.getErrorCode());
            Log.e(TAG, errorMessage);
            return;
        }

        // Get the transition type.
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT ) {

            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails,false);
            Log.i(TAG, geofenceTransitionDetails);
        }else if( geofenceTransition == Geofence.GEOFENCE_TRANSITION_DWELL){
            // Get the geofences that were triggered. A single event can trigger multiple geofences.
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            String toBeDeletedId=triggeringGeofences.get(0).getRequestId();

            // Get the transition details as a String.
            String geofenceTransitionDetails = getGeofenceTransitionDetails(
                    this,
                    geofenceTransition,
                    triggeringGeofences
            );

            // Send notification and log the transition details.
            sendNotification(geofenceTransitionDetails,true);

            Intent localIntent = new Intent(Constants.BROADCAST_ACTION)
                            // Puts the status into the Intent
                            .putExtra(Constants.ID_TO_BE_DELETED, toBeDeletedId)
                            .addCategory(Intent.CATEGORY_DEFAULT);
            // Broadcasts the Intent to receivers in this app.
            sendBroadcast(localIntent);



            Log.i(TAG, geofenceTransitionDetails);
            //Recreate this geofence.


        }
         else {
            // Log the error.
            Log.e(TAG, getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }


        int transitionType =
                geofencingEvent.getGeofenceTransition();
        String tranTypeStr = "UNKNOWN(" + transitionType + ")";
        switch(transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                tranTypeStr = "ENTER";
                break;
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                tranTypeStr = "EXIT";
                break;
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                tranTypeStr = "DWELL";
                break;
        }



        Location triggerLoc = geofencingEvent.getTriggeringLocation();
        Log.v(TAG, "triggering location is " + triggerLoc);
        List <Geofence> triggerList =
                geofencingEvent.getTriggeringGeofences();
        String[] triggerIds = new String[triggerList.size()];
        for (int i = 0; i < triggerIds.length; i++) {
            // Grab the Id of each geofence
            triggerIds[i] = triggerList.get(i).getRequestId();
            String msg = tranTypeStr + ": " + triggerLoc.getLatitude() +
                    ", " + triggerLoc.getLongitude();
            String title = triggerIds[i];
         //   displayNotificationMessage(title, msg);
        }


    }


    private void displayNotificationMessage(String title, String message)
    {
        int notif_id = (int) (System.currentTimeMillis() & 0xFFL);
        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(false)
                .build();
        notificationMgr.notify(notif_id, notification);
    }

    /**
     * Gets transition details and returns them as a formatted string.
     *
     * @param context               The app context.
     * @param geofenceTransition    The ID of the geofence transition.
     * @param triggeringGeofences   The geofence(s) triggered.
     * @return                      The transition details formatted as String.
     */
    private String getGeofenceTransitionDetails(
            Context context,
            int geofenceTransition,
            List<Geofence> triggeringGeofences) {

        String geofenceTransitionString = getTransitionString(geofenceTransition);

        // Get the Ids of each geofence that was triggered.
        ArrayList triggeringGeofencesIdsList = new ArrayList();
        for (Geofence geofence : triggeringGeofences) {
            triggeringGeofencesIdsList.add(geofence.getRequestId());
        }
        String triggeringGeofencesIdsString = TextUtils.join(", ",  triggeringGeofencesIdsList);

        return geofenceTransitionString + ": " + triggeringGeofencesIdsString;
    }

    /**
     * Posts a notification in the notification bar when a transition is detected.
     * If the user clicks the notification, control goes to the MainActivity.
     */
    private void sendNotification(String notificationDetails, boolean isDwell) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        int notif_id = (int) (System.currentTimeMillis() & 0xFFL);


        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        if(isDwell){
            builder.setSmallIcon(R.drawable.common_signin_btn_icon_disabled_focus_dark)
                    // In a real app, you may want to use a library like Volley
                    // to decode the Bitmap.
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.common_signin_btn_icon_disabled_focus_dark))
                    .setColor(Color.RED)
                    .setContentTitle(notificationDetails)

                    .setContentText(getString(R.string.geofence_transition_notification_text))
                    .setContentIntent(notificationPendingIntent);

        }else{
            builder.setSmallIcon(R.drawable.ic_launcher)
                    // In a real app, you may want to use a library like Volley
                    // to decode the Bitmap.
                    .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                            R.drawable.ic_launcher))
                    .setColor(Color.RED)
                    .setContentTitle(notificationDetails)
                    .setContentText(getString(R.string.geofence_transition_notification_text))
                    .setContentIntent(notificationPendingIntent);
        }
        // Define the notification settings.

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);
        builder.setOngoing(false);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        Random random=new Random();

        mNotificationManager.notify(notif_id, builder.build());

/*


        Notification notification = new NotificationCompat.Builder(this)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setOngoing(false)
                .build();
        notificationMgr.notify(notif_id, notification);*/
    }

    /**
     * Maps geofence transition types to their human-readable equivalents.
     *
     * @param transitionType    A transition type constant defined in Geofence
     * @return                  A String indicating the type of transition
     */
    private String getTransitionString(int transitionType) {
        switch (transitionType) {
            case Geofence.GEOFENCE_TRANSITION_ENTER:
                return getString(R.string.geofence_transition_entered);
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                return getString(R.string.geofence_transition_exited);
            case Geofence.GEOFENCE_TRANSITION_DWELL:
                return "Dwelling:";
            default:
                return getString(R.string.unknown_geofence_transition);
        }
    }


    public interface ServiceCallbacks{
        public void deletePOI(String id);
    }


}
