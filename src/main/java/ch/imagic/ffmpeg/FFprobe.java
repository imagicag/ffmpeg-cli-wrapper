package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.process.FFMpegProcess;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Wrapper around FFprobe
 *
 */
public class FFprobe extends FFcommon {

    static final Gson gson = FFmpegUtils.getGson();

    public FFprobe() throws IOException {
        this(
                getDefaultExecutor(),
                FFMpegLogger.noop(),
                getDefaultFfprobeBinary(),
                FFMpegProcessFactory.defaultFactory());
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

    public FFmpegProbeResult probe(String mediaPath) throws IOException {
        return probe(mediaPath, null);
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

    /**
     * Throws an exception if this is an unsupported version of ffprobe.
     *
     * @throws IllegalArgumentException if this is not the official ffprobe binary.
     * @throws IOException If a I/O error occurs while executing ffprobe.
     */
    private void checkIfFFprobe() throws IllegalArgumentException, IOException {
        if (!isFFprobe()) {
            throw new IllegalArgumentException("This binary '" + path + "' is not a supported version of ffprobe");
        }
    }

    @Override
    public void run(List<String> args) throws IOException {
        checkIfFFprobe();
        super.run(args);
    }

    protected JsonElement probeGson(String mediaPath, String userAgent) throws IOException {
        checkIfFFprobe();

        List<String> args = new ArrayList<>();

        // TODO Add:
        // .add("--show_packets")
        // .add("--show_frames")

        args.addAll(List.of(getAbsolutePath(), "-v", "quiet"));

        if (userAgent != null) {
            args.addAll(List.of("-user_agent", userAgent));
        }

        args.addAll(List.of(
                "-print_format", "json", "-show_error", "-show_format", "-show_streams", "-show_chapters", mediaPath));

        try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.copyOf(args));
                BufferedReader r = p.stdoutReader()) {
            var errorReader = pipeOne(p.stderr(), OutputStream.nullOutputStream());
            JsonElement element = JsonParser.parseReader(r);

            waitAndthrowOnError(errorReader);
            throwOnError(p);
            return element;
        }
    }

    public String probeJson(String mediaPath, String userAgent) throws IOException {
        return probeGson(mediaPath, userAgent).toString();
    }

    public FFmpegProbeResult probe(String mediaPath, String userAgent) throws IOException {
        return probe(mediaPath, userAgent, FFmpegProbeResult.class);
    }

    public <T> T probe(String mediaPath, String userAgent, Class<T> clazz) throws IOException {
        JsonElement elem = probeGson(mediaPath, userAgent);
        return gson.fromJson(elem, clazz);
    }
}
