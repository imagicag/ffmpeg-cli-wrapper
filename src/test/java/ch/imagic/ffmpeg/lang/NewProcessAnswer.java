package ch.imagic.ffmpeg.lang;

import ch.imagic.ffmpeg.Helper;
import ch.imagic.ffmpeg.process.FFMpegProcess;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class NewProcessAnswer implements Answer<FFMpegProcess> {
    final String resource;

    public NewProcessAnswer(String resource) {
        this.resource = resource;
    }

    @Override
    public FFMpegProcess answer(InvocationOnMock invocationOnMock) throws Throwable {
        return new MockProcess(Helper.loadResource(resource));
    }
}
