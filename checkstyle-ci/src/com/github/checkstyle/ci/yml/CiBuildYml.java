package com.github.checkstyle.ci.yml;

public final class CiBuildYml {
    private CiStepsYml steps;

    // ///////////////////////////////////////////////////////////////////////////////////////

    public CiStepsYml getSteps() {
        return steps;
    }

    public void setSteps(CiStepsYml steps) {
        this.steps = steps;
    }
}
