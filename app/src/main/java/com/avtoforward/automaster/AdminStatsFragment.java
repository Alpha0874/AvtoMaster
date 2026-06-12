package com.avtoforward.automaster;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.gson.JsonObject;

public class AdminStatsFragment extends Fragment {

    private TextView textTotalMasters, textOnlineMasters, textMastersOnForum;
    private TextView textTotalClients, textOnlineClients;
    private TextView textNewOrders, textCompletedOrders;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_stats, container, false);

        textTotalMasters = view.findViewById(R.id.textTotalMasters);
        textOnlineMasters = view.findViewById(R.id.textOnlineMasters);
        textMastersOnForum = view.findViewById(R.id.textMastersOnForum);
        textTotalClients = view.findViewById(R.id.textTotalClients);
        textOnlineClients = view.findViewById(R.id.textOnlineClients);
        textNewOrders = view.findViewById(R.id.textNewOrders);
        textCompletedOrders = view.findViewById(R.id.textCompletedOrders);

        loadStats();
        return view;
    }

    private void loadStats() {
        new Thread(() -> {
            JsonObject stats = PocketBaseClient.getStats();
            if (stats != null && getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    textTotalMasters.setText(getString(R.string.stat_value, stats.get("total_masters").getAsInt()));
                    textOnlineMasters.setText(getString(R.string.stat_value, stats.get("online_masters").getAsInt()));
                    textMastersOnForum.setText(getString(R.string.stat_value, stats.get("masters_on_forum").getAsInt()));
                    textTotalClients.setText(getString(R.string.stat_value, stats.get("total_clients").getAsInt()));
                    textOnlineClients.setText(getString(R.string.stat_value, stats.get("online_clients").getAsInt()));
                    textNewOrders.setText(getString(R.string.stat_value, stats.get("new_orders").getAsInt()));
                    textCompletedOrders.setText(getString(R.string.stat_value, stats.get("completed_orders").getAsInt()));
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadStats();
    }
}