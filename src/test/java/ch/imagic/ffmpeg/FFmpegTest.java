package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.fixtures.Codecs;
import ch.imagic.ffmpeg.fixtures.Formats;
import ch.imagic.ffmpeg.fixtures.PixelFormats;
import ch.imagic.ffmpeg.lang.MockProcess;
import ch.imagic.ffmpeg.lang.NewProcessAnswer;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class FFmpegTest {

    @Mock
    FFMpegProcessFactory runFunc;

    FFmpeg ffmpeg;

    @BeforeEach
    public void before() throws IOException {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version")))
                .thenAnswer(new NewProcessAnswer("ffmpeg-version"));
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-formats")))
                .thenAnswer(new NewProcessAnswer("ffmpeg-formats"));
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-codecs")))
                .thenAnswer(new NewProcessAnswer("ffmpeg-codecs"));
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-pix_fmts")))
                .thenAnswer(new NewProcessAnswer("ffmpeg-pix_fmts"));

        ffmpeg = new FFmpeg(runFunc);
    }

    public static <T> List<T> argThatHasItem(T s) {
        return argThat(items -> items != null && items.contains(s));
    }

    @Test
    public void testVersion() throws Exception {
        assertEquals("ffmpeg version 0.10.9-7:0.10.9-1~raring1", ffmpeg.version());
        assertEquals("ffmpeg version 0.10.9-7:0.10.9-1~raring1", ffmpeg.version());

        verify(runFunc, times(1)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version"));
    }

    @Test
    public void testCodecs() throws IOException {
        // Run twice, the second should be cached
        assertEquals(Codecs.CODECS, ffmpeg.codecs());
        assertEquals(Codecs.CODECS, ffmpeg.codecs());

        verify(runFunc, times(1)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-codecs"));
    }

    @Test
    public void testFormats() throws IOException {
        // Run twice, the second should be cached
        assertEquals(Formats.FORMATS, ffmpeg.formats());
        assertEquals(Formats.FORMATS, ffmpeg.formats());

        verify(runFunc, times(1)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-formats"));
    }

    @Test
    public void testPixelFormat() throws IOException {
        // Run twice, the second should be cached
        assertEquals(PixelFormats.PIXEL_FORMATS, ffmpeg.pixelFormats());
        assertEquals(PixelFormats.PIXEL_FORMATS, ffmpeg.pixelFormats());

        verify(runFunc, times(1)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-pix_fmts"));
    }

    @Test
    public void versionRetriesAfterProcessFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version")))
                .thenAnswer(invocation -> {
                    boolean first = attempts.getAndIncrement() == 0;
                    return process(first ? "failed version\n" : "successful version\n", "", first ? 1 : 0);
                });

        IOException failure = assertThrows(IOException.class, ffmpeg::version);
        assertTrue(failure.getMessage().contains("exited with code 1"));
        assertEquals("successful version", ffmpeg.version());
        verify(runFunc, times(2)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version"));
    }

    @Test
    public void codecsRetriesAfterProcessFailure() throws Exception {
        assertQueryRetries(
                "-codecs",
                " DEV.L. partial Partial codec\n",
                " DEV.L. first First codec\n DEV.L. second Second codec\n",
                ffmpeg::codecs);
    }

    @Test
    public void formatsRetriesAfterProcessFailure() throws Exception {
        assertQueryRetries(
                "-formats",
                " DE partial Partial format\n",
                " DE first First format\n DE second Second format\n",
                ffmpeg::formats);
    }

    @Test
    public void pixelFormatsRetriesAfterProcessFailure() throws Exception {
        assertQueryRetries(
                "-pix_fmts", "IO... partial 3 24\n", "IO... first 3 24\nIO... second 4 32\n", ffmpeg::pixelFormats);
    }

    @Test
    public void runPipesStdinStdoutAndStderrAndBuildsCommand() throws Exception {
        ByteArrayOutputStream processStdin = new ByteArrayOutputStream();
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new MockProcess(processStdin, bytes("process stdout"), bytes("process stderr"), true, 0));
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        ffmpeg.run(
                        builder(),
                        FFMpegStreamConsumer.toOutputStream(stdout),
                        FFMpegStreamConsumer.toOutputStream(stderr),
                        (a, b) -> null,
                        bytes("process stdin"))
                .get();

        assertEquals("process stdin", processStdin.toString(StandardCharsets.UTF_8));
        assertEquals("process stdout", stdout.toString(StandardCharsets.UTF_8));
        assertEquals("process stderr", stderr.toString(StandardCharsets.UTF_8));
        verify(runFunc)
                .createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.eq(prependBinary(builder().build())));
    }

    @Test
    public void runReportsNonZeroProcessExit() throws Exception {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new MockProcess(OutputStream.nullOutputStream(), bytes(""), bytes("diagnostic"), true, 23));
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();

        IOException failure = assertThrows(
                IOException.class,
                () -> ffmpeg.run(
                                builder(),
                                FFMpegStreamConsumer.toOutputStream(OutputStream.nullOutputStream()),
                                FFMpegStreamConsumer.toOutputStream(stderr),
                                (a, b) -> null,
                                InputStream.nullInputStream())
                        .get());

        assertTrue(failure.getMessage().contains("exited with code 23"));
        assertEquals("diagnostic", stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void runPropagatesOutputPipeFailure() throws Exception {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new MockProcess(bytes("output")));
        OutputStream failingOutput = new OutputStream() {
            @Override
            public void write(int value) throws IOException {
                throw new IOException("cannot write output");
            }
        };

        IOException failure = assertThrows(
                IOException.class,
                () -> ffmpeg.run(builder(), FFMpegStreamConsumer.toOutputStream(failingOutput))
                        .get());

        assertEquals("cannot write output", failure.getMessage());
    }

    @Test
    public void runReturnsStdoutConsumerResult() throws Exception {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(new MockProcess(bytes("process output")));

        String result = ffmpeg.run(builder(), input -> new String(input.readAllBytes(), StandardCharsets.UTF_8))
                .get();

        assertEquals("process output", result);
    }

    @Test
    public void runClosesProcessWhenExecutorRejectsJob() throws Exception {
        AtomicBoolean processClosed = new AtomicBoolean();
        var process = new MockProcess(InputStream.nullInputStream()) {
            @Override
            public void close() {
                processClosed.set(true);
            }
        };
        FFMpegProcessFactory processFactory = mock(FFMpegProcessFactory.class);
        when(processFactory.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(process);
        FFmpeg rejectingFfmpeg = new FFmpeg(
                command -> {
                    throw new RejectedExecutionException("rejected");
                },
                FFMpegLogger.noop(),
                ffmpeg.getPath(),
                processFactory);

        assertThrows(RejectedExecutionException.class, () -> rejectingFfmpeg.run(builder()));

        assertTrue(processClosed.get());
    }

    private void assertQueryRetries(String option, String failedOutput, String successfulOutput, IoQuery query)
            throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem(option)))
                .thenAnswer(invocation -> {
                    boolean first = attempts.getAndIncrement() == 0;
                    return process(first ? failedOutput : successfulOutput, "", first ? 1 : 0);
                });

        assertThrows(IOException.class, query::get);
        assertEquals(2, query.get().size());
        verify(runFunc, times(2)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem(option));
    }

    private List<String> prependBinary(List<String> arguments) {
        java.util.ArrayList<String> command = new java.util.ArrayList<>();
        command.add(ffmpeg.getPath().getAbsolutePath());
        command.addAll(arguments);
        return command;
    }

    private static FFmpegBuilder builder() {
        return new FFmpegBuilder().setInput("input.mp4").addOutput("output.mp4").done();
    }

    private static MockProcess process(String stdout, String stderr, int exitCode) {
        return new MockProcess(OutputStream.nullOutputStream(), bytes(stdout), bytes(stderr), true, exitCode);
    }

    private static ByteArrayInputStream bytes(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @FunctionalInterface
    private interface IoQuery {
        List<?> get() throws IOException;
    }
}
