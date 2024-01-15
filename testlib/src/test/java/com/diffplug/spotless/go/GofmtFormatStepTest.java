package com.diffplug.spotless.go;

import com.diffplug.spotless.ResourceHarness;
import com.diffplug.spotless.StepHarness;
import com.diffplug.spotless.tag.GofmtTest;
import org.junit.jupiter.api.Test;

@GofmtTest
public class GofmtFormatStepTest extends ResourceHarness {
    @Test
    void test() {
        try (StepHarness harness = StepHarness.forStep(GofmtFormatStep.withVersion("go1.21.5").create())) {
                harness.testResource("go/gofmt/go.dirty", "go/gofmt/go.clean")
                    .close();
        }
    }
}
