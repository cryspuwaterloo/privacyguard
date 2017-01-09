package com.justinhu.leaksimulator;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

/**
 * Created by justinhu on 2017-01-08.
 */

public class HomeFragment extends Fragment implements Button.OnClickListener {
    //public static final String ARG_PLANET_NUMBER = "planet_number";
    public static final String TAG = "HomeFragment";
    private OnItemSelectedListener mListener;

    public HomeFragment() {
        // Empty constructor required for fragment subclasses
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);
        String planet = "Home"; //getResources().getStringArray(R.array.planets_array)[i];
        Button location = (Button) rootView.findViewById(R.id.button_location);
        location.setOnClickListener(this);
        Button contact = (Button) rootView.findViewById(R.id.button_contact);
        Button phoneState = (Button) rootView.findViewById(R.id.button_phoneState);
        getActivity().setTitle(planet);
        return rootView;
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_location:
                mListener.onItemSelected(R.id.nav_location);
                break;
            default:
                break;
        }

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnItemSelectedListener) {
            mListener = (OnItemSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnItemSelectedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnItemSelectedListener {
        void onItemSelected(int id);
    }
}
