package com.diffplug.gradle.spotless;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ApplyFormatTask extends DefaultTask {
  // set by SpotlessExtension, but possibly overridden by FormatExtension
  public Charset encoding = StandardCharsets.UTF_8;
  public LineEnding.Policy lineEndingPolicy = LineEnding.UNIX_POLICY;

  // set by FormatExtension
  public boolean paddedCell = false;
  public Iterable<File> target;
  public List<FormatterStep> steps = new ArrayList<>();

  @TaskAction
  public void apply() throws IOException {
    if (target == null) {
      throw new GradleException("You must specify 'Iterable<File> toFormat'");
    }
    // combine them into the master formatter
    Formatter formatter = Formatter.builder()
        .lineEndingPolicy(lineEndingPolicy)
        .encoding(encoding)
        .projectDirectory(getProject().getProjectDir().toPath())
        .steps(steps)
        .build();

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
