package ch.imagic.ffmpeg.process;

import ch.imagic.ffmpeg.FFMpegLogger;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Simple function that creates a Process with the arguments, and returns a BufferedReader reading
 * stdout
 *
 */
class BasicFFMpegProcessFactory implements FFMpegProcessFactory {

    @Override
    public FFMpegProcess createProcess(Executor executor, FFMpegLogger logger, List<String> args) throws IOException {
        Objects.requireNonNull(args, "Arguments must not be null");
        if (args.isEmpty()) {
            throw new IllegalArgumentException("No arguments specified");
        }

        ProcessBuilder builder = new ProcessBuilder(args);
        Process proc;
        try {
            proc = builder.start();
        } catch (Exception t) {
            throw new IOException("Failed to invoke process '" + String.join(", ", args) + "'", t);
        }

        try {
            if (logger.wantsCommandLine()) {
                logger.onCommandLine(proc.pid(), args);
            }
        } catch (Throwable t) {
            // IGNORE EXCEPTIONS FROM SILLY LOGGERS
        }

        return new BasicFFMpegProcess(executor, logger, proc);
    }
}
