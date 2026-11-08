package com.family.update;

import android.app.job.JobParameters;
import android.app.job.JobService;

/** The background check (see Updater). One thread, so a slow download never holds up the app itself. */
public class UpdateJob extends JobService {
    @Override
    public boolean onStartJob(JobParameters p) {
        boolean now = p.getJobId() == Updater.JOB_NOW;
        new Thread(() -> {
            Updater.run(getApplicationContext(), now);
            jobFinished(p, false);
        }, "updater").start();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters p) { return true; } // try again later
}
