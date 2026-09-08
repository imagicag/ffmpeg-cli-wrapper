package ch.imagic.ffmpeg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FFcommonTest {

    @TempDir
    Path temporaryFolder;

    @Test
    public void getSystemBinaryReturnsFirstCaseInsensitiveMatch() throws Exception {
        File firstDirectory = temporaryFolder.resolve("first").toFile();
        File secondDirectory = temporaryFolder.resolve("second").toFile();
        assertTrue(firstDirectory.mkdir());
        assertTrue(secondDirectory.mkdir());
        File first = executableFile(firstDirectory, "FFmpeg");
        executableFile(secondDirectory, "ffmpeg");

        File result = FFcommon.getSystemBinary(
                "ffmpeg", firstDirectory.getPath() + File.pathSeparator + secondDirectory.getPath(), false);

        assertEquals(first, result);
    }

    @Test
    public void getSystemBinaryFindsExeFile() throws Exception {
        File directory = temporaryFolder.resolve("bin").toFile();
        assertTrue(directory.mkdir());
        File executable = executableFile(directory, "FFPROBE.EXE");

        assertEquals(executable, FFcommon.getSystemBinary("ffprobe", directory.getPath(), true));
    }

    @Test
    public void getSystemBinaryThrowsWhenNotFound() throws Exception {
        assertThrows(
                FileNotFoundException.class,
                () -> FFcommon.getSystemBinary("missing", temporaryFolder.toString(), false));
    }

    private File executableFile(File directory, String name) throws Exception {
        File file = new File(directory, name);
        assertTrue(file.createNewFile());
        assertTrue(file.setExecutable(true));
        return file;
    }
}
