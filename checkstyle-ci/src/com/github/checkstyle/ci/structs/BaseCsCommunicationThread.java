package com.github.checkstyle.ci.structs;

public abstract class BaseCsCommunicationThread extends BaseCommunicationThread {
    @Override
    protected final void execute() throws Exception {
        while (!stopping) {
            try {
                work();

                Sleep(0.1);
            } catch (final Exception e) {
                printException(e);

                try {
                    workError(e);
                } catch (final Exception e2) {
                    printException(e2);
                }
            }
        }
    }

    protected abstract void workError(Exception e) throws Exception;

    protected abstract void work() throws Exception;
}
