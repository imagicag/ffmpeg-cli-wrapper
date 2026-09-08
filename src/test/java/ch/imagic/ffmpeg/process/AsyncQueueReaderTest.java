package ch.imagic.ffmpeg.process;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Test;

public class AsyncQueueReaderTest {
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    @Test
    public void preservesAllDataBeforeEof() throws IOException {
        byte[] expected = new byte[200_000];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = (byte) i;
        }

        try (InputStream reader = new AsyncQueueReader(executor, new ByteArrayInputStream(expected), ignored -> {})) {
            assertArrayEquals(expected, reader.readAllBytes());
            assertEquals(-1, reader.read());
        }
    }

    @Test
    public void loggerFailureDoesNotInterruptStream() throws IOException {
        byte[] expected = new byte[100_000];
        Arrays.fill(expected, (byte) 42);

        try (InputStream reader = new AsyncQueueReader(executor, new ByteArrayInputStream(expected), ignored -> {
            throw new IllegalStateException("logger failed");
        })) {
            assertArrayEquals(expected, reader.readAllBytes());
        }
    }

    @Test
    public void closeClosesOriginalStream() throws IOException {
        CloseTrackingInputStream input = new CloseTrackingInputStream();
        AsyncQueueReader reader = new AsyncQueueReader(executor, input, ignored -> {});

        reader.close();

        assertTrue(input.closed);
    }

    @Test
    public void awaitsWorkerTermination() throws IOException, InterruptedException {
        AsyncQueueReader reader =
                new AsyncQueueReader(executor, new ByteArrayInputStream(new byte[] {1}), ignored -> {});

        assertArrayEquals(new byte[] {1}, reader.readAllBytes());

        assertTrue(reader.awaitAsyncTermination(1, TimeUnit.SECONDS));
        reader.close();
    }

    @Test
    public void awaitsWorkerWhileConsumerIsRunning() throws IOException, InterruptedException {
        CountDownLatch consumerStarted = new CountDownLatch(1);
        AsyncQueueReader reader = new AsyncQueueReader(executor, new ByteArrayInputStream(new byte[] {1}), ignored -> {
            consumerStarted.countDown();
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assertTrue(consumerStarted.await(1, TimeUnit.SECONDS));
        assertFalse(reader.awaitAsyncTermination(200, TimeUnit.MILLISECONDS));

        long start = System.nanoTime();
        assertTrue(reader.awaitAsyncTermination(5, TimeUnit.SECONDS));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);

        assertTrue("Expected to wait at least one second, waited " + elapsedMillis + " ms", elapsedMillis >= 1_000);
        reader.close();
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed;

        private CloseTrackingInputStream() {
            super(new byte[0]);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
