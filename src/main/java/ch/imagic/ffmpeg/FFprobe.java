package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.builder.Strict;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.process.FFMpegProcess;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;

/**
 * Wrapper around FFprobe
 *
 */
public class FFprobe extends FFcommon {

    static final Gson gson = FFmpegUtils.getGson();

    protected Strict strict;

    public FFprobe() throws IOException {
        this(
                getDefaultExecutor(),
                FFMpegLogger.noop(),
                getDefaultFfprobeBinary(),
                FFMpegProcessFactory.defaultFactory());
    }

    public FFprobe(File ffmpegBinary) throws IOException {
        this(getDefaultExecutor(), FFMpegLogger.noop(), ffmpegBinary, FFMpegProcessFactory.defaultFactory());
    }

    public FFprobe(Executor executor, File ffmpegBinary) throws IOException {
        this(executor, FFMpegLogger.noop(), ffmpegBinary, FFMpegProcessFactory.defaultFactory());
    }

    public FFprobe(Executor executor, FFMpegLogger logger, File ffmpegBinary) throws IOException {
        this(executor, logger, ffmpegBinary, FFMpegProcessFactory.defaultFactory());
    }

    public FFprobe(File ffmpegBinary, FFMpegProcessFactory processFactory) throws IOException {
        this(getDefaultExecutor(), FFMpegLogger.noop(), ffmpegBinary, processFactory);
    }

    public FFprobe(FFMpegProcessFactory processFactory) throws IOException {
        this(getDefaultExecutor(), FFMpegLogger.noop(), getDefaultFfprobeBinary(), processFactory);
    }
    /**
     * Creates a FFProbe execution instance.
     *
     * @param executor executor that will be used to create asynchronous tasks to monitor the status of the ffmpeg binary.
     *                 The executor MUST be capable of running at least 4 more tasks in parallel per concurrent execution.
     * @param logger logger facade used for logging.
     * @param ffprobeBinary File to the binary. NOTE: java.io.File#getAbsolutePath will be directly fed into ProcessBuilder and executed, DO NOT USE BINARIES OR PATHS YOU DON'T TRUST
     * @param processFactory The factory function for the process. BasicRunFFMpegProcessFactory is sufficient for most uses.
     * @throws IOException if an error occurs determining the ffmpeg version.
     */
    public FFprobe(Executor executor, FFMpegLogger logger, File ffprobeBinary, FFMpegProcessFactory processFactory)
            throws IOException {
        super(executor, logger, ffprobeBinary, processFactory);
    }

    /**
     * Probes a media file asynchronously and discards standard error.
     *
     * @param mediaPath media file to probe
     * @return a handle whose result contains the detected media information
     * @throws IOException if the FFprobe process cannot be started
     */
    public FFMpegJob<FFmpegProbeResult> probe(File mediaPath) throws IOException {
        return probe(mediaPath, FFMpegStreamConsumer.noop());
    }

    /**
     * Returns true if the binary we are using is the true ffprobe. This is to avoid conflict with
     * avprobe (from the libav project), that some symlink to ffprobe.
     *
     * @return true iff this is the official ffprobe binary.
     * @throws IOException If a I/O error occurs while executing ffprobe.
     */
    public boolean isFFprobe() throws IOException {
        return version().startsWith("ffprobe");
    }

    public Strict getStrict() {
        return strict;
    }

    public FFprobe setStrict(Strict strict) {
        this.strict = strict;
        return this;
    }

