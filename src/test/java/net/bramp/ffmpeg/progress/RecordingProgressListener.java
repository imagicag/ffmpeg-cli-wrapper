package net.bramp.ffmpeg.progress;

import java.util.ArrayList;
import java.util.List;

/** Test class to keep a record of all progresses. */
public class RecordingProgressListener implements ProgressListener {
  public final List<Progress> progesses = new ArrayList<>();

  @Override
  public void progress(Progress p) {
    progesses.add(p);
  }

  public void reset() {
    progesses.clear();
  }
}
