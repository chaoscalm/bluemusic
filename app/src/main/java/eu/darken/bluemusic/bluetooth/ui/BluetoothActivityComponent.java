package eu.darken.bluemusic.bluetooth.ui;


import android.support.v4.app.Fragment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import dagger.Binds;
import dagger.Module;
import dagger.Subcomponent;
import dagger.android.support.FragmentKey;
import dagger.multibindings.IntoMap;
import eu.darken.bluemusic.bluetooth.ui.discover.DiscoverComponent;
import eu.darken.bluemusic.bluetooth.ui.discover.DiscoverFragment;
import eu.darken.ommvplib.injection.PresenterComponent;
import eu.darken.ommvplib.injection.activity.ActivityComponent;

@BluetoothActivityComponent.Scope
@Subcomponent(modules = {
        BluetoothActivityComponent.FragmentBinderModule.class
})
public interface BluetoothActivityComponent extends ActivityComponent<BluetoothActivity>, PresenterComponent<BluetoothActivityPresenter.View, BluetoothActivityPresenter> {

    @Subcomponent.Builder
    abstract class Builder extends ActivityComponent.Builder<BluetoothActivity, BluetoothActivityComponent> {

    }

    @javax.inject.Scope
    @Retention(RetentionPolicy.RUNTIME)
    @interface Scope {
    }

    @Module(subcomponents = {
            DiscoverComponent.class
    })
    abstract class FragmentBinderModule {

        @Binds
        @IntoMap
        @FragmentKey(DiscoverFragment.class)
        abstract Factory<? extends Fragment> discover(DiscoverComponent.Builder impl);

    }
}