package com.diffplug.gradle.spotless;

import java.io.File;
import java.io.IOException;

public class ApplyFormatTask extends BaseFormatTask {
  @Override
  public void doTask(Formatter formatter) throws Exception {
    formatApply(formatter);
  }

  /** Applies the format. */
  private void formatApply(Formatter formatter) throws IOException {
    for (File file : target) {
      getLogger().debug("Applying format to " + file);
      // keep track of the problem toFormat
      if (paddedCell) {
        PaddedCellTaskMisc.apply(this, formatter, file);
      } else {
        formatter.applyFormat(file);
      }
    };
  }
}
