package eu.darken.bluemusic.bluetooth.core;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import eu.darken.bluemusic.AppComponent;
import eu.darken.bluemusic.settings.core.Settings;


@Module
public class DeviceSourceModule {
    @Provides
    @AppComponent.Scope
    BluetoothSource provideDeviceSource(Context context, Settings settings) {
        return new LiveBluetoothSource(context, settings);
    }
}
