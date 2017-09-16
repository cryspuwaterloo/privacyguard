package ca.uwaterloo.crysp.privacyguard.Application.Fragments;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ca.uwaterloo.crysp.privacyguard.Application.Database.Traffic;
import ca.uwaterloo.crysp.privacyguard.Application.Interfaces.AppDataInterface;
import ca.uwaterloo.crysp.privacyguard.Application.Logger;
import ca.uwaterloo.crysp.privacyguard.R;

/**
 * Created by rongjunyan on 2017-08-08.
 */

public class TrafficFragment extends Fragment {
    private static final String TAG = "TrafficFragment";

    private AppDataInterface activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AppDataInterface)context;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        View view = inflater.inflate(R.layout.traffic_report_fragment, null);

        PackageManager pm = getContext().getPackageManager();
        ImageView appIcon = (ImageView)view.findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(activity.getAppPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(activity.getAppName());

        TextView trafficOutE = (TextView)view.findViewById(R.id.trafficOutE);
        TextView trafficOutNe = (TextView)view.findViewById(R.id.trafficsOutNe);
        TextView trafficInE = (TextView)view.findViewById(R.id.trafficsInE);
        TextView trafficInNe = (TextView)view.findViewById(R.id.trafficsInNe);

        trafficOutE.setText(transfer(getDataOutE()));
        trafficOutNe.setText(transfer(getDataOutNe()));
        trafficInE.setText(transfer(getDataInE()));
        trafficInNe.setText(transfer(getDataInNe()));

        return view;
    }

    private int getDataOutE(){
        int data = 0;
        List<Traffic> traffics = activity.getTraffics(true, true);
        for(Traffic traffic: traffics){
            data = data + traffic.getSize();
        }

        return data;
    }

    private int getDataOutNe(){
        int data = 0;
        List<Traffic> traffics = activity.getTraffics(false, true);
        for(Traffic traffic: traffics){
            data = data + traffic.getSize();
        }
        return data;
    }

    private int getDataInE(){
        int data = 0;
        List<Traffic> traffics = activity.getTraffics(true, false);
        for(Traffic traffic: traffics){
            data = data + traffic.getSize();
        }
        return data;
    }

    private int getDataInNe(){
        int data = 0;
        List<Traffic> traffics = activity.getTraffics(false, false);
        for(Traffic traffic: traffics){
            data = data + traffic.getSize();
        }
        return data;
    }

    private String transfer(int data){

        if(data >  1024){
            data = data /1024;
            if(data  > 1024){
                data = data /1024;
                if(data  > 1024){
                    data = data /1024;
                    return Integer.toString(data) + " GB";
                }else{
                    return Integer.toString(data) + " MB";
                }

            }else{
                return Integer.toString(data) + " KB";
            }
        }else{
            return Integer.toString(data) + " bytes";
        }
    }
}