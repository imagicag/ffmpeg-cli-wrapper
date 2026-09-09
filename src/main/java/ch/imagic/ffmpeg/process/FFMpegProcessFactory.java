package ch.imagic.ffmpeg.process;

import ch.imagic.ffmpeg.FFMpegLogger;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * Runs a process returning a Reader to its stdout
 *
 */
@FunctionalInterface
public interface FFMpegProcessFactory {

    /**
     * This function should create the ffmpeg process with the given args. The first arg is the absolute path to the ffmpeg binary.
     *
     * Unless this function completely ignores the logger then it has several important responsabilities towards it:
     * 1. It must call "onCommandLine" during process creation (as soon as the pid is obtainable)
     * 2. It must ensure that raw stdout is piped to onRawStdout and stderr is piped to onRawStderr.
     * 3. It must ensure that the returned process will invoke "onProcessDeath" when its close() method is invoked.
     * 4. It must ensure that no further messages are sent to onRawStdout and onRawStderr before onProcessDeath is called.
     * 5. It should respect if the logger does not want a specific method to be invoked.
     *
     * The function can assume that the loggers return value to the boolean functions remains constant and does not change during runtime.
     */
    FFMpegProcess createProcess(Executor executor, FFMpegLogger logger, List<String> args) throws IOException;

    static FFMpegProcessFactory defaultFactory() {
        return new BasicFFMpegProcessFactory();
    }
}
