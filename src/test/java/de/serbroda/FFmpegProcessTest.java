package de.serbroda;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;

public class FFmpegProcessTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testRecord() throws IOException, InterruptedException {
        FFmpegProcess process = new FFmpegProcess("C:\\Users\\danny\\Downloads\\ffmpeg-20200522-38490cb-win64-static\\bin\\ffmpeg.exe");
        process.startRecordAsync("https://bitdash-a.akamaihd.net/content/sintel/hls/playlist.m3u8",
                tempFolder.newFolder("test").toPath().toFile().toString());

        for (int i = 1; i <= 10; i++) {
            Thread.sleep(1000);
        }

        process.stopRecord();
    }
}
