package com.github.checkstyle.ci.structs;

import java.util.ArrayList;
import java.util.List;

import com.github.checkstyle.ci.utils.LogManager;

public abstract class BaseCommunicationThread extends Thread {
    protected final String id;

    protected boolean stopped = true;
    protected boolean stopping = false;
    protected long communicationId = 0;

    protected final List<BaseCommunication> messages = new ArrayList<BaseCommunication>();

    public BaseCommunicationThread() {
        id = this.getClass().getSimpleName();
    }

    @Override
    public synchronized void start() {
        stopping = false;
        stopped = false;

        super.start();
    }

    // it is up to the actual thread to check stopping and to stop itself
    public void stopThread() {
        stopping = true;

        printDisplay("Issuing Stop Command.");
        interrupt();
    }

    public boolean isStopped() {
        return stopped;
    }

    protected long getNextCommunicationId() {
        final long temp = communicationId;
        communicationId++;
        return temp;
    }

    public void postCommunication(BaseCommunication communication) {
        synchronized (messages) {
            messages.add(communication);
        }
    }

    protected boolean hasNextCommunication() {
        return messages.size() > 0;
    }

    protected BaseCommunication getNextCommunication() {
        synchronized (messages) {
            if (messages.size() > 0) {
                return messages.remove(0);
            }
            return null;
        }
    }

    @Override
    public void run() {
        printDisplay("Starting (" + Thread.currentThread().getId() + ")");
        stopped = false;

        try {
            onStarted();

            execute();
        } catch (final Exception e) {
            if (!(e instanceof InterruptedException)) {
                e.printStackTrace();
            }
        } finally {
            printDisplay("Ending");
        }

        try {
            onStopped();
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            printDisplay("Stopped");
        }

        stopped = true;
    }

    protected abstract void onStarted() throws Exception;

    protected abstract void execute() throws Exception;

    protected abstract void onStopped() throws Exception;

    protected void Sleep(double time) {
        try {
            sleep((long) (time * 1000));
        } catch (final InterruptedException e) {
            // ignore exception, early sleep termination
            printDisplay("Force Sleep Termination");
        }
    }

    protected void printDisplay(String s) {
        LogManager.printDisplay(id, s);
    }

    protected void printError(String s) {
        LogManager.printError(id, s);
    }

    protected void printException(Exception e) {
        printException(null, e);
    }

    protected void printException(String s, Exception e) {
        LogManager.printError(id, s, e);
    }
}
