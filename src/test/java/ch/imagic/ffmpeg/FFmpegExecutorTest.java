package ch.imagic.ffmpeg;

import static ch.imagic.ffmpeg.FFmpeg.FPS_30;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.job.FFmpegJob;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.progress.Progress;
import ch.imagic.ffmpeg.progress.RecordingProgressListener;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Tests actually shelling out ffmpeg and ffprobe. Could be flakey if ffmpeg or ffprobe change. */
public class FFmpegExecutorTest {

    static final Logger LOG = LoggerFactory.getLogger(FFmpegExecutorTest.class);

    @Rule
    public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

    final FFmpeg ffmpeg = new FFmpeg();
    final FFprobe ffprobe = new FFprobe();
    final FFmpegExecutor ffExecutor = new FFmpegExecutor(ffmpeg, ffprobe);
    final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FFmpegExecutorTest() throws IOException {}

    // Webserver which can be used for fetching files over HTTP
    static HttpServer server;

    @BeforeClass
    public static void startWebserver() throws IOException {
        Path sample = Path.of(Samples.big_buck_bunny_720p_1mb);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/" + Samples.base_big_buck_bunny_720p_1mb, exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "video/mp4");
            exchange.sendResponseHeaders(200, Files.size(sample));
            try (OutputStream response = exchange.getResponseBody()) {
                Files.copy(sample, response);
            }
        });
        server.start();

        LOG.info("Started server at {}", getWebserverRoot());
    }

    @AfterClass
    public static void stopWebserver() {
        server.stop(0);
    }

    public static String getWebserverRoot() {
        InetSocketAddress address = server.getAddress();
        String host = address.getHostString();
        if (host.indexOf(':') >= 0 && !(host.startsWith("[") && host.endsWith("]"))) {
            host = "[" + host + "]";
        }
        return "http://" + host + ":" + address.getPort() + "/";
    }

    @Test
    public void testNormal() throws InterruptedException, ExecutionException, IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.DEBUG)
                .setUserAgent(
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/52.0.2743.82 Safari/537.36")
                .setInput(getWebserverRoot() + Samples.base_big_buck_bunny_720p_1mb)
                .addExtraArgs("-probesize", "1000000")
                // .setStartOffset(1500, TimeUnit.MILLISECONDS)
                .overrideOutputFiles(true)
                .addOutput(Samples.output_mp4)
                .setFrames(100)
                .setFormat("mp4")
                .setStartOffset(500, TimeUnit.MILLISECONDS)
                .setAudioCodec("aac")
                .setAudioChannels(1)
                .setAudioSampleRate(48000)
                .setAudioBitStreamFilter("chomp")
                .setAudioFilter("aecho=0.8:0.88:6:0.4")
                .setAudioQuality(1)
                .setVideoCodec("libx264")
                .setVideoFrameRate(FPS_30)
                .setVideoResolution(320, 240)
                // .setVideoFilter("scale=320:trunc(ow/a/2)*2")
                // .setVideoPixelFormat("yuv420p")
                // .setVideoBitStreamFilter("noise")
                .setVideoQuality(2)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegJob job = ffExecutor.createJob(builder);
        runAndWait(job);

        assertEquals(FFmpegJob.State.FINISHED, job.getState());
    }

    @Test
    public void testTwoPass() throws InterruptedException, ExecutionException, IOException {
        FFmpegProbeResult in = ffprobe.probe(Samples.big_buck_bunny_720p_1mb);
        assertFalse(in.hasError());

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(in)
                .overrideOutputFiles(true)
                .addOutput(Samples.output_mp4)
                .setFormat("mp4")
                .disableAudio()
                .setVideoCodec("mpeg4")
                .setVideoFrameRate(FFmpeg.FPS_30)
                .setVideoResolution(320, 240)
                .setTargetSize(1024 * 1024)
                .done();

        FFmpegJob job = ffExecutor.createTwoPassJob(builder);
        runAndWait(job);

        assertEquals(FFmpegJob.State.FINISHED, job.getState());
    }

    @Test
    public void testFilter() throws InterruptedException, ExecutionException, IOException {

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(Samples.big_buck_bunny_720p_1mb)
                .overrideOutputFiles(true)
                .addOutput(Samples.output_mp4)
                .setFormat("mp4")
                .disableAudio()
                .setVideoCodec("mpeg4")
                .setVideoFilter("scale=320:trunc(ow/a/2)*2")
                .done();

        FFmpegJob job = ffExecutor.createJob(builder);
        runAndWait(job);

        assertEquals(FFmpegJob.State.FINISHED, job.getState());
    }

    @Test
    public void testMetaTags() throws InterruptedException, ExecutionException, IOException {

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(Samples.big_buck_bunny_720p_1mb)
                .overrideOutputFiles(true)
                .addOutput(Samples.output_mp4)
                .setFormat("mp4")
                .disableAudio()
                .setVideoCodec("mpeg4")
                .addMetaTag("comment", "This=Nice!")
                .addMetaTag("title", "Big Buck Bunny")
                .done();

        FFmpegJob job = ffExecutor.createJob(builder);
        runAndWait(job);

        assertEquals(FFmpegJob.State.FINISHED, job.getState());
    }

    /**
     * Test if addStdoutOutput() actually works, and the output can be correctly captured.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     */
    @Test
    public void testStdout() throws InterruptedException, ExecutionException, IOException {

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(Samples.big_buck_bunny_720p_1mb)
                .addStdoutOutput()
                .setFormat("s8")
                .setAudioChannels(1)
                .done();

        List<String> newArgs = new ArrayList<>();
        newArgs.add(ffmpeg.getPath().getAbsolutePath());
        newArgs.addAll(builder.build());

        // TODO Add support to the FFmpegJob to export the stream
        Process p = new ProcessBuilder(newArgs).start();

        long byteCount = p.getInputStream().transferTo(OutputStream.nullOutputStream());

        assertEquals(0, p.waitFor());

        // This is perhaps fragile, but one byte per audio sample
        assertEquals(254976, byteCount);
    }

    @Test
    public void testProgress() throws InterruptedException, ExecutionException, IOException {
        FFmpegProbeResult in = ffprobe.probe(Samples.big_buck_bunny_720p_1mb);

        assertFalse(in.hasError());

        FFmpegBuilder builder = new FFmpegBuilder()
                .readAtNativeFrameRate() // Slows the test down
                .setInput(in)
                .overrideOutputFiles(true)
                .addOutput(Samples.output_mp4)
                .done();

        RecordingProgressListener listener = new RecordingProgressListener();

        FFmpegJob job = ffExecutor.createJob(builder, listener);
        runAndWait(job);

        assertEquals(FFmpegJob.State.FINISHED, job.getState());

        List<Progress> progesses = listener.progesses;

        // Since the results of ffmpeg are not predictable, test for the bare minimum.
        assertTrue(progesses.size() >= 2);
        assertEquals(Progress.Status.CONTINUE, progesses.get(0).status);
        assertEquals(Progress.Status.END, progesses.get(progesses.size() - 1).status);
    }

    @Test
    public void testIssue112() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(Samples.testscreen_jpg)
                .addInput(Samples.test_mp3)
                .addExtraArgs("-loop", "1")
                .overrideOutputFiles(true)
                .addOutput(Samples.output_mp4)
                .setFormat("mp4")
                // .setDuration(30, TimeUnit.SECONDS)
                .addExtraArgs("-shortest")
                .setAudioCodec("aac")
                .setAudioSampleRate(48_000)
                .setAudioBitRate(32768)
                .setVideoCodec("libx264")
                .setVideoFrameRate(24, 1)
                .setVideoResolution(640, 480)
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL) // Allow FFmpeg to use experimental specs
                .done();

        // Run a one-pass encode
        ffExecutor.createJob(builder).run();
    }

    protected void runAndWait(FFmpegJob job) throws ExecutionException, InterruptedException {
        executor.submit(job).get();
    }
}
