package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.info.Codec;
import ch.imagic.ffmpeg.info.Format;
import ch.imagic.ffmpeg.info.PixelFormat;
import ch.imagic.ffmpeg.nut.Fraction;
import ch.imagic.ffmpeg.process.FFMpegProcess;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import ch.imagic.ffmpeg.progress.ProgressListener;
import ch.imagic.ffmpeg.progress.ProgressParser;
import ch.imagic.ffmpeg.progress.TcpProgressParser;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrapper around FFmpeg
 *
 */
public class FFmpeg extends FFcommon {

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
        version();
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

    /**
     * Throws an exception if this is an unsupported version of ffmpeg.
     *
     * @throws IllegalArgumentException if this is not the official ffmpeg binary.
     * @throws IOException If a I/O error occurs while executing ffmpeg.
     */
    private void checkIfFFmpeg() throws IllegalArgumentException, IOException {
        if (!isFFmpeg()) {
            throw new IllegalArgumentException("This binary '" + path + "' is not a supported version of ffmpeg");
        }
    }

    public synchronized List<Codec> codecs() throws IOException {
        checkIfFFmpeg();

        if (this.codecs == null) {
            codecs = new ArrayList<>();

            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-codecs"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipeOne(p.stderr(), OutputStream.nullOutputStream());

                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = CODECS_REGEX.matcher(line);
                    if (!m.matches()) continue;

                    codecs.add(new Codec(m.group(2), m.group(3), m.group(1)));
                }

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                this.codecs = List.copyOf(codecs);
            }
        }

        return codecs;
    }

    public synchronized List<Format> formats() throws IOException {
        checkIfFFmpeg();

        if (this.formats == null) {
            formats = new ArrayList<>();
            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-formats"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipeOne(p.stderr(), OutputStream.nullOutputStream());
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = FORMATS_REGEX.matcher(line);
                    if (!m.matches()) continue;

                    formats.add(new Format(m.group(2), m.group(3), m.group(1)));
                }

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                this.formats = List.copyOf(formats);
            }
        }
        return formats;
    }

    public synchronized List<PixelFormat> pixelFormats() throws IOException {
        checkIfFFmpeg();

        if (this.pixelFormats == null) {
            pixelFormats = new ArrayList<>();

            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-pix_fmts"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipeOne(p.stderr(), OutputStream.nullOutputStream());
                String line;
                while ((line = r.readLine()) != null) {
                    Matcher m = PIXEL_FORMATS_REGEX.matcher(line);
                    if (!m.matches()) continue;
                    String flags = m.group(1);

                    pixelFormats.add(new PixelFormat(
                            m.group(2), Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), flags));
                }

                waitAndthrowOnError(errorReader);
                throwOnError(p);
                this.pixelFormats = List.copyOf(pixelFormats);
            }
        }

        return pixelFormats;
    }

    protected ProgressParser createProgressParser(ProgressListener listener) throws IOException {
        // TODO In future create the best kind for this OS, unix socket, named pipe, or TCP.
        try {
            // Default to TCP because it is supported across all OSes, and is better than UDP because it
            // provides good properties such as in-order packets, reliability, error checking, etc.
            return new TcpProgressParser(Objects.requireNonNull(listener));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void run(List<String> args) throws IOException {
        checkIfFFmpeg();
        super.run(args);
    }

    public void run(FFmpegBuilder builder) throws IOException {
        run(builder, null);
    }

    public void run(FFmpegBuilder builder, ProgressListener listener) throws IOException {
        Objects.requireNonNull(builder);

        if (listener != null) {
            try (ProgressParser progressParser = createProgressParser(listener)) {
                progressParser.start();
                builder = builder.addProgress(progressParser.getUri());

                run(builder.build());
            }
        } else {
            run(builder.build());
        }
    }

    public FFmpegBuilder builder() {
        return new FFmpegBuilder();
    }
}
