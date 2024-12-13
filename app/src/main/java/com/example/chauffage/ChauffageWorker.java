package com.example.chauffage;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;


public class ChauffageWorker extends Worker {
    private Context context;
    public ChauffageWorker(
            @NonNull Context context,
            @NonNull WorkerParameters parameters) {
        super(context, parameters);
        this.context = context;
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.i("DoWork", "Affichage planifié");
        return Result.success();
    }
}
