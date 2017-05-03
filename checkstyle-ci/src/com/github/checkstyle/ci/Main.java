package com.github.checkstyle.ci;

import com.github.checkstyle.ci.globals.Globals;
import com.github.checkstyle.ci.utils.LogManager;

public class Main {
    public static void main(String[] arguments) throws Exception {
        LogManager.printDisplay("Main", "Starting " + Globals.PROJECT + " CI");

        WebThread.start();
        CiThread.instance.start();

        while (WebThread.SERVER.isAlive() || !CiThread.instance.isStopped()) {
            Thread.sleep(10000);
        }
    }
}
