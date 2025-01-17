package com.mentra.shownotifications;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.teamopensmartglasses.augmentoslib.AugmentOSLib;
import com.teamopensmartglasses.augmentoslib.SmartGlassesAndroidService;
import com.teamopensmartglasses.augmentoslib.events.NotificationEvent;
import com.teamopensmartglasses.augmentoslib.events.SpeechRecOutputEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.EventBusException;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class NotificationService extends SmartGlassesAndroidService {
    public static final String TAG = "NotificationService";

    public AugmentOSLib augmentOSLib;

    private DisplayQueue displayQueue;

    public NotificationService() {
        super();
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

    public void completeInitialization(){
        Log.d(TAG, "COMPLETE CONVOSCOPE INITIALIZATION");

        displayQueue.startQueue();
    }

    @Override
    public void onDestroy(){
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
    public void onNotificationEvent(NotificationEvent event) {
        JSONObject notificationData = event.notificationData;
        Log.d(TAG, "Received event: " + notificationData.toString());
//        displayQueue.addTask(new DisplayQueue.Task(() -> augmentOSLib.sendDoubleTextWall(finalLiveCaptionString, ""), true, false, true));
//        debounceAndShowTranscriptOnGlasses(text, isFinal);
    }

}
