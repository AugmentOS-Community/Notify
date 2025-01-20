package com.mentra.shownotifications;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.teamopensmartglasses.augmentoslib.AugmentOSLib;
import com.teamopensmartglasses.augmentoslib.PhoneNotification;
import com.teamopensmartglasses.augmentoslib.SmartGlassesAndroidService;
import com.teamopensmartglasses.augmentoslib.events.NotificationEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class NotificationService extends SmartGlassesAndroidService {
    public static final String TAG = "NotificationService";
    public AugmentOSLib augmentOSLib;
    private final JSONArray notificationQueue;
    private DisplayQueue displayQueue;
    private static final List<String> notificationAppBlackList = Arrays.asList(
        "youtube",
        "augment",
        "maps"
//        "facebook",
//        "instagram",
//        "tiktok",
//        "snapchat",
    );
    private final Handler callTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    public NotificationService() {
        super();
        this.notificationQueue = new JSONArray();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Create AugmentOSLib instance with context: this
        augmentOSLib = new AugmentOSLib(this);

        //setup event bus subscribers
        setupEventBusSubscribers();

        displayQueue = new DisplayQueue();

        Log.d(TAG, "Show Notifications on Glasses service started");

        completeInitialization();
    }

    protected void setupEventBusSubscribers() {
        EventBus eventBus = EventBus.getDefault();
        if (!eventBus.isRegistered(this)) {
            try {
                eventBus.register(this);
            } catch (EventBusException e) {
                Log.w("EventBus", "Subscriber already registered: " + e.getMessage());
            }
        }
    }

    public void completeInitialization() {
        Log.d(TAG, "COMPLETE CONVOSCOPE INITIALIZATION");

        displayQueue.startQueue();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Called");
        Log.d(TAG, "Deinit augmentOSLib");
        augmentOSLib.deinit();
        Log.d(TAG, "locationSystem remove");
        EventBus.getDefault().unregister(this);

        if (displayQueue != null) displayQueue.stopQueue();
        Log.d(TAG, "ran onDestroy");
        super.onDestroy();
    }

    @Subscribe
    public void onNotificationEvent(NotificationEvent event) throws JSONException {
        JSONObject notificationData = event.getNotificationData();
        Log.d(TAG, "Received event: " + notificationData.toString());
        addNotificationToQueueAndShowOnGlasses(notificationData);
    }

    private void addNotificationToQueueAndShowOnGlasses(JSONObject notification) {
        if(notification == null || notificationAppBlackList.contains(notification.optString("appName").toLowerCase())) return;

        addNotification(notification);
        String notificationString = constructNotificationString();

        callTimeoutHandler.removeCallbacks(timeoutRunnable);

        timeoutRunnable = () -> {
            // Call your desired function here
            Log.d(TAG, "Link timeout home");
            augmentOSLib.sendHomeScreen();
        };
        callTimeoutHandler.postDelayed(timeoutRunnable, 16000);

        displayQueue.addTask(new DisplayQueue.Task(() -> augmentOSLib.sendTextWall(notificationString), true, false, false));
    }

    private synchronized void addNotification(JSONObject notificationData) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = LocalDateTime.now().format(formatter);

        PhoneNotification newPhoneNotification = new PhoneNotification(
                notificationData.optString("title"),
                notificationData.optString("text"),
                notificationData.optString("appName"),
                formattedDateTime,
                UUID.randomUUID().toString()
        );

        for (int i = 0; i < notificationQueue.length(); i++) { // Remove element with same title and appName
            try {
                PhoneNotification existingPhoneNotification = (PhoneNotification) notificationQueue.get(i);
                if (existingPhoneNotification.getTitle().equals(newPhoneNotification.getTitle()) &&
                    existingPhoneNotification.getAppName().equals(newPhoneNotification.getAppName())) {
                    notificationQueue.remove(i);
                    break; // Exit loop after removing the duplicate
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error checking existing notifications in queue: " + e.getMessage());
            }
        }

        if (notificationQueue.length() >= 2) {
            notificationQueue.remove(0);
        }

        notificationQueue.put(newPhoneNotification);
        Log.d(TAG, "PhoneNotification added to queue: " + notificationData);
    }

    private String constructNotificationString() {
        StringBuilder notificationsString = new StringBuilder();

        for (int i = notificationQueue.length() - 1; i >= 0; i--) {
            try {
                PhoneNotification notification = (PhoneNotification) notificationQueue.get(i);
                String appName = notification.getAppName();
                String title = notification.getTitle();
                String text = notification.getText().replace("\n", ". ");

                String notificationString;
                if (title == null || title.isEmpty()) {
                    notificationString = String.format("%s: %s", appName, text);
                } else {
                    notificationString = String.format("%s - %s: %s", appName, title, text);
                }

                if (notificationString.length() > 50) {
                    notificationString = notificationString.substring(0, 47) + "...";
                }

                notificationsString.append(notificationString).append("\n");
            } catch (JSONException e) {
                Log.e(TAG, "Error constructing notification string at index " + i + ": " + e.getMessage());
            }
        }

        return notificationsString.toString().trim();
    }
}