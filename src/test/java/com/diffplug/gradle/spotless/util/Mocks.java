package com.diffplug.gradle.spotless.util;

import org.gradle.api.Action;
import org.gradle.api.tasks.incremental.IncrementalTaskInputs;
import org.gradle.api.tasks.incremental.InputFileDetails;

import java.io.File;

public final class Mocks {
    private Mocks() {}

    public static IncrementalTaskInputs mockIncrementalTaskInputs(Iterable<File> target) {
      return new IncrementalTaskInputs() {
          @Override
          public boolean isIncremental() {
              return false;
          }

          @Override
          public void outOfDate(Action<? super InputFileDetails> action) {
              for (File file : target) {
                  action.execute(mockInputFileDetails(file));
              }
          }

          @Override
          public void removed(Action<? super InputFileDetails> action) {
              // do nothing
          }
      };
    }

    private static InputFileDetails mockInputFileDetails(File file) {
      return new InputFileDetails() {
          @Override
          public boolean isAdded() {
              return false;
          }

          @Override
          public boolean isModified() {
              return false;
          }

          @Override
          public boolean isRemoved() {
              return false;
          }

          @Override
          public File getFile() {
              return file;
          }
      };
    }
}
