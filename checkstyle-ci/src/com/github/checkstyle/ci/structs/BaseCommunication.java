package com.github.checkstyle.ci.structs;

public abstract class BaseCommunication {
    protected long communicationId;
    protected BaseCommunicationThread sendingThread;
    protected boolean waitingForResponse = false;
    protected Object extraData;

    public BaseCommunication() {
        init();
    }

    protected void init() {
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    public long getCommunicationId() {
        return communicationId;
    }

    public void setCommunicationId(long communicationId) {
        this.communicationId = communicationId;
    }

    public BaseCommunicationThread getSendingThread() {
        return sendingThread;
    }

    public void setSendingThread(BaseCommunicationThread sendingThread) {
        this.sendingThread = sendingThread;
    }

    public boolean isWaitingForResponse() {
        return waitingForResponse;
    }

    public void setWaitingForResponse(boolean waitingForResponse) {
        this.waitingForResponse = waitingForResponse;
    }

    public Object getExtraData() {
        return extraData;
    }

    public void setExtraData(Object extraData) {
        this.extraData = extraData;
    }
}
