package com.github.checkstyle.ci;

public final class CiJob {
    public static enum CiStage {
        START, MIDDLE_SINGLE, MIDDLE_MULTI, END;
    }

    private CiStage stage;
    private int jobNumber;

    private String user;
    private String pr;
    private String branch;

    // TODO: ???

    // ///////////////////////////////////////////////////////////////////////////////////////

    public CiStage getStage() {
        return stage;
    }

    public void setStage(CiStage stage) {
        this.stage = stage;
    }

    public int getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(int jobNumber) {
        this.jobNumber = jobNumber;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPr() {
        return pr;
    }

    public void setPr(String pr) {
        this.pr = pr;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }
}
