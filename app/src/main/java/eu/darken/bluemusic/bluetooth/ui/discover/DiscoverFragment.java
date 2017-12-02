package eu.darken.bluemusic.bluetooth.ui.discover;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import eu.darken.bluemusic.R;
import eu.darken.bluemusic.bluetooth.core.SourceDevice;
import eu.darken.bluemusic.util.Preconditions;
import eu.darken.bluemusic.util.ui.ClickableAdapter;
import eu.darken.ommvplib.injection.ComponentPresenterSupportFragment;


public class DiscoverFragment extends ComponentPresenterSupportFragment<DiscoverPresenter.View, DiscoverPresenter, DiscoverComponent>
        implements DiscoverPresenter.View, ClickableAdapter.OnItemClickListener<SourceDevice> {

    @BindView(R.id.recyclerview) RecyclerView recyclerView;
    @BindView(R.id.progressbar) View progressBar;

    Unbinder unbinder;
    DiscoverAdapter adapter;

    public static Fragment newInstance() {
        return new DiscoverFragment();
    }

    @Override
    public Class<DiscoverPresenter> getTypeClazz() {
        return DiscoverPresenter.class;
    }

    @Nullable
    @Override
    public android.view.View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.view.View layout = inflater.inflate(R.layout.fragment_layout_discover, container, false);
        unbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DiscoverAdapter();
        recyclerView.setAdapter(adapter);
        adapter.setItemClickListener(this);
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        if (unbinder != null) unbinder.unbind();
        super.onDestroyView();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        //noinspection ConstantConditions
        final ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        Preconditions.checkNotNull(actionBar);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.label_add_device);
        actionBar.setSubtitle(null);
    }

    @Override
    public void showDevices(List<SourceDevice> devices) {
        recyclerView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        adapter.setData(devices);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(View view, int position, SourceDevice item) {
        getPresenter().onAddDevice(item);
    }

    @Override
    public void showError(Throwable error) {
        Preconditions.checkNotNull(getView());
        Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();
    }

    @Override
    public void showProgress() {
        recyclerView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void closeScreen() {
        //noinspection ConstantConditions
        getActivity().finish();
    }

    @Override
    public void showUpgradeDialog() {
        //noinspection ConstantConditions
        new AlertDialog.Builder(getContext())
                .setTitle(R.string.label_premium_version)
                .setMessage(R.string.desc_premium_required_additional_devices)
                .setIcon(R.drawable.ic_stars_white_24dp)
                .setPositiveButton(R.string.action_upgrade, (dialogInterface, i) -> getPresenter().onPurchaseUpgrade(getActivity()))
                .setNegativeButton(R.string.action_cancel, (dialogInterface, i) -> {})
                .show();
    }


}
