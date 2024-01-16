package com.diffplug.spotless.maven.go;

import com.diffplug.spotless.maven.MavenIntegrationHarness;
import org.junit.jupiter.api.Test;

@com.diffplug.spotless.tag.GofmtTest
public class GofmtTest extends MavenIntegrationHarness {
    @Test
    void testGofmt() throws Exception {
        writePomWithGoSteps("<gofmt><version>go1.21.5</version></gofmt>");

        setFile("src/main/go/example.go").toResource("go/gofmt/go.dirty");
        mavenRunner().withArguments("spotless:apply").runNoError();
        assertFile("src/main/go/example.go").sameAsResource("go/gofmt/go.clean");
    }
}
