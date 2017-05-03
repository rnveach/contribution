package com.github.checkstyle.ci.yml;

public final class CiYml {
    private String box;
    private CiBuildYml build;

    // ///////////////////////////////////////////////////////////////////////////////////////

    public String getBox() {
        return box;
    }

    public void setBox(String box) {
        this.box = box;
    }

    public CiBuildYml getBuild() {
        return build;
    }

    public void setBuild(CiBuildYml build) {
        this.build = build;
    }
}
