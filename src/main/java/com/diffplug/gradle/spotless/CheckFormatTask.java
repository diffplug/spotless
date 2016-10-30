package com.diffplug.gradle.spotless;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CheckFormatTask extends DefaultTask {
  // set by SpotlessExtension, but possibly overridden by FormatExtension
  @Input
  public Charset encoding = StandardCharsets.UTF_8;
  @Input
  public LineEnding.Policy lineEndingPolicy = LineEnding.UNIX_POLICY;

  // set by FormatExtension
  @Input
  public boolean paddedCell = false;
  @InputFiles
  @SkipWhenEmpty
  public Iterable<File> target;
  @Input
  @SkipWhenEmpty
  public List<FormatterStep> steps = new ArrayList<>();

  @TaskAction
  public void check(IncrementalTaskInputs inputs) throws IOException {
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

    formatCheck(formatter, inputs);
  }

  /** Checks the format. */
  private void formatCheck(Formatter formatter, IncrementalTaskInputs inputs) throws IOException {
    List<File> problemFiles = new ArrayList<>();

    inputs.outOfDate(input -> {
      File file = input.getFile();
      getLogger().debug("Checking format on " + file);
      // keep track of the problem toFormat
      try {
        if (!formatter.isClean(file)) {
          problemFiles.add(file);
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    });

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
