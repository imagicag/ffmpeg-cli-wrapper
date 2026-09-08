package ch.imagic.ffmpeg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FFcommonTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void getSystemBinaryReturnsFirstCaseInsensitiveMatch() throws Exception {
        File firstDirectory = temporaryFolder.newFolder("first");
        File secondDirectory = temporaryFolder.newFolder("second");
        File first = executableFile(firstDirectory, "FFmpeg");
        executableFile(secondDirectory, "ffmpeg");

        File result = FFcommon.getSystemBinary(
                "ffmpeg", firstDirectory.getPath() + File.pathSeparator + secondDirectory.getPath(), false);

        assertEquals(first, result);
    }

    @Test
    public void getSystemBinaryFindsExeFile() throws Exception {
        File directory = temporaryFolder.newFolder("bin");
        File executable = executableFile(directory, "FFPROBE.EXE");

        assertEquals(executable, FFcommon.getSystemBinary("ffprobe", directory.getPath(), true));
    }

    @Test(expected = FileNotFoundException.class)
    public void getSystemBinaryThrowsWhenNotFound() throws Exception {
        FFcommon.getSystemBinary("missing", temporaryFolder.getRoot().getPath(), false);
    }

    private File executableFile(File directory, String name) throws Exception {
        File file = new File(directory, name);
        assertTrue(file.createNewFile());
        assertTrue(file.setExecutable(true));
        return file;
    }
}
