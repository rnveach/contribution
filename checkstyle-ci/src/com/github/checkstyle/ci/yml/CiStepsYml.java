package com.github.checkstyle.ci.yml;

import java.util.List;

public final class CiStepsYml {
    private List<CiScriptYml> script;

    // ///////////////////////////////////////////////////////////////////////////////////////

    public List<CiScriptYml> getScript() {
        return script;
    }

    public void setScript(List<CiScriptYml> script) {
        this.script = script;
    }
}
