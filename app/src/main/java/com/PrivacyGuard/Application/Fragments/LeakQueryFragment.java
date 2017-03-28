package com.PrivacyGuard.Application.Fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.PrivacyGuard.Application.Activities.R;
import com.PrivacyGuard.Application.Interfaces.AppDataInterface;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lucas on 25/03/17.
 */

public class LeakQueryFragment extends Fragment {

    private Calendar calendar = Calendar.getInstance(Locale.CANADA);

    private Date startDate;
    private Date endDate;

    private static final String DATE_FORMAT_DISPLAY = "E, MMM d, yyyy";
    private static final DateFormat dateFormatDispay = new SimpleDateFormat(DATE_FORMAT_DISPLAY, Locale.CANADA);

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

        PackageManager pm = getContext().getPackageManager();
        ImageView appIcon = (ImageView)view.findViewById(R.id.app_icon);
        try {
            appIcon.setImageDrawable(pm.getApplicationIcon(activity.getAppPackageName()));
        } catch (PackageManager.NameNotFoundException e) {
            appIcon.setImageResource(R.drawable.default_icon);
        }

        TextView appNameText = (TextView)view.findViewById(R.id.app_name);
        appNameText.setText(activity.getAppName());

        Spinner spinnerCategory = (Spinner) view.findViewById(R.id.spinner_category);
        ArrayAdapter<CharSequence> adapterCategory = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_category_values, R.layout.simple_spinner_item);
        adapterCategory.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapterCategory);

        Spinner spinnerStatus = (Spinner) view.findViewById(R.id.spinner_status);
        ArrayAdapter<CharSequence> adapterStatus = ArrayAdapter.createFromResource(getContext(),
                R.array.spinner_status_values, R.layout.simple_spinner_item);
        adapterStatus.setDropDownViewResource(R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(adapterStatus);

        final TextView startEditText = (TextView)view.findViewById(R.id.start_date);
        final TextView endEditText = (TextView)view.findViewById(R.id.end_date);
        ImageView startDateCalendar = (ImageView)view.findViewById(R.id.start_date_calendar);
        ImageView endDateCalendar = (ImageView)view.findViewById(R.id.end_date_calendar);

        startEditText.setText(formatDisplayDate(startDate));
        endEditText.setText(formatDisplayDate(endDate));

        final DatePickerDialog.OnDateSetListener startDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                startDate = getStartOfDay(year, month, dayOfMonth);
                startEditText.setText(formatDisplayDate(startDate));
            }
        };

        final DatePickerDialog.OnDateSetListener endDateListener = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                endDate = getEndOfDay(year, month, dayOfMonth);
                endEditText.setText(formatDisplayDate(endDate));
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

        ImageButton query = (ImageButton)view.findViewById(R.id.query);

        return view;
    }

    private String formatDisplayDate(Date date) {
        return dateFormatDispay.format(date);
    }

    private Date getStartOfDay(int year, int month, int day) {
        Calendar calendar = Calendar.getInstance(Locale.CANADA);
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
        Calendar calendar = Calendar.getInstance(Locale.CANADA);
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
