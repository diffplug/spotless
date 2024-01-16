package com.diffplug.spotless.maven.go;

import com.diffplug.spotless.maven.FormatterFactory;
import org.apache.maven.project.MavenProject;

import java.util.Collections;
import java.util.Set;

public class Go extends FormatterFactory {
    @Override
    public Set<String> defaultIncludes(MavenProject project) {
        return Collections.emptySet();
    }

    @Override
    public String licenseHeaderDelimiter() {
        return null;
    }

    public void addGofmt(Gofmt gofmt) {
        addStepFactory(gofmt);
    }
}
