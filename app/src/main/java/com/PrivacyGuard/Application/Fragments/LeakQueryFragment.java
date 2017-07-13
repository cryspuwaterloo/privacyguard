package com.PrivacyGuard.Application.Fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Database.DataLeak;
import com.PrivacyGuard.Application.Database.DatabaseHandler;
import com.PrivacyGuard.Application.Interfaces.AppDataInterface;
import com.PrivacyGuard.Application.Logger;
import com.PrivacyGuard.Plugin.LeakReport;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by lucas on 25/03/17.
 */

public class LeakQueryFragment extends Fragment {

    private static String TAG = "Test";


    private Calendar calendar = Calendar.getInstance(Locale.CANADA);

    private Date startDate;
    private Date endDate;

    private static final String FOREGROUND = "Foreground";
    private static final String BACKGROUND = "Background";
    private static final String ALL = "All";

    private static final String DATE_FORMAT_DISPLAY = "E, MMM d, yyyy";
    private static final DateFormat dateFormatDisplay = new SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.CANADA);

    private static final String DATE_FORMAT_DISPLAY_SPECIFIC = "h:mm:ss a, E, MMM d, yyyy";
    private static final DateFormat dateFormatDisplaySpecific = new SimpleDateFormat(DATE_FORMAT_DISPLAY_SPECIFIC, Locale.CANADA);

    private ListAdapter listAdapter;
    private TextView totalNumber;
    private View progressBar;
    private ImageButton query;
    private Button share;

    private AppDataInterface activity;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = (AppDataInterface)context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        calendar.setTime(new Date());
        startDate = getStartOfDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
        endDate = getEndOfDay(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DATE));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.leak_query_fragment, null);

        ListView leaksList = (ListView)view.findViewById(R.id.list_view);
        leaksList.addHeaderView(LayoutInflater.from(getContext()).inflate(R.layout.query_list_header, null, false));
        listAdapter = new ListAdapter(getContext(), new ArrayList<DataLeak>());
        leaksList.setAdapter(listAdapter);
        leaksList.addFooterView(LayoutInflater.from(getContext()).inflate(R.layout.query_list_footer, null, false));

        totalNumber = (TextView)view.findViewById(R.id.total_number);
        progressBar = view.findViewById(R.id.progress_bar);

        PackageManager pm = getContext().getPackageManager();
        ImageView appIcon = (ImageView)view.findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(activity.getAppPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(activity.getAppName());

        final Spinner spinnerCategory = (Spinner) view.findViewById(R.id.spinner_category);
        ArrayAdapter<CharSequence> adapterCategory = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_category_values, R.layout.simple_spinner_item);
        adapterCategory.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapterCategory);

        final Spinner spinnerStatus = (Spinner) view.findViewById(R.id.spinner_status);
        ArrayAdapter<CharSequence> adapterStatus = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_status_values, R.layout.simple_spinner_item);
        adapterStatus.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapterStatus);

        final TextView startEditText = (TextView)view.findViewById(R.id.start_date);
        final TextView endEditText = (TextView)view.findViewById(R.id.end_date);
        ImageView startDateCalendar = (ImageView)view.findViewById(R.id.start_date_calendar);
        ImageView endDateCalendar = (ImageView)view.findViewById(R.id.end_date_calendar);

        startEditText.setText(dateFormatDisplay.format(startDate));
        endEditText.setText(dateFormatDisplay.format(endDate));

        final DatePickerDialog.OnDateSetListener startDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                startDate = getStartOfDay(year, month, dayOfMonth);
                startEditText.setText(dateFormatDisplay.format(startDate));
            }
        };

        final DatePickerDialog.OnDateSetListener endDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                endDate = getEndOfDay(year, month, dayOfMonth);
                endEditText.setText(dateFormatDisplay.format(endDate));
            }
        };

        startDateCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.setTime(startDate);
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), startDateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        endDateCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                calendar.setTime(endDate);
                DatePickerDialog dialog = new DatePickerDialog(getActivity(), endDateListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                dialog.show();
            }
        });

        query = (ImageButton)view.findViewById(R.id.query);
        query.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listAdapter.clear();
                listAdapter.notifyDataSetChanged();

                totalNumber.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                query.setEnabled(false);
                query.setAlpha(0.3f);

                new LoadQueryData().execute(spinnerCategory.getSelectedItem().toString(),
                        spinnerStatus.getSelectedItem().toString());
            }
        });

        share = (Button)view.findViewById(R.id.Share);
        share.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                int size = listAdapter.getCount();
                String message = "All " + size + " Results:\n\n";

                for(int i=0 ; i<listAdapter.getCount() ; i++){
                    DataLeak data = listAdapter.getItem(i);
                    message = message + "Application: " + data.getAppName() + "\n";
                    message = message + "Category: " + data.getCategory().toLowerCase() + "\n";
                    message = message + "Type: " + data.getType() + "\n";
                    message = message + "Time: " + dateFormatDisplaySpecific.format(data.getTimestampDate()) + "\n";
                    message = message + "Content: " + data.getLeakContent() + "\n";
                    message = message + "Status: " + (data.getForegroundStatus() == DatabaseHandler.FOREGROUND_STATUS ? FOREGROUND : BACKGROUND)
                            + "\n\n";
                }
                sendEmail(message);

            }


        });


        return view;
    }


    protected void sendEmail(String message) {
        String[] TO = {""};
        String[] CC = {""};
        Intent emailIntent = new Intent(Intent.ACTION_SEND);

        emailIntent.setData(Uri.parse("mailto:"));
        emailIntent.setType("text/plain");
        emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
        emailIntent.putExtra(Intent.EXTRA_CC, CC);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Your subject");
        emailIntent.putExtra(Intent.EXTRA_TEXT, message);

        try {
            startActivity(Intent.createChooser(emailIntent, "Send mail..."));
        } catch (android.content.ActivityNotFoundException ex) {
            Logger.d(TAG, "error");
        }
    }


    private class ListAdapter extends ArrayAdapter<DataLeak> {
        public ListAdapter(Context context, ArrayList<DataLeak> leaks) {
            super(context, 0, leaks);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            DataLeak dataLeak = getItem(position);

            // Check if an existing view is being reused, otherwise inflate the view
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.data_leak, parent, false);
            }

            TextView appNameText = (TextView)convertView.findViewById(R.id.app_name);
            TextView categoryText = (TextView)convertView.findViewById(R.id.category);
            TextView typeText = (TextView)convertView.findViewById(R.id.type);
            TextView timeStampText = (TextView)convertView.findViewById(R.id.time_stamp);
            TextView contentText = (TextView)convertView.findViewById(R.id.content);
            TextView statusText = (TextView)convertView.findViewById(R.id.status);

            String categoryCamelCase = dataLeak.getCategory().toLowerCase();
            categoryCamelCase = categoryCamelCase.substring(0, 1).toUpperCase() + categoryCamelCase.substring(1);

            appNameText.setText(dataLeak.getAppName());
            categoryText.setText(categoryCamelCase);
            typeText.setText(dataLeak.getType());
            timeStampText.setText(dateFormatDisplaySpecific.format(dataLeak.getTimestampDate()));
            contentText.setText(dataLeak.getLeakContent());
            statusText.setText(dataLeak.getForegroundStatus() == DatabaseHandler.FOREGROUND_STATUS ? FOREGROUND : BACKGROUND);

            return convertView;
        }
    }

    private class LoadQueryData extends AsyncTask<String, Void, ArrayList<DataLeak>> {

        @Override
        protected ArrayList<DataLeak> doInBackground(String... params) {
            List<DataLeak> leaks = new ArrayList<>();
            String category = params[0];
            String status = params[1];

            if (category.equals(ALL)) {
                for (LeakReport.LeakCategory cat : LeakReport.LeakCategory.values()) {
                    leaks.addAll(activity.getLeaks(cat));
                }
            } else {
                LeakReport.LeakCategory categoryValue = LeakReport.LeakCategory.valueOf(category.toUpperCase());
                leaks.addAll(activity.getLeaks(categoryValue));
            }

            ArrayList<DataLeak> revisedLeaks = new ArrayList<>();

            for (DataLeak dataLeak : leaks) {
                if (status.equals(FOREGROUND) && dataLeak.getForegroundStatus() != DatabaseHandler.FOREGROUND_STATUS) {
                    continue;
                }

                if (status.equals(BACKGROUND) && dataLeak.getForegroundStatus() != DatabaseHandler.BACKGROUND_STATUS) {
                    continue;
                }

                if (dataLeak.getTimestampDate().getTime() < startDate.getTime()) {
                    continue;
                }

                if (dataLeak.getTimestampDate().getTime() > endDate.getTime()) {
                    continue;
                }

                revisedLeaks.add(dataLeak);
            }

            Collections.sort(revisedLeaks, new Comparator<DataLeak>() {
                @Override
                public int compare(DataLeak lhs, DataLeak rhs) {
                    long lt = lhs.getTimestampDate().getTime();
                    long rt = rhs.getTimestampDate().getTime();
                    if (lt == rt) return 0;
                    return lt < rt ? -1 : 1;
                }
            });

            return revisedLeaks;
        }

        @Override
        protected void onPostExecute(ArrayList<DataLeak> result) {
            totalNumber.setText(String.format("%s Results", result.size()));

            listAdapter.clear();
            listAdapter.addAll(result);
            listAdapter.notifyDataSetChanged();

            totalNumber.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            query.setEnabled(true);
            query.setAlpha(1.0f);
        }
    }


    private Date getStartOfDay(int year, int month, int day) {
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date getEndOfDay(int year, int month, int day) {
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, day);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
}
