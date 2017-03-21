package com.PrivacyGuard.Application.Database;

import java.text.ParseException;
import java.util.Date;

public class DataLeak {

    public String category;
    public String type;
    public String leakContent;
    public String timestamp;
    public Date timestampDate;

    public DataLeak(String category, String type, String content, String timestamp){
        this.category = category;
        this.type = type;
        this.leakContent = content;
        this.timestamp = timestamp;

        try {
            this.timestampDate = DatabaseHandler.DATE_FORMAT.parse(timestamp);
        }
        catch (ParseException ex) {
            throw new RuntimeException("Invalid timestamp for DataLeak, tried to parse: " + timestamp);
        }
    }
}
