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

    protected <T> FFMpegJob<T> probeGson(
            String mediaPath,
            FFMpegStreamConsumer<Void> stdErr,
            Function<JsonElement, T> mapper,
            String... additionalArguments)
            throws IOException {
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
                "-print_format", "json", "-show_error", "-show_format", "-show_streams", "-show_chapters", mediaPath));

        CompletableFuture<T> future = new CompletableFuture<>();
        FFMpegProcess p = runFunc.createProcess(executor, logger, List.copyOf(args));
        if (p == null) {
            throw new IllegalStateException("FFMpegProcessFactory returned null");
        }
        Thread thr = Thread.currentThread();
        executor.execute(() -> {
            try (p;
                    BufferedReader r = p.stdoutReader()) {
                if (thr == Thread.currentThread()) {
                    throw new IllegalStateException("Bad executor");
                }
                var errorReader = pipe1(p.stderr(), stdErr);
                JsonElement element = JsonParser.parseReader(r);

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                future.complete(mapper.apply(element));
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        });

        return new BasicFFMpegJob<>(future, p);
    }

    public FFMpegJob<String> probeJson(File mediaPath, FFMpegStreamConsumer<Void> stdErr, String... additionalArguments)
            throws IOException {
        return probeGson(mediaPath.getAbsolutePath(), stdErr, JsonElement::toString, additionalArguments);
    }

    public FFMpegJob<FFmpegProbeResult> probe(
            File mediaPath, FFMpegStreamConsumer<Void> stdErr, String... additionalArguments) throws IOException {
        return probe(mediaPath, stdErr, FFmpegProbeResult.class, additionalArguments);
    }

    public <T> FFMpegJob<T> probe(
            File mediaPath, FFMpegStreamConsumer<Void> stdErr, Class<T> resultClass, String... additionalArguments)
            throws IOException {
        return probeGson(
                mediaPath.getAbsolutePath(), stdErr, elem -> gson.fromJson(elem, resultClass), additionalArguments);
    }

    public <T> FFMpegJob<T> probe(
            String mediaPath, FFMpegStreamConsumer<Void> stdErr, Class<T> resultClass, String... additionalArguments)
            throws IOException {
        return probeGson(mediaPath, stdErr, elem -> gson.fromJson(elem, resultClass), additionalArguments);
    }
}
