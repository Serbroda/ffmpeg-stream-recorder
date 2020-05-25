package de.serbroda;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class FFmpegProcess {

    public static final String DEFAULT_FFMPEG_EXECUTABLE = "ffmpeg";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    protected final String ffmpegExecutable;

    private Thread thread;
    private Process process;
    private String currentWorkDirectory;


    public FFmpegProcess() {
        this(DEFAULT_FFMPEG_EXECUTABLE);
    }

    public FFmpegProcess(String ffmpegExecutable) {
        this.ffmpegExecutable = ffmpegExecutable;
    }

    public Optional<Process> getProcess() {
        return Optional.ofNullable(process);
    }

    public void startRecordAsync(String url) {
        startRecordAsync(url, System.getProperty("user.dir"));
    }

    public void startRecordAsync(String url, String workRootDirectory) {
        thread = new Thread(() -> startRecord(url, workRootDirectory));
        thread.start();
    }

    public void startRecord(String url) {
        startRecord(url, System.getProperty("user.dir"));
    }

    public void startRecord(String url, String workRootDirectory) {
        currentWorkDirectory = Paths.get(workRootDirectory, DATE_FORMAT.format(new Date())).toAbsolutePath().toString();

        if(!new File(currentWorkDirectory).exists()) {
            new File(currentWorkDirectory).mkdir();
        }

        List<String> commandArguments = new LinkedList<>();
        commandArguments.add(ffmpegExecutable);
        commandArguments.add("-y");
        commandArguments.add("-i");
        commandArguments.add(url);
        commandArguments.add("-c");
        commandArguments.add("copy");
        commandArguments.add("-f");
        commandArguments.add("segment");
        commandArguments.add("-segment_list");
        commandArguments.add("out.ffcat");
        commandArguments.add("seg_%03d.ts");

        try {
            runCMD(commandArguments.toArray(new String[0]), currentWorkDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void concat(String segmentList, String sourceDirectory, String output) {
        List<String> commandArguments = new LinkedList<>();
        commandArguments.add(ffmpegExecutable);
        commandArguments.add("-f");
        commandArguments.add("concat");
        commandArguments.add("-i");
        commandArguments.add(segmentList);
        commandArguments.add("-c");
        commandArguments.add("copy");
        commandArguments.add(output);

        try {
            runCMD(commandArguments.toArray(new String[0]), currentWorkDirectory);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        if (process == null || !process.isAlive()) {
            return;
        }
        process.destroy();
        if (thread != null) {
            thread.stop();
        }
    }


    private void runCMD(String[] commandArgs, String workDirectory) throws IOException, InterruptedException {
        System.out.println("Standard output: " + String.join(" ", commandArgs));
        ProcessBuilder processBuilder = new ProcessBuilder(commandArgs);
        processBuilder.directory(new File(workDirectory));
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


        System.out.println("Read command ");
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
                    System.out.println("=====================================================================================" + line + newLineCharacter);
                }
                isErrorReady = stdError.ready();
                //System.out.println("ERROR READY " + isErrorReady);
                if (isErrorReady) {
                    line = stdError.readLine();
                    isErrorError = false;
                    System.out.println("ERROR::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::" + line + newLineCharacter);

                }
                isProcessAlive = process.isAlive();
                //System.out.println("Process Alive " + isProcessAlive);
                if (!isProcessAlive) {
                    System.out.println(":::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::: Process DIE " + line + newLineCharacter);
                    line = null;
                    isErrorError = false;
                    process.waitFor(1000, TimeUnit.MILLISECONDS);
                }

            } while (line != null);

            //Nothing else to read, lets pause for a bit before trying again
            System.out.println("PROCESS WAIT FOR");
            process.waitFor(100, TimeUnit.MILLISECONDS);
        }
        System.out.println("Command finished");
    }
}
