package ch.imagic.ffmpeg;

import static ch.imagic.ffmpeg.FFmpegTest.argThatHasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import ch.imagic.ffmpeg.builder.Strict;
import ch.imagic.ffmpeg.fixtures.Samples;
import ch.imagic.ffmpeg.lang.MockProcess;
import ch.imagic.ffmpeg.lang.NewProcessAnswer;
import ch.imagic.ffmpeg.probe.FFmpegChapter;
import ch.imagic.ffmpeg.probe.FFmpegCodecType;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.Fraction;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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
public class FFprobeTest {

    @Mock
    FFMpegProcessFactory runFunc;

    FFprobe ffprobe;

    static final Gson gson = FFmpegUtils.getGson();

    @BeforeEach
    public void before() throws IOException {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version")))
                .thenAnswer(new NewProcessAnswer("ffprobe-version"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.BIG_BUCK_BUNNY_720P_1MB).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-big_buck_bunny_720p_1mb.mp4"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.ALWAYS_ON_MY_MIND).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-always_on_my_mind.mp4"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.START_PTS_TEST).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-start_pts_test"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.DIVIDE_BY_ZERO).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-divide-by-zero"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.BOOK_WITH_CHAPTERS).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("book_with_chapters.m4b"));

        when(runFunc.createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        argThatHasItem(new File(Samples.SIDE_DATA_LIST).getAbsolutePath())))
                .thenAnswer(new NewProcessAnswer("ffprobe-side_data_list"));

        ffprobe = new FFprobe(runFunc);
    }

    @Test
    public void testVersion() throws Exception {
        assertEquals("ffprobe version 3.0.2 Copyright (c) 2007-2016 the FFmpeg developers", ffprobe.version());
        assertEquals("ffprobe version 3.0.2 Copyright (c) 2007-2016 the FFmpeg developers", ffprobe.version());

        verify(runFunc, times(1)).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-version"));
    }

    @Test
    public void testProbeVideo() throws IOException {
        FFmpegProbeResult info =
                ffprobe.probe(new File(Samples.BIG_BUCK_BUNNY_720P_1MB)).get();
        assertFalse(info.hasError());

        // Only a quick sanity check until we do something better
        assertEquals(2, info.getStreams().size());
        assertEquals(FFmpegCodecType.VIDEO, info.getStreams().get(0).getCodecType());
        assertEquals(FFmpegCodecType.AUDIO, info.getStreams().get(1).getCodecType());

        assertEquals(6, info.getStreams().get(1).getChannels());
        assertEquals(48_000, info.getStreams().get(1).getSampleRate());

        assertTrue(info.getChapters().isEmpty());
        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeBookWithChapters() throws IOException {
        FFmpegProbeResult info =
                ffprobe.probe(new File(Samples.BOOK_WITH_CHAPTERS)).get();
        assertFalse(info.hasError());
        assertEquals(24, info.getChapters().size());

        FFmpegChapter firstChapter = info.getChapters().get(0);
        assertEquals("1/44100", firstChapter.getTimeBase());
        assertEquals(0L, firstChapter.getStart());
        assertEquals("0.000000", firstChapter.getStartTime());
        assertEquals(11951309L, firstChapter.getEnd());
        assertEquals("271.004739", firstChapter.getEndTime());
        assertEquals("01 - Sammy Jay Makes a Fuss", firstChapter.getTags().getTitle());

        FFmpegChapter lastChapter = info.getChapters().get(info.getChapters().size() - 1);
        assertEquals("1/44100", lastChapter.getTimeBase());
        assertEquals(237875790L, lastChapter.getStart());
        assertEquals("5394.008844", lastChapter.getStartTime());
        assertEquals(248628224L, lastChapter.getEnd());
        assertEquals("5637.828209", lastChapter.getEndTime());
        assertEquals(
                "24 - Chatterer Has His Turn to Laugh", lastChapter.getTags().getTitle());
    }

    @Test
    public void testProbeVideo2() throws IOException {
        FFmpegProbeResult info =
                ffprobe.probe(new File(Samples.ALWAYS_ON_MY_MIND)).get();
        assertFalse(info.hasError());

        // Only a quick sanity check until we do something better
        assertEquals(2, info.getStreams().size());
        assertEquals(FFmpegCodecType.VIDEO, info.getStreams().get(0).getCodecType());
        assertEquals(FFmpegCodecType.AUDIO, info.getStreams().get(1).getCodecType());

        assertEquals(2, info.getStreams().get(1).getChannels());
        assertEquals(48_000, info.getStreams().get(1).getSampleRate());

        // Test a UTF-8 name
        assertEquals(
                "c:\\Users\\Bob\\Always On My Mind [Program Only] - Adelén.mp4",
                info.getFormat().getFilename());

        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeStartPts() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(new File(Samples.START_PTS_TEST)).get();
        assertFalse(info.hasError());

        // Check edge case with a time larger than an integer
        assertEquals(8570867078L, info.getStreams().get(0).getStartPts());
    }

    @Test
    public void testProbeDivideByZero() throws IOException {
        // https://github.com/bramp/ffmpeg-cli-wrapper/issues/10
        FFmpegProbeResult info = ffprobe.probe(new File(Samples.DIVIDE_BY_ZERO)).get();
        assertFalse(info.hasError());

        assertEquals(Fraction.ZERO, info.getStreams().get(1).getCodecTimeBase());

        // System.out.println(FFmpegUtils.getGson().toJson(info));
    }

    @Test
    public void testProbeSideDataList() throws IOException {
        FFmpegProbeResult info = ffprobe.probe(new File(Samples.SIDE_DATA_LIST)).get();

        // Check edge case with a time larger than an integer
        assertEquals(1, info.getStreams().get(0).getSideDataList().length);
        assertEquals(
                "Display Matrix", info.getStreams().get(0).getSideDataList()[0].getSideDataType());
        assertEquals(
                "\n00000000:            0      -65536           0\n00000001:        65536           0           0\n00000002:            0           0  1073741824\n",
                info.getStreams().get(0).getSideDataList()[0].getDisplayMatrix());
        assertEquals(90, info.getStreams().get(0).getSideDataList()[0].getRotation());
    }

    @Test
    public void probeJsonFileOverloadBuildsCommandAndForwardsStderr() throws Exception {
        File media = new File("relative media.mp4");
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(process("{\"answer\":42}", "warning", 0));

        assertEquals(
                "{\"answer\":42}",
                ffprobe.probeJson(media, FFMpegStreamConsumer.toOutputStream(stderr), "-select_streams", "v:0")
                        .get());

        verify(runFunc)
                .createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.eq(List.of(
                                ffprobe.getPath().getAbsolutePath(),
                                "-v",
                                "quiet",
                                "-select_streams",
                                "v:0",
                                "-print_format",
                                "json",
                                "-show_error",
                                "-show_format",
                                "-show_streams",
                                "-show_chapters",
                                media.getAbsolutePath())));
        assertEquals("warning", stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void probeWithStrictBuildsCommand() throws Exception {
        File media = new File("media.mp4");
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(process("{}", "", 0));

        assertSame(ffprobe, ffprobe.setStrict(Strict.EXPERIMENTAL));
        assertEquals(Strict.EXPERIMENTAL, ffprobe.getStrict());
        ffprobe.probeJson(media, FFMpegStreamConsumer.noop()).get();

        verify(runFunc)
                .createProcess(
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.eq(List.of(
                                ffprobe.getPath().getAbsolutePath(),
                                "-v",
                                "quiet",
                                "-strict",
                                "experimental",
                                "-print_format",
                                "json",
                                "-show_error",
                                "-show_format",
                                "-show_streams",
                                "-show_chapters",
                                media.getAbsolutePath())));
    }

    @Test
    public void genericStringOverloadUsesPathVerbatimAndRequestedClass() throws Exception {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(process("{\"value\":\"ok\"}", "", 0));

        ProbeValue result = ffprobe.probe("pipe:0", FFMpegStreamConsumer.noop(), ProbeValue.class, "-show_data")
                .get();

        assertEquals("ok", result.value);
        verify(runFunc).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("pipe:0"));
        verify(runFunc).createProcess(Mockito.any(), Mockito.any(), argThatHasItem("-show_data"));
    }

    @Test
    public void probeReportsProcessFailureAfterPipingStderr() throws Exception {
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(process("{}", "probe failed", 9));

        IOException failure = assertThrows(
                IOException.class,
                () -> ffprobe.probeJson(new File("media"), FFMpegStreamConsumer.toOutputStream(stderr))
                        .get());

        assertTrue(failure.getMessage().contains("exited with code 9"));
        assertEquals("probe failed", stderr.toString(StandardCharsets.UTF_8));
    }

    @Test
    public void probeReportsInvalidJson() throws Exception {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(process("not json", "", 0));

        IOException failure = assertThrows(
                IOException.class,
                () -> ffprobe.probeJson(new File("media"), FFMpegStreamConsumer.noop())
                        .get());

        assertNotNull(failure.getCause());
    }

    @Test
    public void probeRejectsNullProcessFromFactory() throws Exception {
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class, () -> ffprobe.probeJson(new File("media"), FFMpegStreamConsumer.noop()));
    }

    @Test
    public void probeClosesBlockingInputAfterJsonParsingFails() throws Exception {
        BlockingInputStream media = new BlockingInputStream();
        InputStream malformedJson = new ByteArrayInputStream("not json".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public int read(byte[] target, int offset, int length) {
                try {
                    assertTrue(media.awaitRead(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail(e);
                }
                return super.read(target, offset, length);
            }
        };
        when(runFunc.createProcess(Mockito.any(), Mockito.any(), Mockito.anyList()))
                .thenReturn(
                        new MockProcess(OutputStream.nullOutputStream(), malformedJson, InputStream.nullInputStream()));

        IOException failure = assertThrows(
                IOException.class,
                () -> ffprobe.probe(media, FFMpegStreamConsumer.noop(), ProbeValue.class)
                        .get());

        assertNotNull(failure.getCause());
        assertTrue(media.awaitClosed(5, TimeUnit.SECONDS));
    }

    @Test
    public void probeClosesProcessWhenExecutorRejectsJob() throws Exception {
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
        FFprobe rejectingProbe = new FFprobe(
                command -> {
                    throw new RejectedExecutionException("rejected");
                },
                FFMpegLogger.noop(),
                ffprobe.getPath(),
                processFactory);

        assertThrows(
                RejectedExecutionException.class,
                () -> rejectingProbe.probeJson(new File("media"), FFMpegStreamConsumer.noop()));

        assertTrue(processClosed.get());
    }

    private static MockProcess process(String stdout, String stderr, int exitCode) {
        return new MockProcess(
                OutputStream.nullOutputStream(),
                new ByteArrayInputStream(stdout.getBytes(StandardCharsets.UTF_8)),
                new ByteArrayInputStream(stderr.getBytes(StandardCharsets.UTF_8)),
                true,
                exitCode);
    }

    private static class ProbeValue {
        String value;
    }

    private static final class BlockingInputStream extends InputStream {
        private final CountDownLatch reading = new CountDownLatch(1);
        private final CountDownLatch closed = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            reading.countDown();
            try {
                closed.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException(e);
            }
            throw new IOException("closed");
        }

        @Override
        public void close() {
            closed.countDown();
        }

        private boolean awaitRead(long timeout, TimeUnit unit) throws InterruptedException {
            return reading.await(timeout, unit);
        }

        private boolean awaitClosed(long timeout, TimeUnit unit) throws InterruptedException {
            return closed.await(timeout, unit);
        }
    }
}
