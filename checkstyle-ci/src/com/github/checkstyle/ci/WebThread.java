package com.github.checkstyle.ci;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import com.github.checkstyle.ci.globals.Globals;
import com.github.checkstyle.ci.utils.LogManager;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.SimpleWebServer;
import fi.iki.elonen.WebServerPlugin;

public final class WebThread {
    private WebThread() {
    }

    // TODO: on load, read saved information (current job number)
    // TODO: on exit, save information (current job number)

    public static final String NAME = WebThread.class.getSimpleName();

    public static final SimpleWebServer SERVER = new SimpleWebServer(Globals.HOST, Globals.PORT,
            Globals.WORKING_DIRECTORY, true) {
        @Override
        public void init() {
            super.init();

            SimpleWebServer.registerPluginForMimeType(new String[] { "index.html" }, "text/html",
                    new WebServerPlugin() {
                        @Override
                        public void initialize(Map<String, String> commandLineOptions) {
                        }

                        @Override
                        public boolean canServeUri(String uri, File rootDir) {
                            return true;
                        }

                        @Override
                        public Response serveFile(String uri, Map<String, String> headers,
                                IHTTPSession session, File file, String mimeType) {
                            return WebThread.getResponse(uri, headers, session.getParms());
                        }
                    }, null);
        }
    };

    // ///////////////////////////////////////////////////////////////////////////////////////

    private static Response getResponse(String uri, Map<String, String> headers,
            Map<String, String> parameters) {
        if ("webhook".equals(uri)) {
            WebPages.doWebHook(headers, parameters);
        } else if ("restart".equals(uri)) {
            WebPages.doRestart(parameters);
        } else if ("list".equals(uri)) {
            WebPages.doList();
        } else if ("view".equals(uri)) {
            WebPages.doView(parameters);
        } else if ("shutdown".equals(uri)) {
            WebPages.doShutdown();
        } else if ("logs".equals(uri)) {

        }

        return new Response(Response.Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
                "Internal Error");
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    public static void start() throws IOException {
        if (!SERVER.isAlive()) {
            printDisplay("Starting...");

            SERVER.start();

            if (SERVER.isAlive()) {
                printDisplay("Started");
            } else {
                printError("Error Starting");
            }
        }
    }

    public static void stop() {
        if (SERVER.isAlive()) {
            printDisplay("Stopping...");

            SERVER.stop();

            printDisplay("Done");
        }
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    public static void printDisplay(String message) {
        LogManager.printDisplay(NAME, message);
    }

    public static void printError(String message) {
        LogManager.printError(NAME, message);
    }

    private static void printException(String message, Exception e) {
        LogManager.printError(NAME, message, e);
    }
}
