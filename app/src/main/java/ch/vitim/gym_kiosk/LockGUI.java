package ch.vitim.gym_kiosk;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;
import android.view.View;

import java.io.IOException;

public class LockGUI {
    private static final String KIOSK_PACKAGE = "ch.vitim.gym_kiosk";
    private static final String PLAYER_PACKAGE = "ch.vitim.gym_kiosk";
    private static final String[] APP_PACKAGES = {KIOSK_PACKAGE, PLAYER_PACKAGE};
    private DeviceAdminReceiver deviceAdminReceiver = new DeviceAdminReceiver();
    private Context context;
    public DevicePolicyManager dpm;
    private ComponentName adminName;

    LockGUI(Context context){
        this.context = context;
    }

    public void initStartTaskMode(final Activity activity){
        try {
            Runtime.getRuntime().exec("dpm set-device-owner ch.vitim.gym_kiosk/.DeviceAdminReceiver");
            Log.e("Tag", String.valueOf(Runtime.getRuntime().exec("dpm set-device-owner ch.vitim.gym_kiosk/.DeviceAdminReceiver")));

        } catch (IOException e) {
            Log.e("Access", "aaaa");
        }
        dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminName = DeviceAdminReceiver.getComponentName(context);
        dpm.setLockTaskPackages(adminName, APP_PACKAGES);
        startLockTaskMode((Activity) context);


        Intent intent = new Intent(context, MainActivity.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            onLockTaskModeEntering(context, intent);
        }

        View decorView = activity.getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    hideSystemUI(activity);
                }
            }
        });
    }

    public void hideSystemUI(Activity activity) {
        // Enables regular immersive mode.
        // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
        // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        View decorView = activity.getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    public void startLockTaskMode(Activity activity){
        hideSystemUI(activity);
        // Set an option to turn on lock task mode when starting the activity.
        ActivityOptions options = ActivityOptions.makeBasic();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            options.setLockTaskEnabled(true);
        }
        // Start our kiosk app's main activity with our lock task mode option.
        PackageManager packageManager = context.getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(KIOSK_PACKAGE);
        if (launchIntent != null) {
            activity.startActivity(launchIntent, options.toBundle());
        }
    }

    // Called just after entering lock task mode.
    public void onLockTaskModeEntering(Context context, Intent intent) {
        DevicePolicyManager dpm = deviceAdminReceiver.getManager(context);
        ComponentName admin = deviceAdminReceiver.getWho(context);

        dpm.addUserRestriction(admin, UserManager.DISALLOW_CREATE_WINDOWS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            dpm.setLockTaskFeatures(adminName,
//                    DevicePolicyManager.LOCK_TASK_FEATURE_HOME |
//                            DevicePolicyManager.LOCK_TASK_FEATURE_OVERVIEW |
//                            DevicePolicyManager.LOCK_TASK_FEATURE_GLOBAL_ACTIONS|
//                            DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS |
//                            DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO |-
//                            DevicePolicyManager.LOCK_TASK_FEATURE_KEYGUARD |
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE);
        }
    }
}
