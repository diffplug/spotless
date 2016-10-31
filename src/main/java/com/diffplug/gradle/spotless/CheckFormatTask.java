package com.diffplug.gradle.spotless;

import org.gradle.api.GradleException;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckFormatTask extends BaseFormatTask {
  @Override
  public void doTask(Formatter formatter) throws Exception {
    formatCheck(formatter);
  }

  /** Checks the format. */
  private void formatCheck(Formatter formatter) throws IOException {
    List<File> problemFiles = new ArrayList<>();

    for (File file : target) {
      getLogger().debug("Checking format on " + file);
      // keep track of the problem toFormat
      try {
        if (!formatter.isClean(file)) {
          problemFiles.add(file);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }

    if (paddedCell) {
      PaddedCellTaskMisc.check(this, formatter, problemFiles);
    } else {
      if (!problemFiles.isEmpty()) {
        // if we're not in paddedCell mode, we'll check if maybe we should be
        if (PaddedCellTaskMisc.anyMisbehave(formatter, problemFiles)) {
          throw PaddedCellTaskMisc.youShouldTurnOnPaddedCell(this);
        } else {
          throw formatViolationsFor(formatter, problemFiles);
        }
      }
    }
  }

  /** Returns an exception which indicates problem files nicely. */
  GradleException formatViolationsFor(Formatter formatter, List<File> problemFiles) throws IOException {
    return new GradleException(DiffMessageFormatter.messageFor(this, formatter, problemFiles));
  }

  /** Returns the name of this format. */
  String getFormatName() {
    String name = getName();
    if (name.startsWith(SpotlessPlugin.EXTENSION)) {
      String after = name.substring(SpotlessPlugin.EXTENSION.length());
      return after.substring(0, after.length() - SpotlessPlugin.CHECK.length()).toLowerCase(Locale.US);
    }
    return name;
  }
}
