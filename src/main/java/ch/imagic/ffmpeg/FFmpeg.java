package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.info.Codec;
import ch.imagic.ffmpeg.info.Format;
import ch.imagic.ffmpeg.info.PixelFormat;
import ch.imagic.ffmpeg.probe.Fraction;
import ch.imagic.ffmpeg.process.FFMpegProcess;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper around FFmpeg
 *
 */
public class FFmpeg extends FFcommon {

    protected static <A, B> BiFunction<A, B, A> first() {
        return (a, _b) -> a;
    }

    public static final Fraction FPS_30 = Fraction.getFraction(30, 1);
    public static final Fraction FPS_29_97 = Fraction.getFraction(30000, 1001);
    public static final Fraction FPS_24 = Fraction.getFraction(24, 1);
    public static final Fraction FPS_23_976 = Fraction.getFraction(24000, 1001);

    public static final int AUDIO_MONO = 1;
    public static final int AUDIO_STEREO = 2;

    public static final String AUDIO_FORMAT_U8 = "u8"; // 8
    public static final String AUDIO_FORMAT_S16 = "s16"; // 16
    public static final String AUDIO_FORMAT_S32 = "s32"; // 32
    public static final String AUDIO_FORMAT_FLT = "flt"; // 32
    public static final String AUDIO_FORMAT_DBL = "dbl"; // 64

    @Deprecated
    public static final String AUDIO_DEPTH_U8 = AUDIO_FORMAT_U8;

    @Deprecated
    public static final String AUDIO_DEPTH_S16 = AUDIO_FORMAT_S16;

    @Deprecated
    public static final String AUDIO_DEPTH_S32 = AUDIO_FORMAT_S32;

    @Deprecated
    public static final String AUDIO_DEPTH_FLT = AUDIO_FORMAT_FLT;

    @Deprecated
    public static final String AUDIO_DEPTH_DBL = AUDIO_FORMAT_DBL;

    public static final int AUDIO_SAMPLE_8000 = 8000;
    public static final int AUDIO_SAMPLE_11025 = 11025;
    public static final int AUDIO_SAMPLE_12000 = 12000;
    public static final int AUDIO_SAMPLE_16000 = 16000;
    public static final int AUDIO_SAMPLE_22050 = 22050;
    public static final int AUDIO_SAMPLE_32000 = 32000;
    public static final int AUDIO_SAMPLE_44100 = 44100;
    public static final int AUDIO_SAMPLE_48000 = 48000;
    public static final int AUDIO_SAMPLE_96000 = 96000;

    static final Pattern CODECS_REGEX = Pattern.compile("^ ([.D][.E][VASD][.I][.L][.S]) (\\S{2,})\\s+(.*)$");
    static final Pattern FORMATS_REGEX = Pattern.compile("^ ([ D][ E]) (\\S+)\\s+(.*)$");
    static final Pattern PIXEL_FORMATS_REGEX =
            Pattern.compile("^([.I][.O][.H][.P][.B]) (\\S{2,})\\s+(\\d+)\\s+(\\d+)$");

    /** Supported codecs */
    List<Codec> codecs = null;

    /** Supported formats */
    List<Format> formats = null;

    /** Supported pixel formats */
    private List<PixelFormat> pixelFormats = null;

    public FFmpeg() throws IOException {
        this(
                getDefaultExecutor(),
                FFMpegLogger.noop(),
                getDefaultFFMPEGBinary(),
                FFMpegProcessFactory.defaultFactory());
    }

    public FFmpeg(File ffmpegBinary, FFMpegProcessFactory processFactory) throws IOException {
        this(getDefaultExecutor(), FFMpegLogger.noop(), ffmpegBinary, processFactory);
    }

    public FFmpeg(FFMpegProcessFactory processFactory) throws IOException {
        this(getDefaultExecutor(), FFMpegLogger.noop(), getDefaultFFMPEGBinary(), processFactory);
    }

    /**
     * Creates a FFMPEG execution instance.
     *
     * @param executor executor that will be used to create asynchronous tasks to monitor the status of the ffmpeg binary.
     *                 The executor MUST be capable of running at least 4 more tasks in parallel per concurrent execution.
     * @param logger logger facade used for logging.
     * @param ffmpegBinary File to the binary. NOTE: java.io.File#getAbsolutePath will be directly fed into ProcessBuilder and executed, DO NOT USE BINARIES OR PATHS YOU DON'T TRUST
     * @param processFactory The factory function for the process. BasicRunFFMpegProcessFactory is sufficient for most uses.
     * @throws IOException if an error occurs determining the ffmpeg version.
     */
    public FFmpeg(Executor executor, FFMpegLogger logger, File ffmpegBinary, FFMpegProcessFactory processFactory)
            throws IOException {
        super(executor, logger, ffmpegBinary, processFactory);
    }

    /**
     * Returns true if the binary we are using is the true ffmpeg. This is to avoid conflict with
     * avconv (from the libav project), that some symlink to ffmpeg.
     *
     * @return true iff this is the official ffmpeg binary.
     * @throws IOException If a I/O error occurs while executing ffmpeg.
     */
    public boolean isFFmpeg() throws IOException {
        return version().startsWith("ffmpeg");
    }

