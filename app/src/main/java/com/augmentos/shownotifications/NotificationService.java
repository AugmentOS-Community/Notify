package com.augmentos.shownotifications;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.augmentos.augmentoslib.AugmentOSLib;
import com.augmentos.augmentoslib.PhoneNotification;
import com.augmentos.augmentoslib.SmartGlassesAndroidService;
import com.augmentos.augmentoslib.events.NotificationEvent;

import org.greenrobot.eventbus.Subscribe;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class NotificationService extends SmartGlassesAndroidService {
    public static final String TAG = "NotificationService";
    private AugmentOSLib augmentOSLib;
    private final Queue<PhoneNotification> notificationQueue = new LinkedList<>();
    private static final int NOTIFICATION_DISPLAY_DURATION = 8500; // 5 seconds
    private final List<String> notificationAppBlackList = Arrays.asList("youtube", "augment", "maps");
    private final Handler callTimeoutHandler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private boolean isDisplayingNotification = false;

    public NotificationService() {
        super();
    }

    @Override
    public void setup() {
        augmentOSLib = new AugmentOSLib(this);
//        setupEventBusSubscribers();
        Log.d(TAG, "Notification Service Started");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service Destroyed");
        augmentOSLib.deinit();
//        EventBus.getDefault().unregister(this);
        callTimeoutHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

//    private void setupEventBusSubscribers() {
//        EventBus eventBus = EventBus.getDefault();
//        if (!eventBus.isRegistered(this)) {
//            try {
//        eventBus.register(this);
//            } catch (EventBusException e) {
//                Log.w(TAG, "EventBus already registered: " + e.getMessage());
//            }
//        }
//    }

    @Subscribe
    public void onNotificationEvent(NotificationEvent event) {
        Log.d(TAG, "Received event: " + event + ", " + event.text);
        PhoneNotification notif = new PhoneNotification(event.title, event.text, event.appName, event.timestamp, event.uuid);
        queueNotification(notif);
    }

    private synchronized void queueNotification(PhoneNotification notif) {
        // Only check blacklist
        for (String blacklisted : notificationAppBlackList) {
            if (notif.getAppName().toLowerCase().contains(blacklisted)) return;
        }

        // Add to the queue
        notificationQueue.offer(notif);

        // Start displaying if not already doing so
        if (!isDisplayingNotification) {
            displayNextNotification();
        }
    }

    private void displayNextNotification() {
        if (notificationQueue.isEmpty()) {
            isDisplayingNotification = false;
            // augmentOSLib.sendHomeScreen();
            return;
        }

        isDisplayingNotification = true;
        PhoneNotification notification = notificationQueue.poll();
        String notificationString = constructNotificationString(notification);

        augmentOSLib.sendTextWall(notificationString);

        // Schedule next notification display
        callTimeoutHandler.removeCallbacks(timeoutRunnable);
        timeoutRunnable = () -> displayNextNotification();
        callTimeoutHandler.postDelayed(timeoutRunnable, NOTIFICATION_DISPLAY_DURATION);
    }

    private String constructNotificationString(PhoneNotification notification) {
        String appName = notification.getAppName();
        String title = notification.getTitle();
        String text = notification.getText().replace("\n", ". ");
        int maxLength = 500;

        String prefix = (title == null || title.isEmpty()) ? appName + ": " : appName + " - " + title + ": ";
        String combinedString = prefix + text;

        if (combinedString.length() > maxLength) {
            int lengthAvailableForText = maxLength - prefix.length() - 4;
            if (lengthAvailableForText > 0 && text.length() > lengthAvailableForText) {
                text = text.substring(0, lengthAvailableForText) + "...";
            }
            combinedString = prefix + text;
        }

        return combinedString;
    }
}
