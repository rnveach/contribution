package com.github.checkstyle.ci;

import java.io.File;
import java.io.IOException;

import com.github.checkstyle.ci.globals.Globals;
import com.github.checkstyle.ci.structs.BaseCsCommunicationThread;

public class WorkerThread extends BaseCsCommunicationThread {
    public final File workerDirectory;

    private final int id;

    public WorkerThread(int id) {
        this.id = id;
        workerDirectory = new File(Globals.WORKER_DIRECTORY, "worker" + id);
    }

    @Override
    protected void onStarted() throws Exception {
        if (!workerDirectory.exists()) {
            if (!workerDirectory.mkdir()) {
                throw new IOException("Failed to create: " + workerDirectory.getAbsolutePath());
            }
        }
    }

    @Override
    protected void work() throws Exception {
        // TODO Auto-generated method stub

    }

    @Override
    protected void workError(Exception e) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void onStopped() throws Exception {
    }
}
