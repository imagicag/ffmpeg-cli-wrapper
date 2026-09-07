package ch.imagic.ffmpeg;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple function that creates a Process with the arguments, and returns a BufferedReader reading
 * stdout
 *
 */
public class RunProcessFunction implements ProcessFunction {

    static final Logger LOG = LoggerFactory.getLogger(RunProcessFunction.class);

    File workingDirectory;

    @Override
    public Process run(List<String> args) throws IOException {
        Objects.requireNonNull(args, "Arguments must not be null");
        if (args.isEmpty()) {
            throw new IllegalArgumentException("No arguments specified");
        }

        if (LOG.isInfoEnabled()) {
            LOG.info("{}", String.join(" ", args));
        }

        ProcessBuilder builder = new ProcessBuilder(args);
        if (workingDirectory != null) {
            builder.directory(workingDirectory);
        }
        builder.redirectErrorStream(true);
        return builder.start();
    }

    public RunProcessFunction setWorkingDirectory(String workingDirectory) {
        this.workingDirectory = new File(workingDirectory);
        return this;
    }

    public RunProcessFunction setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
        return this;
    }
}
