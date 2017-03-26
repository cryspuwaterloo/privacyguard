package com.PrivacyGuard.Application.Fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Interfaces.AppDataInterface;

/**
 * Created by lucas on 25/03/17.
 */

public class LeakQueryFragment extends Fragment {

    private AppDataInterface activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AppDataInterface)context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.leak_query_fragment, null);

        return view;
    }
}
