package com.github.checkstyle.ci;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import com.github.checkstyle.ci.CiJob.CiStage;
import com.github.checkstyle.ci.globals.Globals;
import com.github.checkstyle.ci.structs.BaseCommunication;
import com.github.checkstyle.ci.structs.BaseCsCommunicationThread;
import com.github.checkstyle.ci.utils.GitUtil;
import com.github.checkstyle.ci.yml.CiScriptYml;
import com.github.checkstyle.ci.yml.CiYml;
import net.sourceforge.yamlbeans.YamlReader;

public final class CiThread extends BaseCsCommunicationThread {
    private CiThread() {
    }

    public static final CiThread instance = new CiThread();

    private final List<WorkerThread> workers = new ArrayList<WorkerThread>();

    private BaseCommunication communication;
    private CiJob job;
    private CiYml yml;

    // ///////////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onStarted() throws Exception {
        if (!Globals.CS_DIRECTORY.exists()) {
            GitUtil.clone(Globals.REPOSITORY, Globals.CS_DIRECTORY);
        }

        for (int i = 0; i < Globals.WORKER_COUNT; i++) {
            final WorkerThread worker = new WorkerThread(i);

            workers.add(worker);
        }

        // TODO: load queue from file
        // restoreFile(Globals.QUEUE_SAVE);
    }

    @Override
    protected void onStopped() throws Exception {
        // TODO: save queue to file
        // saveFile(Globals.QUEUE_SAVE, data);
    }

    @Override
    protected void work() throws Exception {
        communication = getNextCommunication();

        if (communication == null) {
            return;
        }

        if (communication.getExtraData() == null
                || !(communication.getExtraData() instanceof CiJob)) {
            // TODO: respond as error
            return;
        }

        job = (CiJob) communication.getExtraData();

        jobStart();
        jobProcess();
        jobFinish();
    }

    @Override
    protected void workError(Exception e) throws Exception {
        if (communication == null) {
            return;
        }

        if (job != null && job.getStage() != CiStage.END) {
            // TODO: mark job as failure and write exception to log

            jobFinish();
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    private void jobStart() throws Exception {
        job.setStage(CiStage.START);

        // TODO: start log file for job

        // check for user's remote, and create it if it doesn't exist

        if (!GitUtil.hasRemote(job.getUser(), Globals.CS_DIRECTORY)) {
            GitUtil.addRemote(
                    job.getUser(),
                    Globals.REPOSITORY.replace("/" + Globals.PROJECT + "/", "/" + job.getUser()
                            + "/"), Globals.CS_DIRECTORY);
        }

        // pull in user's branch and reset to it

        GitUtil.fetch(job.getUser(), Globals.CS_DIRECTORY);
        GitUtil.resetAndClean(job.getUser(), job.getBranch(), Globals.CS_DIRECTORY);

        // find and read YML file

        final File ymlFile = new File(Globals.CS_DIRECTORY + File.separator
                + Globals.YAML_FILE_NAME);

        if (!ymlFile.exists()) {
            throw new FileNotFoundException("Couldn't find: " + ymlFile.getAbsolutePath());
        }

        final YamlReader reader = new YamlReader(new FileReader(ymlFile));
        try {
            yml = reader.read(CiYml.class);
        } finally {
            reader.close();
        }
    }

    private void jobProcess() {
        job.setStage(CiStage.MIDDLE_SINGLE);

        // TODO: log starting processes

        final List<CiScriptYml> scripts = yml.getBuild().getSteps().getScript();

        for (final CiScriptYml script : scripts) {
            if (!script.isMultiMode()) {
                submitNewWorker(script);
                waitOnWorkers();
            }
        }

        // TODO: log starting multi-mode processes

        job.setStage(CiStage.MIDDLE_MULTI);

        for (final CiScriptYml script : scripts) {
            if (script.isMultiMode()) {
                submitNewWorker(script);
            }
        }

        waitOnWorkers();

        // TODO: log finished processes
    }

    private void jobFinish() {
        job.setStage(CiStage.END);

        // TODO: end log file for job

        job = null;

        if (communication.isWaitingForResponse()) {
            // TODO
        }

        communication = null;
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    private void submitNewWorker(CiScriptYml script) {
        final boolean submit = false;

        while (!submit) {
            for (final WorkerThread thread : workers) {
                // TODO: is thread busy?
            }

            if (!submit) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    private void waitOnWorkers() {
        boolean atleastOneRunning = true;

        while (atleastOneRunning) {
            atleastOneRunning = false;

            for (final WorkerThread thread : workers) {
                // TODO: is thread busy?
            }

            if (atleastOneRunning) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    private void saveFile(File file, Object data) throws IOException {
        if (file.exists()) {
            file.delete();
        }

        final ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(
                new FileOutputStream(file)));

        try {
            out.writeObject(data);
        } finally {
            out.close();
        }
    }

    private Object restoreFile(File file) throws IOException, ClassNotFoundException {
        if (!file.exists()) {
            return null;
        }

        final ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(
                new FileInputStream(file)));
        final Object data;

        try {
            data = in.readObject();
        } finally {
            in.close();
        }

        return data;
    }
}
