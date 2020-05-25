package de.serbroda;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class FFmpegProcess {

    public static final String DEFAULT_FFMPEG_EXECUTABLE = "ffmpeg";

    protected final String ffmpegExecutable;

    private Thread thread;
    private Process process;

    public FFmpegProcess() {
        this(DEFAULT_FFMPEG_EXECUTABLE);
    }

    public FFmpegProcess(String ffmpegExecutable) {
        this.ffmpegExecutable = ffmpegExecutable;
    }

    public Optional<Process> getProcess() {
        return Optional.ofNullable(process);
    }

    public boolean isRunning() {
        return process != null && process.isAlive();
    }

    public void startAsync(final List<String> commandArgs) {
        startAsync(commandArgs, null);
    }

    public void startAsync(final List<String> commandArgs, final FFmpegProcessOptions options) {
        final FFmpegProcess self = this;
        thread = new Thread(() -> {
            try {
                self.start(commandArgs, options);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public void start(final List<String> commandArgs) throws IOException, InterruptedException {
        start(commandArgs, null);
    }

    public void start(final List<String> commandArgs, final FFmpegProcessOptions options) throws IOException, InterruptedException {
        assertState();

        if (commandArgs.isEmpty()) {
            commandArgs.add(ffmpegExecutable);
        } else if (!commandArgs.get(0).endsWith("ffmpeg") && !commandArgs.get(0).endsWith("ffmpeg.exe")) {
            commandArgs.add(0, ffmpegExecutable);
        }

        FFmpegProcessOptions ensuredOptions = ensureDefaultOptions(options);

        if (ensuredOptions.isPrintMessages()) {
            System.out.println("Standard output: " + String.join(" ", commandArgs));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        processBuilder.directory(new File(ensuredOptions.getWorkDirectory()));
        process = processBuilder.start();

        // Get input streams
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        String line = "";
        String newLineCharacter = System.getProperty("line.separator");

        boolean isOutReady = false;
        boolean isErrorReady = false;
        boolean isProcessAlive = false;

        boolean isErrorOut = true;
        boolean isErrorError = true;

        if (ensuredOptions.isPrintMessages()) {
            System.out.println("Read command ");
        }
        while (process.isAlive()) {
            //Read the stdOut

            do {
                isOutReady = stdInput.ready();
                //System.out.println("OUT READY " + isOutReady);
                isErrorOut = true;
                isErrorError = true;

                if (isOutReady) {
                    line = stdInput.readLine();
                    isErrorOut = false;
                    if (ensuredOptions.isPrintMessages()) {
                        System.out.println("=====================================================================================" + line + newLineCharacter);
                    }
                }
                isErrorReady = stdError.ready();
                //System.out.println("ERROR READY " + isErrorReady);
                if (isErrorReady) {
                    line = stdError.readLine();
                    isErrorError = false;
                    if (ensuredOptions.isPrintMessages()) {
                        System.out.println("ERROR::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::" + line + newLineCharacter);
                    }

                }
                isProcessAlive = process.isAlive();
                //System.out.println("Process Alive " + isProcessAlive);
                if (!isProcessAlive) {
                    if (ensuredOptions.isPrintMessages()) {
                        System.out.println(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: Process DIE " + line + newLineCharacter);
                    }
                    line = null;
                    isErrorError = false;
                    process.waitFor();
                }

            } while (line != null);

            //Nothing else to read, lets pause for a bit before trying again
            if (ensuredOptions.isPrintMessages()) {
                System.out.println("PROCESS WAIT FOR");
            }
            process.waitFor(100, TimeUnit.MILLISECONDS);
        }
        if (ensuredOptions.isPrintMessages()) {
            System.out.println("Command finished");
        }
    }

    public void stop() {
        if (process != null) {
            process.destroy();
        }
        if (thread != null) {
            thread.stop();
        }
    }

    private void assertState() {
        if (isRunning()) {
            throw new IllegalStateException("Process is already running. Create a new instance of "
                    + this.getClass().getSimpleName() + " to record multiple streams.");
        }
    }

    private FFmpegProcessOptions ensureDefaultOptions(FFmpegProcessOptions options) {
        FFmpegProcessOptions returnOptions = options;
        if (returnOptions == null) {
            returnOptions = new FFmpegProcessOptions();
        }
        if (returnOptions.getWorkDirectory() == null) {
            returnOptions.setWorkDirectory(System.getProperty("user.dir"));
        }
        return returnOptions;
    }
}
