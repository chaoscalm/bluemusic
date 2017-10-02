package eu.darken.bluemusic.screens.devices;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
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
import eu.darken.bluemusic.core.bluetooth.SourceDevice;
import eu.darken.bluemusic.screens.MainActivity;
import eu.darken.bluemusic.util.Preconditions;
import eu.darken.bluemusic.util.ui.ClickableAdapter;
import eu.darken.ommvplib.injection.ComponentPresenterSupportFragment;


public class DevicesFragment extends ComponentPresenterSupportFragment<DevicesPresenter.View, DevicesPresenter, DevicesComponent>
        implements DevicesPresenter.View, ClickableAdapter.OnItemClickListener<SourceDevice> {

    @BindView(R.id.recyclerview) RecyclerView recyclerView;
    @BindView(R.id.progressbar) View progressBar;

    Unbinder unbinder;
    private DevicesAdapter adapter;

    @Override
    public Class<DevicesPresenter> getTypeClazz() {
        return DevicesPresenter.class;
    }

    @Nullable
    @Override
    public android.view.View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        android.view.View layout = inflater.inflate(R.layout.fragment_layout_devicemanager, container, false);
        unbinder = ButterKnife.bind(this, layout);
        return layout;
    }

    @Override
    public void onViewCreated(android.view.View view, @Nullable Bundle savedInstanceState) {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DevicesAdapter();
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
        final ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        Preconditions.checkNotNull(actionBar);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.label_add_device);
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
        getActivity().getSupportFragmentManager().popBackStack();
    }
}