    public synchronized List<Codec> codecs() throws IOException {
        if (this.codecs == null) {
            List<Codec> newCodecs = new ArrayList<>();

            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-codecs"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipe1(p.stderr(), FFMpegStreamConsumer.noop());

                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = CODECS_REGEX.matcher(line);
                    if (!m.matches()) continue;

                    newCodecs.add(new Codec(m.group(2), m.group(3), m.group(1)));
                }

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                this.codecs = Collections.unmodifiableList(newCodecs);
            }
        }

        return codecs;
    }

    public synchronized List<Format> formats() throws IOException {
        if (this.formats == null) {
            List<Format> newFormats = new ArrayList<>();
            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-formats"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipe1(p.stderr(), FFMpegStreamConsumer.noop());
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = FORMATS_REGEX.matcher(line);
                    if (!m.matches()) continue;

                    newFormats.add(new Format(m.group(2), m.group(3), m.group(1)));
                }

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                this.formats = Collections.unmodifiableList(newFormats);
            }
        }
        return formats;
    }

    public synchronized List<PixelFormat> pixelFormats() throws IOException {
        if (this.pixelFormats == null) {
            List<PixelFormat> newPixelFormats = new ArrayList<>();

            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-pix_fmts"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipe1(p.stderr(), FFMpegStreamConsumer.noop());
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = PIXEL_FORMATS_REGEX.matcher(line);
                    if (!m.matches()) continue;
                    String flags = m.group(1);

                    newPixelFormats.add(new PixelFormat(
                            m.group(2), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), flags));
                }

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                this.pixelFormats = Collections.unmodifiableList(newPixelFormats);
            }
        }

        return pixelFormats;
    }

    /**
     * Runs the binary (ffmpeg) with the supplied args.
     *
     * If the OutputStream throws an exception then the ffmpeg process is killed as soon as possible.
     *
     * The execution happens in the background and the returned FFMpegJob can be used to cancel or wait (with a timeout) for the job.
     *
     * Unless this function throws an exception the stdin InputStream is closed once it is no longer needed.
     *
     * @param args The arguments to pass to the binary.
     * @throws IOException If there is a problem executing the binary
     */
    protected <A, B, T> FFMpegJob<T> runJob(
            List<String> args,
            boolean allowAnyExitCode,
            FFMpegStreamConsumer<A> stdout,
            FFMpegStreamConsumer<B> stderr,
            BiFunction<A, B, T> merger,
            InputStream stdin)
            throws IOException {
        Objects.requireNonNull(args);
        Objects.requireNonNull(stdout);
        Objects.requireNonNull(stderr);
        Objects.requireNonNull(merger);

        Thread thr = Thread.currentThread();
        CompletableFuture<T> future = new CompletableFuture<>();
        FFMpegProcess p = runFunc.createProcess(executor, logger, path(args));

        executor.execute(() -> {
            if (thr == Thread.currentThread()) {
                p.close();
                throw new IllegalStateException("Bad executor");
            }

            try (p;
                    stdin) {
                var pipeResult = pipe3(
                        p.stdout(), stdout, p.stderr(), stderr, stdin, FFMpegStreamConsumer.toOutputStream(p.stdin()));
                throwOnError(p, allowAnyExitCode);
                future.complete(merger.apply(pipeResult.a(), pipeResult.b()));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return new BasicFFMpegJob<>(future, p);
    }

    public FFMpegJob<Void> run(FFmpegBuilder builder) throws IOException {
        Objects.requireNonNull(builder);
        return runJob(
                builder.build(),
                builder.getNoOutput(),
                FFMpegStreamConsumer.noop(),
                FFMpegStreamConsumer.noop(),
                first(),
                InputStream.nullInputStream());
    }

    public <T> FFMpegJob<T> run(FFmpegBuilder builder, FFMpegStreamConsumer<T> stdOut) throws IOException {
        Objects.requireNonNull(builder);
        return runJob(
                builder.build(),
                builder.getNoOutput(),
                stdOut,
                FFMpegStreamConsumer.noop(),
                first(),
                InputStream.nullInputStream());
    }

    public <T> FFMpegJob<T> runCaptureStderr(FFmpegBuilder builder, FFMpegStreamConsumer<T> stderr) throws IOException {
        Objects.requireNonNull(builder);
        return runJob(
                builder.build(),
                builder.getNoOutput(),
                FFMpegStreamConsumer.noop(),
                stderr,
                (ignored, result) -> result,
                InputStream.nullInputStream());
    }

    /**
     * Consumes stdOut and stdErr in separate threads and later once ffmpeg is finished merge the results using the merger function.
     * Any exception thrown is propagated to FFMpegJob#get, however only the first exception to occur is reported,
     * further exceptions are discarded. The ffmpeg process is already stopped/exited by the time the merger function is invoked.
     * If any of the stream consumers throw then that causes the ffmpeg process to be stopped soon after.
     *
     * This function returns as soon as the ffmpeg process was started.
     *
     * The job can be killed/canceled with the returned job handle.
     * Closing the job handle stops the ffmpeg process as soon as possible.
     */
    public <T, A, B> FFMpegJob<T> run(
            FFmpegBuilder builder,
            FFMpegStreamConsumer<A> stdOut,
            FFMpegStreamConsumer<B> stderr,
            BiFunction<A, B, T> merger)
            throws IOException {
        Objects.requireNonNull(builder);
        return runJob(builder.build(), builder.getNoOutput(), stdOut, stderr, merger, InputStream.nullInputStream());
    }

    /**
     * This function closes the stream parameters unless it throws an exception.
     */
    public <T, A, B> FFMpegJob<T> run(
            FFmpegBuilder builder,
            FFMpegStreamConsumer<A> stdOut,
            FFMpegStreamConsumer<B> stderr,
            BiFunction<A, B, T> merger,
            InputStream stdin)
            throws IOException {
        Objects.requireNonNull(builder);
        return runJob(builder.build(), builder.getNoOutput(), stdOut, stderr, merger, stdin);
    }

    public FFmpegBuilder builder() {
        return new FFmpegBuilder();
    }
}
