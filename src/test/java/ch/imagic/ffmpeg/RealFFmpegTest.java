package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.probe.FFmpegCodecType;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.Fraction;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class RealFFmpegTest {

    final FFmpeg ffmpeg = new FFmpeg();
    final FFprobe ffprobe = new FFprobe();

    @TempDir
    Path temporaryDirectory;

    public RealFFmpegTest() throws IOException {}

    @Test
    public void testRawVideoRoundTripThroughFfv1() throws IOException {
        Path originalRaw = temporaryDirectory.resolve("original.raw");
        Path losslessVideo = temporaryDirectory.resolve("lossless.mkv");
        Path roundTrippedRaw = temporaryDirectory.resolve("round-tripped.raw");

        FFmpegBuilder generateRaw = new FFmpegBuilder()
                .setFormat("lavfi")
                .setInput("testsrc=size=640x360")
                .addOutput(originalRaw.toString())
                .setFormat("rawvideo")
                .setVideoCodec("rawvideo")
                .setVideoPixelFormat("bgr24")
                .setFrames(30)
                .done();
        try (FFMpegJob<Void> job = ffmpeg.run(generateRaw)) {
            job.get();
        }

        FFmpegBuilder encodeLosslessly = new FFmpegBuilder()
                .setFormat("rawvideo")
                .setInputFrameRate(Fraction.getFraction(1, 1))
                .setInputPixelFormat("bgr24")
                .setVideoSize(640, 360)
                .setInput(originalRaw.toString())
                .addOutput(losslessVideo.toString())
                .setVideoCodec("ffv1")
                .setVideoFrameRate(1, 1)
                .done();
        try (FFMpegJob<Void> job = ffmpeg.run(encodeLosslessly)) {
            job.get();
        }

        FFmpegBuilder decodeRaw = new FFmpegBuilder()
                .setInput(losslessVideo.toString())
                .addOutput(roundTrippedRaw.toString())
                .setFormat("rawvideo")
                .setVideoCodec("rawvideo")
                .setVideoPixelFormat("bgr24")
                .done();
        try (FFMpegJob<Void> job = ffmpeg.run(decodeRaw)) {
            job.get();
        }

        Assertions.assertEquals(-1, Files.mismatch(originalRaw, roundTrippedRaw));
    }

    @Test
    public void testMovBeforeMdatCleanWay() throws IOException, InterruptedException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.TRACE)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .addStdoutOutput()
                .setDuration(0, TimeUnit.MILLISECONDS)
                .setFormat("null")
                .done();

        try (FFMpegJob<Boolean> job = ffmpeg.runCaptureStderr(builder, input -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("type:'moov'")) {
                    return true;
                } else if (line.contains("type:'mdat'")) {
                    return false;
                }
            }

            throw new IOException("Failed to parse");
        })) {

            Assertions.assertTrue(job.get());
            Assertions.assertEquals(0, job.getExitCode().orElse(-1));
        }
    }

    @Test
    public void testMovBeforeMdatNoOutputWay() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.TRACE)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .noOutput();

        try (FFMpegJob<Boolean> job = ffmpeg.runCaptureStderr(builder, input -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("type:'moov'")) {
                    return true;
                } else if (line.contains("type:'mdat'")) {
                    return false;
                }
            }

            throw new IOException("Failed to parse");
        })) {
            Assertions.assertTrue(job.get());
            Assertions.assertNotEquals(0, job.getExitCode().orElse(-1));
        }
    }

    @Test
    public void testExceptionOccursDuringParsing() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.TRACE)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .noOutput();

        try (FFMpegJob<Boolean> job = ffmpeg.runCaptureStderr(builder, input -> {
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
            for (int i = 0; i < 10; i++) {
                if (reader.readLine() == null) {
                    break;
                }
            }

            throw new IOException("BEEP");
        })) {

            try {
                job.get();
                Assertions.fail("exception expected");
            } catch (IOException ioe) {
                Assertions.assertEquals("BEEP", ioe.getMessage());
            }
        }
    }

    @Test
    public void testTranscodeFromInputStreamToOutputStream() throws IOException {
        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput("-")
                .addStdoutOutput()
                .setFormat("avi")
                .done();
        ByteArrayOutputStream aviOutput = new ByteArrayOutputStream();

        try (var mp4Input = Files.newInputStream(Path.of(Samples.BIG_BUCK_BUNNY_720P_1MB));
                FFMpegJob<Void> job = ffmpeg.run(
                        builder,
                        FFMpegStreamConsumer.toOutputStream(aviOutput),
                        FFMpegStreamConsumer.noop(),
                        (stdout, stderr) -> null,
                        mp4Input)) {
            job.get();
        }

        Path aviFile = temporaryDirectory.resolve("transcoded.avi");
        Files.write(aviFile, aviOutput.toByteArray());
        FFmpegProbeResult probeResult = ffprobe.probe(aviFile.toFile()).get();

        Assertions.assertTrue(probeResult.getFormat().getFormatName().contains("avi"));
    }

    @Test
    public void testTranscodeIgnoresLoggerExceptions() throws IOException {
        AtomicInteger commandLines = new AtomicInteger();
        AtomicInteger stdoutWrites = new AtomicInteger();
        AtomicInteger stderrWrites = new AtomicInteger();
        AtomicInteger processDeaths = new AtomicInteger();
        FFMpegLogger badLogger = new FFMpegLogger() {
            @Override
            public boolean wantsRawStdout() {
                return true;
            }

            @Override
            public boolean wantsRawStderr() {
                return true;
            }

            @Override
            public boolean wantsCommandLine() {
                return true;
            }

            @Override
            public boolean wantsProcessDeath() {
                return true;
            }

            @Override
            public void onCommandLine(long pid, List<String> commandLine) {
                commandLines.incrementAndGet();
                throw new RuntimeException("logger failed");
            }

            @Override
            public void onRawStdout(long pid, byte[] rawData, int off, int len) {
                stdoutWrites.incrementAndGet();
                throw new RuntimeException("logger failed");
            }

            @Override
            public void onRawStderr(long pid, byte[] rawData, int off, int len) {
                stderrWrites.incrementAndGet();
                throw new RuntimeException("logger failed");
            }

            @Override
            public void onProcessDeath(long pid, int exitCode) {
                processDeaths.incrementAndGet();
                throw new RuntimeException("logger failed");
            }
        };
        FFmpeg loggedFfmpeg = new FFmpeg(
                FFcommon.getDefaultExecutor(),
                badLogger,
                ffmpeg.getPath(),
                ch.imagic.ffmpeg.process.FFMpegProcessFactory.defaultFactory());
        FFmpegBuilder builder = new FFmpegBuilder()
                .setVerbosity(FFmpegBuilder.Verbosity.INFO)
                .setInput(Samples.BIG_BUCK_BUNNY_720P_1MB)
                .addStdoutOutput()
                .setFormat("avi")
                .done();
        ByteArrayOutputStream aviOutput = new ByteArrayOutputStream();

        try (FFMpegJob<Void> job = loggedFfmpeg.run(builder, FFMpegStreamConsumer.toOutputStream(aviOutput))) {
            job.get();
        }

        Path aviFile = temporaryDirectory.resolve("transcoded-with-bad-logger.avi");
        Files.write(aviFile, aviOutput.toByteArray());
        FFmpegProbeResult probeResult = ffprobe.probe(aviFile.toFile()).get();

        Assertions.assertTrue(probeResult.getFormat().getFormatName().contains("avi"));
        Assertions.assertEquals(1, commandLines.get());
        Assertions.assertTrue(stdoutWrites.get() > 0);
        Assertions.assertTrue(stderrWrites.get() > 0);
        Assertions.assertEquals(1, processDeaths.get());
    }

    @Test
    public void testProbeFromInputStream() throws IOException {
        FFmpegProbeResult probeResult;
        try (var imageInput = Files.newInputStream(Path.of(Samples.TESTSCREEN_JPG));
                FFMpegJob<FFmpegProbeResult> job = ffprobe.probe(
                        imageInput, FFMpegStreamConsumer.noop(), FFmpegProbeResult.class, "-select_streams", "v:0")) {
            probeResult = job.get();
        }

        Assertions.assertFalse(probeResult.hasError());
        Assertions.assertEquals(1, probeResult.getStreams().size());
        Assertions.assertEquals(
                FFmpegCodecType.VIDEO, probeResult.getStreams().get(0).getCodecType());
    }

    @Test
    public void testProbeFromTcpStreamWithoutReadTimeout() throws Exception {
        ExecutorService serverExecutor = Executors.newSingleThreadExecutor();
        CountDownLatch releaseServer = new CountDownLatch(1);
        try (ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            Future<?> server = serverExecutor.submit(() -> {
                try (Socket connection = serverSocket.accept();
                        var imageInput = Files.newInputStream(Path.of(Samples.BIG_BUCK_BUNNY_720P_1MB))) {
                    imageInput.transferTo(connection.getOutputStream());
                    connection.getOutputStream().flush();
                    releaseServer.await();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            try (Socket client = new Socket(InetAddress.getLoopbackAddress(), serverSocket.getLocalPort());
                    var tcpInput = client.getInputStream();
                    FFMpegJob<FFmpegProbeResult> job =
                            ffprobe.probe(tcpInput, FFMpegStreamConsumer.noop(), FFmpegProbeResult.class)) {
                Assertions.assertEquals(0, client.getSoTimeout());
                Assertions.assertTrue(
                        job.await(30, TimeUnit.SECONDS), "FFprobe did not finish while the TCP stream remained open");

                FFmpegProbeResult probeResult = job.get();
                Assertions.assertFalse(probeResult.hasError());
                Assertions.assertEquals(2, probeResult.getStreams().size());
                Assertions.assertEquals(
                        FFmpegCodecType.VIDEO, probeResult.getStreams().get(0).getCodecType());
            } finally {
                releaseServer.countDown();
            }

            awaitServer(server);
        } finally {
            releaseServer.countDown();
            serverExecutor.shutdownNow();
        }
    }

    private static void awaitServer(Future<?> server) throws Exception {
        try {
            server.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    @Test
    public void killingProbeClosesBlockingInputAndStopsExecutorThreads() throws Exception {
        TrackingExecutor executor = new TrackingExecutor();
        FFprobe trackedProbe = new FFprobe(executor, ffprobe.getPath());
        byte[] image = Files.readAllBytes(Path.of(Samples.TESTSCREEN_JPG));
        BlockingInputStream input = new BlockingInputStream(image, image.length / 2);
        FFMpegJob<FFmpegProbeResult> job =
                trackedProbe.probe(input, FFMpegStreamConsumer.noop(), FFmpegProbeResult.class);

        try {
            Assertions.assertTrue(input.awaitBlocked(5, TimeUnit.SECONDS), "FFprobe did not consume half the input");
            Assertions.assertFalse(job.await(5, TimeUnit.SECONDS), "FFprobe unexpectedly finished without input EOF");

            job.kill();

            Assertions.assertTrue(executor.awaitTermination(), "FFprobe executor threads remained alive after kill");
        } finally {
            job.kill();
            input.close();
            executor.awaitTermination();
        }
    }

    private static final class TrackingExecutor implements Executor {
        private final ConcurrentLinkedQueue<Thread> threads = new ConcurrentLinkedQueue<>();
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public void execute(Runnable command) {
            Thread thread = new Thread(command, "ffprobe-test-" + counter.getAndIncrement());
            threads.add(thread);
            thread.start();
        }

        private boolean awaitTermination() throws InterruptedException {
            int counter = this.counter.get();
            while (true) {
                if (this.counter.get() != counter) {
                    // While this is a implementation detail, currently this should always be the case
                    // In the future this may become allowed then we just have to change this test a bit.
                    System.out.println("Threads were started after shutdown!");
                    return false;
                }
                Thread t = threads.poll();
                if (t == null) {
                    return true;
                }
                t.join(5000);
                if (t.isAlive()) {
                    return false;
                }
            }
        }
    }

    private static final class BlockingInputStream extends InputStream {
        private final byte[] data;
        private final int limit;
        private final CountDownLatch blocked = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);
        private int position;

        private BlockingInputStream(byte[] data, int limit) {
            this.data = data;
            this.limit = limit;
        }

        @Override
        public int read() throws IOException {
            byte[] oneByte = new byte[1];
            return read(oneByte, 0, 1) < 0 ? -1 : Byte.toUnsignedInt(oneByte[0]);
        }

        @Override
        public int read(byte[] target, int offset, int length) throws IOException {
            if (closed.getCount() == 0) {
                throw new IOException("Stream closed");
            }
            if (position < limit) {
                int count = Math.min(length, limit - position);
                System.arraycopy(data, position, target, offset, count);
                position += count;
                return count;
            }

            blocked.countDown();
            try {
                closed.await();
                throw new IOException("Stream closed");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
        }

        @Override
        public void close() {
            closed.countDown();
        }

        private boolean awaitBlocked(long timeout, TimeUnit unit) throws InterruptedException {
            return blocked.await(timeout, unit);
        }
    }

    @Test
    public void testProbeVideoFromInputStream() throws IOException {
        FFmpegProbeResult probeResult;
        try (var videoInput = Files.newInputStream(Path.of(Samples.BIG_BUCK_BUNNY_720P_1MB));
                FFMpegJob<FFmpegProbeResult> job =
                        ffprobe.probe(videoInput, FFMpegStreamConsumer.noop(), FFmpegProbeResult.class)) {
            probeResult = job.get();
        }

        Assertions.assertFalse(probeResult.hasError());
        Assertions.assertEquals(2, probeResult.getStreams().size());
        Assertions.assertEquals(
                FFmpegCodecType.VIDEO, probeResult.getStreams().get(0).getCodecType());
        Assertions.assertEquals(
                FFmpegCodecType.AUDIO, probeResult.getStreams().get(1).getCodecType());
    }
}
