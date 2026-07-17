package com.avtoforward.automaster.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.avtoforward.automaster.OrderCreationActivity;
import com.avtoforward.automaster.R;
import com.google.android.material.card.MaterialCardView;

public class ServicesFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        MaterialCardView cardElectrician = view.findViewById(R.id.cardElectrician);
        MaterialCardView cardMechanic = view.findViewById(R.id.cardMechanic);
        MaterialCardView cardHydraulic = view.findViewById(R.id.cardHydraulic);
        MaterialCardView cardTire = view.findViewById(R.id.cardTireService);
        MaterialCardView cardJumpStart = view.findViewById(R.id.cardJumpStart);
        MaterialCardView cardTowTruck = view.findViewById(R.id.cardTowTruck);
        MaterialCardView cardOther = view.findViewById(R.id.cardOther);

        cardElectrician.setOnClickListener(v -> openOrderCreation("Автоэлектрик"));
        cardMechanic.setOnClickListener(v -> openOrderCreation("Автомеханик"));
        cardHydraulic.setOnClickListener(v -> openOrderCreation("Гидравлик"));
        cardTire.setOnClickListener(v -> openOrderCreation("Шиномонтаж"));
        cardJumpStart.setOnClickListener(v -> openOrderCreation("Прикур-авто"));
        cardTowTruck.setOnClickListener(v -> openOrderCreation("Эвакуатор"));
        cardOther.setOnClickListener(v -> openOrderCreation("Другое"));

        return view;
    }

    private void openOrderCreation(String service) {
        Intent intent = new Intent(getActivity(), OrderCreationActivity.class);
        intent.putExtra("selected_service", service);
        startActivity(intent);
    }
}