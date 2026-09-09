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

    public FFmpeg(File ffmpegBinary) throws IOException {
        this(getDefaultExecutor(), FFMpegLogger.noop(), ffmpegBinary, FFMpegProcessFactory.defaultFactory());
    }

    public FFmpeg(Executor executor, File ffmpegBinary) throws IOException {
        this(executor, FFMpegLogger.noop(), ffmpegBinary, FFMpegProcessFactory.defaultFactory());
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
                var errorReader = pipe1(p.stderr(), FFMpegStreamConsumer.noop(), true);

                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = CODECS_REGEX.matcher(line);
                    if (!m.matches()) continue;

                    newCodecs.add(new Codec(m.group(2), m.group(3), m.group(1)));
                }

                waitAndThrowOnError(errorReader);
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
                var errorReader = pipe1(p.stderr(), FFMpegStreamConsumer.noop(), true);
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = FORMATS_REGEX.matcher(line);
                    if (!m.matches()) continue;

                    newFormats.add(new Format(m.group(2), m.group(3), m.group(1)));
                }

                waitAndThrowOnError(errorReader);
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
                var errorReader = pipe1(p.stderr(), FFMpegStreamConsumer.noop(), true);
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = PIXEL_FORMATS_REGEX.matcher(line);
                    if (!m.matches()) continue;
                    String flags = m.group(1);

                    newPixelFormats.add(new PixelFormat(
                            m.group(2), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), flags));
                }

                waitAndThrowOnError(errorReader);
                throwOnError(p);
                this.pixelFormats = Collections.unmodifiableList(newPixelFormats);
            }
        }

        return pixelFormats;
    }

    /**
     * Runs FFmpeg asynchronously with the supplied arguments and stream handlers.
     *
     * <p>The process is stopped if a stream consumer fails. Once the process exits successfully, the
     * consumer results are passed to {@code merger}. Processing failures are reported by {@link
     * FFMpegJob#get()}. Unless this method throws, {@code stdin} is closed when it is no longer needed.
     *
     * IMPORTANT:
     * This function assumes that the InputStream eventually runs EOF. The job will not complete
     * unless the InputStream reading either throws an exception (Such as socket timeout) or signals EOF.
     * The same holds true of any output consumer.
     *
     * @param args the arguments to pass to FFmpeg, excluding the binary path
     * @param allowAnyExitCode whether a non-zero process exit code should be accepted
     * @param stdout consumer for the process standard output
     * @param stderr consumer for the process standard error
     * @param merger function that combines the standard output and standard error consumer results
     * @param stdin input supplied to the process standard input
     * @param <A> standard output consumer result type
     * @param <B> standard error consumer result type
     * @param <T> merged job result type
     * @return a handle for awaiting, cancelling, or obtaining the result of the running process
     * @throws IOException if the FFmpeg process cannot be started
     */
    protected <A, B, T> FFMpegJob<T> runJob(
            List<String> args,
            boolean allowAnyExitCode,
            FFMpegStreamConsumer<A> stdout,
            FFMpegStreamConsumer<B> stderr,
            BiFunction<A, B, T> merger,
            InputStream stdin)
            throws IOException {
        InputStream toClose = stdin;
        FFMpegProcess toCloseProcess = null;
        try {
            Objects.requireNonNull(args);
            Objects.requireNonNull(stdout);
            Objects.requireNonNull(stderr);
            Objects.requireNonNull(merger);

            Thread thr = Thread.currentThread();
            CompletableFuture<T> future = new CompletableFuture<>();
            FFMpegProcess p = runFunc.createProcess(executor, logger, path(args));
            toCloseProcess = p;
            executor.execute(() -> {
                if (thr == Thread.currentThread()) {
                    p.close();
                    throw new IllegalStateException("Bad executor");
                }

                try (p;
                        stdin) {
                    var pipeResult = pipe3(
                            p.stdout(),
                            stdout,
                            p.stderr(),
                            stderr,
                            stdin,
                            FFMpegStreamConsumer.toOutputStream(p.stdin()));

                    throwOnError(p, allowAnyExitCode);
                    future.complete(merger.apply(pipeResult.a(), pipeResult.b()));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            toCloseProcess = null;
            toClose = null;
            return new BasicFFMpegJob<>(future, p, stdin);
        } finally {
            closeSilently(toClose);
            closeSilently(toCloseProcess);
        }
    }

    /**
     * Runs the command produced by {@code builder} asynchronously and discards standard output and
     * standard error.
     *
     * @param builder builder that supplies the FFmpeg arguments
     * @return a handle for the running process
     * @throws IOException if the FFmpeg process cannot be started
     */
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

    /**
     * Runs the command produced by {@code builder} asynchronously, consumes standard output, and
     * discards standard error.
     *
     * Note: The job will not complete unless the FFMpegStreamConsumer returns.
     * Even killing the job does nothing to unblock a FFMpegStreamConsumer.
     *
     * @param builder builder that supplies the FFmpeg arguments
     * @param stdOut consumer for the process standard output
     * @param <T> standard output consumer result type
     * @return a handle whose result is produced by {@code stdOut}
     * @throws IOException if the FFmpeg process cannot be started
     */
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

    /**
     * Runs the command produced by {@code builder} asynchronously, consumes standard error, and
     * discards standard output.
     *
     * Note: The job will not complete unless the FFMpegStreamConsumer returns.
     * Even killing the job does nothing to unblock a FFMpegStreamConsumer.
     *
     * @param builder builder that supplies the FFmpeg arguments
     * @param stderr consumer for the process standard error
     * @param <T> standard error consumer result type
     * @return a handle whose result is produced by {@code stderr}
     * @throws IOException if the FFmpeg process cannot be started
     */
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
     * Runs the command produced by {@code builder} asynchronously and consumes standard output and
     * standard error concurrently.
     *
     * <p>After FFmpeg exits, {@code merger} combines the two consumer results. If a consumer fails, the
     * process is stopped as soon as possible. Processing failures are reported by {@link FFMpegJob#get()};
     * if multiple operations fail, only the first failure is reported.
     *
     * @param builder builder that supplies the FFmpeg arguments
     * @param stdOut consumer for the process standard output
     * @param stderr consumer for the process standard error
     * @param merger function that combines the standard output and standard error consumer results
     * @param <T> merged job result type
     * @param <A> standard output consumer result type
     * @param <B> standard error consumer result type
     * @return a handle for awaiting, cancelling, or obtaining the result of the running process
     * @throws IOException if the FFmpeg process cannot be started
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
     * Runs the command produced by {@code builder} asynchronously, consumes standard output and
     * standard error concurrently, and supplies {@code stdin} to the process.
     *
     * <p>After FFmpeg exits, {@code merger} combines the two consumer results. If a consumer fails, the
     * process is stopped as soon as possible. Processing failures are reported by {@link FFMpegJob#get()};
     * if multiple operations fail, only the first failure is reported. Unless this method throws,
     * {@code stdin} is closed when it is no longer needed.
     *
     * Note: The job will not complete unless the FFMpegStreamConsumers and BiFunction return.
     * Even killing the job does nothing to unblock a FFMpegStreamConsumer or BiFunction.
     *
     * IMPORTANT:
     * This function assumes that the InputStream eventually runs EOF. The job will not complete
     * unless the InputStream reading either throws an exception (Such as socket timeout) or signals EOF.
     *
     * @param builder builder that supplies the FFmpeg arguments
     * @param stdOut consumer for the process standard output
     * @param stderr consumer for the process standard error
     * @param merger function that combines the standard output and standard error consumer results
     * @param stdin input supplied to the process standard input
     * @param <T> merged job result type
     * @param <A> standard output consumer result type
     * @param <B> standard error consumer result type
     * @return a handle for awaiting, cancelling, or obtaining the result of the running process
     * @throws IOException if the FFmpeg process cannot be started
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
