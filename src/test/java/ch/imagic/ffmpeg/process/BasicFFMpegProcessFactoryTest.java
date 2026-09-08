package ch.imagic.ffmpeg.process;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.imagic.ffmpeg.FFMpegLogger;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class BasicFFMpegProcessFactoryTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @AfterEach
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void reportsLaunchFailureWithCommandAndCause() {
        List<String> command = List.of("/path/that/does/not/exist", "argument");

        IOException failure = assertThrows(
                IOException.class,
                () -> new BasicFFMpegProcessFactory().createProcess(executor, FFMpegLogger.noop(), command));

        assertTrue(failure.getMessage().contains(String.join(", ", command)));
        assertTrue(failure.getCause() instanceof IOException);
    }

    @Test
    public void callsCommandCallbackWithStartedPidAndArguments() throws Exception {
        List<String> command = List.of("/bin/sh", "-c", "exit 0");
        AtomicLong callbackPid = new AtomicLong(-1);
        AtomicReference<List<String>> callbackCommand = new AtomicReference<>();
        FFMpegLogger logger = new FFMpegLogger() {
            @Override
            public boolean wantsCommandLine() {
                return true;
            }

            @Override
            public void onCommandLine(long pid, List<String> commandLine) {
                callbackPid.set(pid);
                callbackCommand.set(commandLine);
            }
        };

        try (FFMpegProcess process = new BasicFFMpegProcessFactory().createProcess(executor, logger, command)) {
            assertEquals(process.pid(), callbackPid.get());
            assertEquals(command, callbackCommand.get());
            assertTrue(process.await(1_000));
        }
    }

    @Test
    public void doesNotCallCommandCallbackWhenLoggerDoesNotRequestIt() throws Exception {
        AtomicReference<List<String>> callbackCommand = new AtomicReference<>();
        FFMpegLogger logger = new FFMpegLogger() {
            @Override
            public void onCommandLine(long pid, List<String> commandLine) {
                callbackCommand.set(commandLine);
            }
        };

        try (FFMpegProcess process =
                new BasicFFMpegProcessFactory().createProcess(executor, logger, List.of("/bin/sh", "-c", "exit 0"))) {
            assertTrue(process.await(1_000));
        }

        assertNull(callbackCommand.get());
    }
}
