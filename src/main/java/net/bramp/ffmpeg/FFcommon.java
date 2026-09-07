package net.bramp.ffmpeg;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import net.bramp.ffmpeg.io.ProcessUtils;

/** Private class to contain common methods for both FFmpeg and FFprobe. */
abstract class FFcommon {

  /** Path to the binary (e.g. /usr/bin/ffmpeg) */
  final String path;

  /** Function to run FFmpeg. We define it like this so we can swap it out (during testing) */
  final ProcessFunction runFunc;

  /** Version string */
  String version = null;

  public FFcommon(String path) {
    this(path, new RunProcessFunction());
  }

  protected FFcommon(String path, ProcessFunction runFunction) {
    if (path == null || path.isEmpty()) {
      throw new IllegalArgumentException();
    }
    this.runFunc = Objects.requireNonNull(runFunction);
    this.path = path;
  }

  protected BufferedReader wrapInReader(Process p) {
    return new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
  }

  protected void throwOnError(Process p) throws IOException {
    try {
      // TODO In java 8 use waitFor(long timeout, TimeUnit unit)
      if (ProcessUtils.waitForWithTimeout(p, 1, TimeUnit.SECONDS) != 0) {
        // TODO Parse the error
        throw new IOException(path + " returned non-zero exit status. Check stdout.");
      }
    } catch (TimeoutException e) {
      throw new IOException("Timed out waiting for " + path + " to finish.");
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
      Process p = runFunc.run(List.of(path, "-version"));
      try {
        BufferedReader r = wrapInReader(p);
        this.version = r.readLine();
        r.transferTo(Writer.nullWriter()); // Throw away rest of the output

        throwOnError(p);
      } finally {
        p.destroy();
      }
    }
    return version;
  }

  public String getPath() {
    return path;
  }

  /**
   * Returns the full path to the binary with arguments appended.
   *
   * @param args The arguments to pass to the binary.
   * @return The full path and arguments to execute the binary.
   * @throws IOException If there is an error capturing output from the binary
   */
  public List<String> path(List<String> args) throws IOException {
    List<String> command = new ArrayList<>(args.size() + 1);
    command.add(path);
    command.addAll(args);
    return List.copyOf(command);
  }

  /**
   * Runs the binary (ffmpeg) with the supplied args. Blocking until finished.
   *
   * @param args The arguments to pass to the binary.
   * @throws IOException If there is a problem executing the binary.
   */
  public void run(List<String> args) throws IOException {
    Objects.requireNonNull(args);

    Process p = runFunc.run(path(args));
    assert (p != null);

    try {
      // TODO Move the copy onto a thread, so that FFmpegProgressListener can be on this thread.

      // Now block reading ffmpeg's stdout. We are effectively throwing away the output.
      Writer output = new OutputStreamWriter(System.out, StandardCharsets.UTF_8);
      wrapInReader(p).transferTo(output); // TODO Should I be outputting to stdout?
      output.flush();

      throwOnError(p);

    } finally {
      p.destroy();
    }
  }
}
