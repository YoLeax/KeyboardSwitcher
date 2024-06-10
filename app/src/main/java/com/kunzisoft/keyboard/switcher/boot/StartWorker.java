package com.kunzisoft.keyboard.switcher.boot;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.kunzisoft.keyboard.switcher.KeyboardSwitcherService;

/**
 * Utility class to show keyboard button at startup
 */
public class StartWorker extends Worker {

    private final String TAG = StartWorker.class.getName();

    private final Context mContext;

    public StartWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        mContext = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            KeyboardSwitcherService.startService(mContext);
        } catch (Exception e) {
            Log.e(TAG, "Unable to start service", e);
            return Result.retry();
        }
        return Result.success();
    }
}
