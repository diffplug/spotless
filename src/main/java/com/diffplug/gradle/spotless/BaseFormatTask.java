package com.diffplug.gradle.spotless;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.SkipWhenEmpty;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseFormatTask extends DefaultTask {
  // set by SpotlessExtension, but possibly overridden by FormatExtension
  @Input
  public Charset encoding = StandardCharsets.UTF_8;
  @Input
  public LineEnding.Policy lineEndingsPolicy = LineEnding.UNIX_POLICY;
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
  public void run() throws Exception {
    if (target == null) {
      throw new GradleException("You must specify 'Iterable<File> toFormat'");
    }
    // combine them into the master formatter
    Formatter formatter = Formatter.builder()
        .lineEndingsPolicy(lineEndingsPolicy)
        .encoding(encoding)
        .projectDirectory(getProject().getProjectDir().toPath())
        .steps(steps)
        .build();

    doTask(formatter);
  }

  public abstract void doTask(Formatter formatter) throws Exception;
}
