# FFmpeg Java CLI Wrapper
Based on ffmpeg-cli-wrapper version 0.8.0 by Andrew Brampton (c) 2013-2022,
see https://github.com/bramp/ffmpeg-cli-wrapper for more details.

This project focuses on providing a ffmpeg-cli-wrapper for java with minimal dependencies and improved
usability in edge cases that were difficult to achieve with the original version.
Minimum required java version is java 17.

## Notable added features
- Canceling of ongoing calls to ffmpeg.
  - Supports polling with timeout 
  - Asynchronous canceling

- Versatile per-call configurable logging
  - 1 line of code integration with most logging frameworks.
  - Log stdout/stderr or just start/stop of ffmpeg separately with fine grained control. 

- FFProbe supports parsing using custom JSON schema classes.
  - You can also just get the raw JSON as String

- Bring your own thread pool!
  - Supports any implementation of "Executor"

- Trusts your binary!
  - By default, no checks are performed that the ffmpeg binary is indeed ffmpeg and not something else, saving a lot of time.
  - Same for ffprobe
  - Much faster, less log lines, less child processes!

## Usage

Maven:
```xml
<dependency>
  <groupId>ch.imagic</groupId>
  <artifactId>ffmpeg-java</artifactId>
  <version>0.1.0</version>
</dependency>
```
### Video Encoding

Code:
```java
import ch.imagic.ffmpeg.FFMpegJob;
import ch.imagic.ffmpeg.FFmpeg;
import ch.imagic.ffmpeg.FFprobe;
import ch.imagic.ffmpeg.builder.FFmpegBuilder;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class EncodingExample {
  public static void main(String[] args) throws IOException, InterruptedException {
    FFmpeg ffmpeg = new FFmpeg(new File("/path/to/ffmpeg"),
        ch.imagic.ffmpeg.process.FFMpegProcessFactory.defaultFactory());
    FFprobe ffprobe = new FFprobe(new File("/path/to/ffprobe"));
    FFmpegProbeResult input = ffprobe.probe(new File("input.mp4")).get();

    FFmpegBuilder builder = new FFmpegBuilder()
        .setInput(input)           // Target-size encoding needs the probed duration
        .overrideOutputFiles(true) // Override the output if it exists
        .addOutput("output.mp4")  // Filename for the destination
          .setFormat("mp4")       // Format is inferred from filename, or can be set
          .setTargetSize(250_000)  // Aim for a 250 KB file
          .disableSubtitle()       // No subtitles
          .setAudioChannels(1)         // Mono audio
          .setAudioCodec("aac")        // Using the AAC codec
          .setAudioSampleRate(48_000)  // At 48 kHz
          .setAudioBitRate(32_768)      // At 32 kbit/s
          .setVideoCodec("libx264")     // Video using x264
          .setVideoFrameRate(24, 1)     // At 24 frames per second
          .setVideoResolution(640, 480) // At 640x480 resolution
          .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
          .done();

    FFMpegJob<Void> job = ffmpeg.run(builder);
    if (!job.await(10, TimeUnit.SECONDS)) {
      job.kill();
      throw new IOException("ffmpeg took too long to transcode");
    }

    job.get(); // Throws IOException if ffmpeg failed
  }
}
```

### Get Media Information

Code:
```java
import ch.imagic.ffmpeg.FFprobe;
import ch.imagic.ffmpeg.probe.FFmpegFormat;
import ch.imagic.ffmpeg.probe.FFmpegProbeResult;
import ch.imagic.ffmpeg.probe.FFmpegStream;
import java.io.File;

public class ProbeExample {
  public static void main(String[] args) throws Exception {
    FFprobe ffprobe = new FFprobe(new File("/path/to/ffprobe"));
    FFmpegProbeResult probeResult = ffprobe.probe(new File("input.mp4")).get();

    FFmpegFormat format = probeResult.getFormat();
    System.out.format("%nFile: '%s' ; Format: '%s' ; Duration: %.3fs",
        format.getFilename(),
        format.getFormatLongName(),
        format.getDuration());

    FFmpegStream stream = probeResult.getStreams().get(0);
    System.out.format("%nCodec: '%s' ; Width: %dpx ; Height: %dpx%n",
        stream.getCodecLongName(),
        stream.getWidth(),
        stream.getHeight());
  }
}
```


Licence (Simplified BSD License)
--------------------------------
```
Copyright (c) 2026, Imagic Bildverarbeitung AG, Sägereistrasse 29, 8152 Glattbrugg, Switzerland
Copyright (c) 2016-2022, Andrew Brampton
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```
