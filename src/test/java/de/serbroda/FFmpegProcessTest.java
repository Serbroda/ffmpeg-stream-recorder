package de.serbroda;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class FFmpegProcessTest {

    private static final String FFMPEG_EXECUTABLE = "C:\\Users\\danny\\Downloads\\ffmpeg-20200522-38490cb-win64-static\\bin\\ffmpeg.exe";
    private static final String TEST_URL = "https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8";

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRecord() throws IOException, ExecutionException, InterruptedException {
        List<String> commandArguments = new LinkedList<>();
        commandArguments.add(FFMPEG_EXECUTABLE);
        commandArguments.add("-y");
        commandArguments.add("-i");
        commandArguments.add(TEST_URL);
        commandArguments.add("-c:v");
        commandArguments.add("copy");
        commandArguments.add("-c:a");
        commandArguments.add("copy");
        commandArguments.add("-f");
        commandArguments.add("segment");
        commandArguments.add("-segment_list");
        commandArguments.add("out.ffcat");
        commandArguments.add("seg_%03d.ts");

        FFmpegProcessOptions options = new FFmpegProcessOptions();
        options.setWorkDirectory(createTestFolderAndGetAbsolutePath("test"));
        FFmpegProcess process = new FFmpegProcess(FFMPEG_EXECUTABLE);
        Future<Integer> exitCode = process.startAsync(commandArguments, options);

        int counter = 0;
        while(!exitCode.isDone()) {
            counter++;
            waitSeconds(1);
            if(counter > 10) {
                process.stop();
            }
        }

        System.out.println("Finished with exit code " + exitCode.get());
    }

    @Test
    public void testRecorder() throws IOException, InterruptedException {
        FFmpegRecorder recorder = new FFmpegRecorder();
        recorder.startRecord(TEST_URL, createTestFolderAndGetAbsolutePath("test"), "output.mp4");
        waitSeconds(30);
        recorder.stopAndFinish();

        System.out.println("Finished");
    }

    private String createTestFolderAndGetAbsolutePath(String folder) throws IOException {
        return tempFolder.newFolder(folder).toPath().toFile().toString();
    }

    private void waitSeconds(int seconds) {
        try {
            for (int i = 1; i <= seconds; i++) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