    /**
     * Probes a media path asynchronously, parses FFprobe's JSON output, and maps the parsed JSON to a
     * result.
     *
     * <p>The additional arguments are inserted before the output and media-path arguments managed by
     * this class. Processing and mapping failures are reported by {@link FFMpegJob#get()}.
     *
     * @param mediaPath media path to pass to FFprobe
     * @param stdErr consumer for the process standard error
     * @param mapper function that maps FFprobe's parsed JSON output to the job result
     * @param additionalArguments additional arguments to pass to FFprobe
     * @param <T> mapped job result type
     * @return a handle for awaiting, cancelling, or obtaining the result of the running process
     * @throws IOException if the FFprobe process cannot be started
     */
    protected <T> FFMpegJob<T> probeGson(
            String mediaPath,
            FFMpegStreamConsumer<Void> stdErr,
            Function<JsonElement, T> mapper,
            InputStream stdin,
            String... additionalArguments)
            throws IOException {
        InputStream toClose = stdin;
        FFMpegProcess toCloseProcess = null;
        try {
            Objects.requireNonNull(mapper);
            List<String> args = new ArrayList<>();

            // TODO Add:
            // .add("--show_packets")
            // .add("--show_frames")

            args.addAll(List.of(getAbsolutePath(), "-v", "quiet"));
            if (strict != null) {
                args.addAll(List.of("-strict", strict.toString()));
            }
            args.addAll(Arrays.asList(additionalArguments));
            args.addAll(List.of(
                    "-print_format",
                    "json",
                    "-show_error",
                    "-show_format",
                    "-show_streams",
                    "-show_chapters",
                    mediaPath));

            CompletableFuture<T> future = new CompletableFuture<>();
            FFMpegProcess p = runFunc.createProcess(executor, logger, List.copyOf(args));
            toCloseProcess = p;
            if (p == null) {
                throw new IllegalStateException("FFMpegProcessFactory returned null");
            }
            Thread thr = Thread.currentThread();
            executor.execute(() -> {
                try (p;
                        stdin;
                        BufferedReader r = p.stdoutReader()) {
                    if (thr == Thread.currentThread()) {
                        throw new IllegalStateException("Bad executor");
                    }

                    // We don't need to consume the entire input if ffprobe is done earlier.
                    var stdinReader = pipe1(stdin, FFMpegStreamConsumer.toOutputStream(p.stdin()), false);

                    var errorReader = pipe1(p.stderr(), stdErr, true);
                    JsonElement element = JsonParser.parseReader(r);

                    throwOnError(p);
                    waitAndThrowOnError(errorReader);
                    checkNowAndThrowOnError(stdinReader);
                    if (!stdinReader.isDone()) {
                        stdin.close();
                    }
                    waitAndIgnoreError(stdinReader);

                    future.complete(mapper.apply(element));
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
            toClose = null;
            toCloseProcess = null;
            return new BasicFFMpegJob<>(future, p, stdin);
        } finally {
            closeSilently(toClose);
            closeSilently(toCloseProcess);
        }
    }

    /**
     * Probes a media file asynchronously and returns FFprobe's JSON result.
     *
     * @param mediaPath media file to probe
     * @param stdErr consumer for the process standard error
     * @param additionalArguments additional arguments to pass to FFprobe
     * @return a handle whose result is the JSON emitted by FFprobe
     * @throws IOException if the FFprobe process cannot be started
     */
    public FFMpegJob<String> probeJson(File mediaPath, FFMpegStreamConsumer<Void> stdErr, String... additionalArguments)
            throws IOException {
        return probeGson(
                mediaPath.getAbsolutePath(),
                stdErr,
                JsonElement::toString,
                InputStream.nullInputStream(),
                additionalArguments);
    }

    /**
     * Probes a media file asynchronously and maps FFprobe's JSON output to an {@link FFmpegProbeResult}.
     *
     * @param mediaPath media file to probe
     * @param stdErr consumer for the process standard error
     * @param additionalArguments additional arguments to pass to FFprobe
     * @return a handle whose result contains the detected media information
     * @throws IOException if the FFprobe process cannot be started
     */
    public FFMpegJob<FFmpegProbeResult> probe(
            File mediaPath, FFMpegStreamConsumer<Void> stdErr, String... additionalArguments) throws IOException {
        return probe(mediaPath, stdErr, FFmpegProbeResult.class, additionalArguments);
    }

    /**
     * Probes a media file asynchronously and deserializes FFprobe's JSON output as {@code resultClass}.
     *
     * @param mediaPath media file to probe
     * @param stdErr consumer for the process standard error
     * @param resultClass class into which the JSON output is deserialized
     * @param additionalArguments additional arguments to pass to FFprobe
     * @param <T> deserialized job result type
     * @return a handle whose result is an instance of {@code resultClass}
     * @throws IOException if the FFprobe process cannot be started
     */
    public <T> FFMpegJob<T> probe(
            File mediaPath, FFMpegStreamConsumer<Void> stdErr, Class<T> resultClass, String... additionalArguments)
            throws IOException {
        return probeGson(
                mediaPath.getAbsolutePath(),
                stdErr,
                elem -> gson.fromJson(elem, resultClass),
                InputStream.nullInputStream(),
                additionalArguments);
    }

    /**
     * Probes a media path asynchronously and deserializes FFprobe's JSON output as {@code resultClass}.
     *
     * <p>This overload accepts any media path or URL supported by FFprobe.
     *
     * @param mediaPath media path or URL to pass to FFprobe
     * @param stdErr consumer for the process standard error
     * @param resultClass class into which the JSON output is deserialized
     * @param additionalArguments additional arguments to pass to FFprobe
     * @param <T> deserialized job result type
     * @return a handle whose result is an instance of {@code resultClass}
     * @throws IOException if the FFprobe process cannot be started
     */
    public <T> FFMpegJob<T> probe(
            String mediaPath, FFMpegStreamConsumer<Void> stdErr, Class<T> resultClass, String... additionalArguments)
            throws IOException {
        return probeGson(
                mediaPath,
                stdErr,
                elem -> gson.fromJson(elem, resultClass),
                InputStream.nullInputStream(),
                additionalArguments);
    }

    /**
     * Probes a media input stream asynchronously and deserializes FFprobe's JSON output as {@code resultClass}.
     *
     * <p>The media stream is passed to FFprobe through standard input. It is consumed asynchronously and always closed
     * by this method. FFprobe may finish probing before reaching the end of the stream, in which case the remaining data
     * may be fully or partially discarded.
     *
     * @param media media data to pass to FFprobe; ownership is transferred to this method
     * @param stdErr consumer for the process standard error
     * @param resultClass class into which the JSON output is deserialized
     * @param additionalArguments additional arguments to pass to FFprobe
     * @param <T> deserialized job result type
     * @return a handle for awaiting, cancelling, or obtaining the result of the running process
     * @throws IOException if the FFprobe process cannot be started or the media stream cannot be closed after startup
     */
    public <T> FFMpegJob<T> probe(
            InputStream media, FFMpegStreamConsumer<Void> stdErr, Class<T> resultClass, String... additionalArguments)
            throws IOException {
        return probeGson("-", stdErr, elem -> gson.fromJson(elem, resultClass), media, additionalArguments);
    }
}
