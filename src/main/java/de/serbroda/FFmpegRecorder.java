package de.serbroda;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;

public class FFmpegRecorder {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    private FFmpegProcess process;
    private String currentWorkDirectory;
    private Future<Integer> exitCodeFuture;
    private String outFile;

    public FFmpegProcess getProcess() {
        return process;
    }

    public String getCurrentWorkDirectory() {
        return currentWorkDirectory;
    }

    public void startRecord(String url, String workingDirectory, String outfile) {
        if(process != null && process.isRunning()) {
            return;
        }

        currentWorkDirectory = Paths.get(workingDirectory, DATE_FORMAT.format(new Date())).toAbsolutePath().toString();
        if(!new File(currentWorkDirectory).exists()) {
            new File(currentWorkDirectory).mkdir();
        }
        outFile = outfile;

        List<String> commandArguments = new LinkedList<>();
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

        FFmpegProcessOptions options = new FFmpegProcessOptions();
        options.setWorkDirectory(currentWorkDirectory);

        process = new FFmpegProcess("C:\\Users\\danny\\Downloads\\ffmpeg-20200522-38490cb-win64-static\\bin\\ffmpeg.exe");
        exitCodeFuture = process.startAsync(commandArguments, options);

    }

    public void stop() throws InterruptedException {
        process.stop();

        while(!exitCodeFuture.isDone()) {
            Thread.sleep(300);
        }
    }

    public void stopAndFinish() throws InterruptedException, IOException {
        stop();

        final File workDir = new File(currentWorkDirectory);
        final File[] files = workDir.listFiles((dir, name) -> name.matches("seg_\\d.*\\.ts"));
        if(files.length > 1) {
            concat();
            clean();
        } else if (files.length == 1) {
            convert(files[0].getAbsolutePath());
            clean();
        } else {
            throw new IllegalStateException("No segment files found in directory " + workDir.toString());
        }
    }

    public void concat() throws IOException, InterruptedException {
        List<String> commandArguments = new LinkedList<>();
        commandArguments.add("-f");
        commandArguments.add("concat");
        commandArguments.add("-i");
        commandArguments.add("out.ffcat");
        commandArguments.add("-c");
        commandArguments.add("copy");
        commandArguments.add(Paths.get(currentWorkDirectory, outFile).toAbsolutePath().toString());

        FFmpegProcessOptions options = new FFmpegProcessOptions();
        options.setPrintMessages(true);
        options.setWorkDirectory(currentWorkDirectory);
        process = new FFmpegProcess("C:\\Users\\danny\\Downloads\\ffmpeg-20200522-38490cb-win64-static\\bin\\ffmpeg.exe");
        process.start(commandArguments, options);
    }

    public void convert(String input) throws IOException, InterruptedException {
        List<String> commandArguments = new LinkedList<>();
        commandArguments.add("-i");
        commandArguments.add(input);
        commandArguments.add("-map");
        commandArguments.add("0");
        commandArguments.add("-c");
        commandArguments.add("copy");
        commandArguments.add(Paths.get(currentWorkDirectory, outFile).toAbsolutePath().toString());

        FFmpegProcessOptions options = new FFmpegProcessOptions();
        options.setPrintMessages(true);
        options.setWorkDirectory(currentWorkDirectory);
        process = new FFmpegProcess("C:\\Users\\danny\\Downloads\\ffmpeg-20200522-38490cb-win64-static\\bin\\ffmpeg.exe");
        process.start(commandArguments, options);
    }

    public void clean() {
        clean(currentWorkDirectory, "seg_\\d.*\\.ts");
        clean(currentWorkDirectory, "out\\.ffcat");
    }

    public void clean(String directory, String pattern) {
        final File workDir = new File(currentWorkDirectory);
        final File[] files = workDir.listFiles((dir, name) -> name.matches("seg_\\d.*\\.ts"));
        for(File file : files) {
            file.delete();
        }
    }
}
