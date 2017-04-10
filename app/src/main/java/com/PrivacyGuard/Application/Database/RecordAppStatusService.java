package com.PrivacyGuard.Application.Database;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by lucas on 07/04/17.
 */

public class RecordAppStatusService extends GcmTaskService {
    @Override
    public int onRunTask(TaskParams params) {
        DatabaseHandler databaseHandler = new DatabaseHandler(getApplicationContext());
        return GcmNetworkManager.RESULT_SUCCESS;
    }
}
