package com.PrivacyGuard.Application.Database;

import android.support.annotation.NonNull;

public class DataLeak implements Comparable<DataLeak> {

    public String category;
    public String type;
    public String leakContent;
    public String timestamp;

    public DataLeak(String category, String type, String content, String timestamp){
        this.category = category;
        this.type = type;
        this.leakContent = content;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(@NonNull DataLeak leak) {
        return timestamp.compareTo(leak.timestamp);
    }
}
