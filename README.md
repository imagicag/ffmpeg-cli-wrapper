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
  <artifactId>ffmpeg-cli</artifactId>
  <version>0.1.0</version>
</dependency>
```
### Video Encoding

Code:
```java
FFmpeg ffmpeg = new FFmpeg("/path/to/ffmpeg");
FFprobe ffprobe = new FFprobe("/path/to/ffprobe");

FFmpegBuilder builder = new FFmpegBuilder()

  .setInput("input.mp4")     // Filename, or a FFmpegProbeResult
  .overrideOutputFiles(true) // Override the output if it exists

  .addOutput("output.mp4")   // Filename for the destination
    .setFormat("mp4")        // Format is inferred from filename, or can be set
    .setTargetSize(250_000)  // Aim for a 250KB file

    .disableSubtitle()       // No subtiles

    .setAudioChannels(1)         // Mono audio
    .setAudioCodec("aac")        // using the aac codec
    .setAudioSampleRate(48_000)  // at 48KHz
    .setAudioBitRate(32768)      // at 32 kbit/s

    .setVideoCodec("libx264")     // Video using x264
    .setVideoFrameRate(24, 1)     // at 24 frames per second
    .setVideoResolution(640, 480) // at 640x480 resolution

    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL); // Allow FFmpeg to use experimental specs

FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

// Run a one-pass encode
executor.createJob(builder).run();

// Or run a two-pass encode (which is better quality at the cost of being slower)
executor.createTwoPassJob(builder).run();
```

### Get Media Information

Code:
```java
FFprobe ffprobe = new FFprobe("/path/to/ffprobe");
FFmpegProbeResult probeResult = ffprobe.probe("input.mp4");

FFmpegFormat format = probeResult.getFormat();
System.out.format("%nFile: '%s' ; Format: '%s' ; Duration: %.3fs", 
	format.filename, 
	format.format_long_name,
	format.duration
);

FFmpegStream stream = probeResult.getStreams().get(0);
System.out.format("%nCodec: '%s' ; Width: %dpx ; Height: %dpx",
	stream.codec_long_name,
	stream.width,
	stream.height
);
```

### Get progress while encoding
```java
FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

FFmpegProbeResult in = ffprobe.probe("input.flv");

FFmpegBuilder builder = new FFmpegBuilder()
	.setInput(in) // Or filename
	.addOutput("output.mp4")
	.done();

FFmpegJob job = executor.createJob(builder, new ProgressListener() {

	// Using the FFmpegProbeResult determine the duration of the input
	final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

	@Override
	public void progress(Progress progress) {
		double percentage = progress.out_time_ns / duration_ns;

		// Print out interesting information about the progress
		System.out.println(String.format(
			"[%.0f%%] status:%s frame:%d time:%s ms fps:%.0f speed:%.2fx",
			percentage * 100,
			progress.status,
			progress.frame,
			FFmpegUtils.toTimecode(progress.out_time_ns, TimeUnit.NANOSECONDS),
			progress.fps.doubleValue(),
			progress.speed
		));
	}
});

job.run();
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
