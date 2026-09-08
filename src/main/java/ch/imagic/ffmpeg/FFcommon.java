package ch.imagic.ffmpeg;

import ch.imagic.ffmpeg.process.FFMpegProcess;
import ch.imagic.ffmpeg.process.FFMpegProcessFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/** Private class to contain common methods for both FFmpeg and FFprobe. */
abstract class FFcommon {

    protected static volatile Executor DEFAULT_EXECUTOR = null;

    protected static Executor getDefaultExecutor() {
        if (DEFAULT_EXECUTOR == null) {
            synchronized (FFcommon.class) {
                if (DEFAULT_EXECUTOR == null) {
                    DEFAULT_EXECUTOR = Executors.newCachedThreadPool();
                }
            }
        }

        return DEFAULT_EXECUTOR;
    }

    protected static volatile File DEFAULT_FFMPEG_BINARY = null;

    protected static File getDefaultFFMPEGBinary() throws FileNotFoundException {
        if (DEFAULT_FFMPEG_BINARY == null) {
            synchronized (FFcommon.class) {
                if (DEFAULT_FFMPEG_BINARY == null) {
                    DEFAULT_FFMPEG_BINARY = getSystemBinary("ffmpeg");
                }
            }
        }

        return DEFAULT_FFMPEG_BINARY;
    }

    protected static volatile File DEFAULT_FFPROBE_BINARY = null;

    protected static File getDefaultFfprobeBinary() throws FileNotFoundException {
        if (DEFAULT_FFPROBE_BINARY == null) {
            synchronized (FFcommon.class) {
                if (DEFAULT_FFPROBE_BINARY == null) {
                    DEFAULT_FFPROBE_BINARY = getSystemBinary("ffprobe");
                }
            }
        }

        return DEFAULT_FFPROBE_BINARY;
    }

    protected static File getSystemBinary(String name) throws FileNotFoundException {
        return getSystemBinary(name, System.getenv("PATH"), File.separatorChar == '\\');
    }

    protected static File getSystemBinary(String name, String path, boolean isWindows) throws FileNotFoundException {
        Objects.requireNonNull(name, "name");
        if (path == null) {
            throw new FileNotFoundException("Could not find " + name + " because PATH env var is missing");
        }

        LinkedList<String> directories = new LinkedList<>(List.of(path.split(Pattern.quote(File.pathSeparator), -1)));
        directories.addFirst(".");

        for (String directoryName : directories) {
            File directory = new File(directoryName.isEmpty() ? "." : directoryName);
            File[] files = directory.listFiles();
            if (files == null) {
                continue;
            }

            for (File file : files) {
                if (file.getName().equalsIgnoreCase(isWindows ? name + ".exe" : name)
                        && file.isFile()
                        && file.canExecute()) {
                    return file;
                }
            }
        }

        throw new FileNotFoundException("Could not find " + name + " in PATH");
    }

    final Executor executor;

    final FFMpegLogger logger;

    /** Path to the binary (e.g. /usr/bin/ffmpeg) */
    final File path;

    /** Function to run FFmpeg. We define it like this so we can swap it out (during testing) */
    final FFMpegProcessFactory runFunc;

    /** Version string */
    String version = null;

    protected FFcommon(Executor executor, FFMpegLogger logger, File path, FFMpegProcessFactory runFunction) {
        this.path = Objects.requireNonNull(path);
        this.executor = Objects.requireNonNull(executor);
        if (!path.exists()) {
            throw new IllegalArgumentException("Binary path does not exist");
        }
        this.logger = Objects.requireNonNull(logger);
        this.runFunc = Objects.requireNonNull(runFunction);
    }

    protected void waitAndthrowOnError(CompletableFuture<?> p) throws IOException {
        try {
            p.get();
        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        } catch (ExecutionException e) {
            var cause = e.getCause();
            if (cause == null) {
                throw new IOException(e);
            }
            if (cause instanceof IOException ioe) {
                throw ioe;
            }
            throw new IOException(cause);
        }
    }

    protected void throwOnError(FFMpegProcess p) throws IOException {
        try {
            // TODO In java 8 use waitFor(long timeout, TimeUnit unit)
            if (!p.await(1000)) {
                throw new IOException(getAbsolutePath() + " pid " + p.pid() + " didnt exit in time");
            }
            int code = p.exitCode().orElse(-1);
            if (code != 0) {
                throw new IOException(getAbsolutePath() + " pid " + p.pid() + " exited with code " + code);
            }

        } catch (InterruptedException e) {
            throw new InterruptedIOException();
        }
    }

    /**
     * Returns the version string for this binary.
     *
     * @return the version string.
     * @throws IOException If there is an error capturing output from the binary.
     */
    public synchronized String version() throws IOException {
        if (this.version == null) {
            try (FFMpegProcess p = runFunc.createProcess(executor, logger, List.of(getAbsolutePath(), "-version"));
                    BufferedReader r = p.stdoutReader()) {
                var errorReader = pipe1(p.stderr(), OutputStream.nullOutputStream());
                this.version = r.readLine();
                r.transferTo(Writer.nullWriter()); // Throw away rest of the output
                waitAndthrowOnError(errorReader);
                throwOnError(p);
            }
        }
        return version;
    }

    protected void pipe3(
            InputStream in1, OutputStream out1, InputStream in2, OutputStream out2, InputStream in3, OutputStream out3)
            throws IOException {
        try (in1;
                in2) {
            var future1 = pipe1(in1, out1);
            var future2 = pipe1(in2, out2);
            var future3 = pipe1(in3, out3);
            List<CompletableFuture<?>> futures = new ArrayList<>(List.of(future1, future2, future3));
            while (!futures.isEmpty()) {
                waitAndthrowOnError(CompletableFuture.anyOf(futures.toArray(CompletableFuture[]::new)));
                futures.removeIf(CompletableFuture::isDone);
            }
        }
    }

    protected CompletableFuture<Void> pipe1(InputStream in, OutputStream out) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        Thread t = Thread.currentThread();
        executor.execute(() -> {
            if (t == Thread.currentThread()) {
                throw new IllegalStateException("Bad executor");
            }
            try (in;
                    out) {
                in.transferTo(out);
            } catch (IOException e) {
                future.completeExceptionally(e);
                // DC
            }
            future.complete(null);
        });
        return future;
    }

    protected String getAbsolutePath() {
        return path.getAbsolutePath();
    }

    public File getPath() {
        return path;
    }

    /**
     * Returns the full path to the binary with arguments appended.
     *
     * @param args The arguments to pass to the binary.
     * @return The full path and arguments to execute the binary.
     * @throws IOException If there is an error capturing output from the binary
     */
    protected List<String> path(List<String> args) throws IOException {
        List<String> command = new ArrayList<>(args.size() + 1);
        command.add(getAbsolutePath());
        command.addAll(args);
        return List.copyOf(command);
    }
}
