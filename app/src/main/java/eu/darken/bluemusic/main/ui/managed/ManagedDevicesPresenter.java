package eu.darken.bluemusic.main.ui.managed;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import eu.darken.bluemusic.BuildConfig;
import eu.darken.bluemusic.bluetooth.core.BluetoothSource;
import eu.darken.bluemusic.main.core.audio.AudioStream;
import eu.darken.bluemusic.main.core.audio.StreamHelper;
import eu.darken.bluemusic.main.core.database.DeviceManager;
import eu.darken.bluemusic.main.core.database.ManagedDevice;
import eu.darken.bluemusic.util.ApiHelper;
import eu.darken.bluemusic.util.iap.IAPRepo;
import eu.darken.mvpbakery.base.Presenter;
import eu.darken.mvpbakery.base.StateListener;
import eu.darken.mvpbakery.injection.ComponentPresenter;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import timber.log.Timber;

@ManagedDevicesComponent.Scope
public class ManagedDevicesPresenter extends ComponentPresenter<ManagedDevicesPresenter.View, ManagedDevicesComponent>
        implements StateListener {
    private final StreamHelper streamHelper;
    private final IAPRepo iapRepo;
    private final BluetoothSource bluetoothSource;
    private final NotificationManager notificationManager;
    private final PowerManager powerManager;
    private final Context context;
    private final PackageManager packageManager;
    private final DeviceManager deviceManager;
    private Disposable deviceSub = Disposable.disposed();
    private Disposable upgradeSub = Disposable.disposed();
    private Disposable bluetoothSub = Disposable.disposed();

    private boolean isBatterySavingHintDismissed = false;
    private boolean isAppLaunchHintDismissed = false;
    private boolean isNotificationPermissionDismissed = false;

    @Inject
    ManagedDevicesPresenter(
            Context context,
            PackageManager packageManager,
            DeviceManager deviceManager,
            StreamHelper streamHelper,
            IAPRepo iapRepo,
            BluetoothSource bluetoothSource,
            NotificationManager notificationManager,
            PowerManager powerManager
    ) {
        this.context = context;
        this.packageManager = packageManager;
        this.deviceManager = deviceManager;
        this.streamHelper = streamHelper;
        this.iapRepo = iapRepo;
        this.bluetoothSource = bluetoothSource;
        this.notificationManager = notificationManager;
        this.powerManager = powerManager;
    }

    @Override
    public void onRestoreState(@Nullable Bundle bundle) {
        if (bundle != null) {
            isBatterySavingHintDismissed = bundle.getBoolean("isBatterySavingHintDismissed");
            isAppLaunchHintDismissed = bundle.getBoolean("isAppLaunchHintDismissed");
        }
    }

    @Override
    public void onSaveState(@NotNull Bundle bundle) {
        bundle.putBoolean("isBatterySavingHintDismissed", isBatterySavingHintDismissed);
        bundle.putBoolean("isAppLaunchHintDismissed", isAppLaunchHintDismissed);
    }

    @Override
    public void onBindChange(@Nullable View view) {
        super.onBindChange(view);
        if (view != null) {
            bluetoothSub = bluetoothSource.isEnabled()
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(enabled -> onView(v -> v.displayBluetoothState(enabled)));

            upgradeSub = iapRepo.isProVersion()
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(isProVersion -> onView(v -> v.updateUpgradeState(isProVersion)));

            deviceSub = deviceManager.devices()
                    .subscribeOn(Schedulers.computation())
                    .map(managedDevices -> {
                        List<ManagedDevice> sorted = new ArrayList<>(managedDevices.values());
                        Collections.sort(sorted, (d1, d2) -> Long.compare(d2.getLastConnected(), d1.getLastConnected()));
                        return sorted;
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(devs -> onView(v -> v.displayDevices(devs)));
        } else {
            deviceSub.dispose();
            upgradeSub.dispose();
            bluetoothSub.dispose();
        }

        checkBatterySavingIssue();
        checkApplaunchIssue();
        checkNotificationPermissions();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkBatterySavingIssue() {
        Intent batterySavingIntent = new Intent();
        batterySavingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        batterySavingIntent.setAction(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);

        ResolveInfo resolveInfo = packageManager.resolveActivity(batterySavingIntent, 0);
        final boolean displayHint = ApiHelper.hasOreo()
                && !powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)
                && (!isBatterySavingHintDismissed || ApiHelper.hasAndroid13())
                && resolveInfo != null;

        onView(v -> v.displayBatteryOptimizationHint(displayHint, batterySavingIntent));
    }

    void onBatterySavingDismissed() {
        isBatterySavingHintDismissed = true;
        checkBatterySavingIssue();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkApplaunchIssue() {
        Intent overlayIntent = new Intent();
        overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        overlayIntent.setAction(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);

        final boolean displayHint = ApiHelper.hasAndroid10()
                && !isAppLaunchHintDismissed
                && !android.provider.Settings.canDrawOverlays(context);

        onView(v -> v.displayAndroid10AppLaunchHint(displayHint, overlayIntent));
    }

    void onAppLaunchHintDismissed() {
        isAppLaunchHintDismissed = true;
        checkApplaunchIssue();
    }


    @TargetApi(Build.VERSION_CODES.TIRAMISU)
    private void checkNotificationPermissions() {
        final boolean displayHint = ApiHelper.hasAndroid13()
                && !isNotificationPermissionDismissed
                && ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED;

        onView(v -> v.displayNotificationPermissionHint(displayHint));
    }

    void onNotificationPermissionsDismissed() {
        isNotificationPermissionDismissed = true;
        checkNotificationPermissions();
    }

    void onNotificationPermissionsGranted() {
        checkNotificationPermissions();
    }

    void onUpdateMusicVolume(ManagedDevice device, float percentage) {
        device.setVolume(AudioStream.Type.MUSIC, percentage);
        deviceManager.save(Collections.singleton(device))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        managedDevices -> {
                            if (!device.isActive()) return;
                            streamHelper.changeVolume(device.getStreamId(AudioStream.Type.MUSIC), device.getVolume(AudioStream.Type.MUSIC), true, 0);
                        },
                        e -> Timber.e(e, "Failed to update music volume.")
                );
    }

    void onUpdateCallVolume(ManagedDevice device, float percentage) {
        device.setVolume(AudioStream.Type.CALL, percentage);
        deviceManager.save(Collections.singleton(device))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        managedDevices -> {
                            if (!device.isActive()) return;
                            streamHelper.changeVolume(device.getStreamId(AudioStream.Type.CALL), device.getVolume(AudioStream.Type.CALL), true, 0);
                        },
                        e -> Timber.e(e, "Failed to update call volume.")
                );
    }

    void onUpdateRingVolume(ManagedDevice device, float percentage) {
        device.setVolume(AudioStream.Type.RINGTONE, percentage);
        deviceManager.save(Collections.singleton(device))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        managedDevices -> {
                            if (!device.isActive()) return;
                            if (ApiHelper.hasMarshmallow() && !notificationManager.isNotificationPolicyAccessGranted()) {
                                Timber.w("Tried to set ring volume but notification policy permissions were missing.");
                            } else {
                                streamHelper.changeVolume(device.getStreamId(AudioStream.Type.RINGTONE), device.getVolume(AudioStream.Type.RINGTONE), true, 0);
                            }
                        },
                        e -> Timber.e(e, "Failed to update ring volume.")
                );
    }

    void onUpdateNotificationVolume(ManagedDevice device, float percentage) {
        device.setVolume(AudioStream.Type.NOTIFICATION, percentage);
        deviceManager.save(Collections.singleton(device))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        managedDevices -> {
                            if (!device.isActive()) return;
                            if (ApiHelper.hasMarshmallow() && !notificationManager.isNotificationPolicyAccessGranted()) {
                                Timber.w("Tried to set notification volume but notification policy permissions were missing.");
                            } else {
                                streamHelper.changeVolume(device.getStreamId(AudioStream.Type.NOTIFICATION), device.getVolume(AudioStream.Type.NOTIFICATION), true, 0);
                            }
                        },
                        e -> Timber.e(e, "Failed to update notification volume.")
                );
    }

    void onUpdateAlarmVolume(ManagedDevice device, float percentage) {
        device.setVolume(AudioStream.Type.ALARM, percentage);
        deviceManager.save(Collections.singleton(device))
                .subscribeOn(Schedulers.computation())
                .subscribe(
                        managedDevices -> {
                            if (!device.isActive()) return;
                            streamHelper.changeVolume(device.getStreamId(AudioStream.Type.ALARM), device.getVolume(AudioStream.Type.ALARM), true, 0);
                        },
                        e -> Timber.e(e, "Failed to update alarm volume.")
                );
    }

    void onUpgradeClicked(Activity activity) {
        iapRepo.buyProVersion(activity);
    }

    void showBluetoothSettingsScreen() {
        Intent intent = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
        } catch (Exception e) {
            Timber.e(e, "Failed to launch Bluetooth settings screen");
        }
    }

    interface View extends Presenter.View {
        void updateUpgradeState(boolean isProVersion);

        void displayDevices(List<ManagedDevice> managedDevices);

        void displayBluetoothState(boolean enabled);

        void displayBatteryOptimizationHint(boolean display, Intent intent);

        void displayAndroid10AppLaunchHint(boolean display, Intent intent);

        void displayNotificationPermissionHint(boolean display);
    }
}
