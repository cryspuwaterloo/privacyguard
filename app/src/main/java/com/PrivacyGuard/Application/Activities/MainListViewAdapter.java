package com.PrivacyGuard.Application.Activities;

import java.util.List;

import android.content.Context;
import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.PrivacyGuard.Application.Database.AppSummary;
import com.PrivacyGuard.Application.Logger;


/**
 * Created by MAK on 17/11/2015.
 */
public class MainListViewAdapter extends BaseAdapter{
    private static final String TAG = MainListViewAdapter.class.getSimpleName();
    private List<AppSummary> list;
    private final Context context;
    private PackageManager pm;

    public MainListViewAdapter(Context context, List<AppSummary> list) {
        super();
        this.context = context;
        this.pm = context.getPackageManager();
        this.list = list;
    }

    public void updateData(List<AppSummary> list){
        this.list = list;
        this.notifyDataSetChanged();
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(R.layout.listview_main, null);
            holder = new ViewHolder();
            holder.appIcon = (ImageView)convertView.findViewById(R.id.main_appIcon);
            holder.appName = (TextView)convertView.findViewById(R.id.main_appName);
            holder.leakCount = (TextView)convertView.findViewById(R.id.main_leakCount);
            holder.leakString = (TextView)convertView.findViewById(R.id.main_appLeak);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        AppSummary app = list.get(position);
        holder.appName.setText(app.getAppName());
        holder.leakCount.setText(String.valueOf(app.getTotalLeaks()));
        holder.leakString.setText(app.getTotalLeaks() == 1 ? R.string.leak_singular : R.string.leak_plural);
        try {
            holder.appIcon.setImageDrawable(pm.getApplicationIcon(app.getPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            holder.appIcon.setImageResource(R.drawable.default_icon);
            Logger.w(TAG, e.getMessage());
        }

        return convertView;
    }

    private static class ViewHolder {
        public ImageView appIcon;
        public TextView appName;
        public TextView leakCount;
        public TextView leakString;
    }
}
