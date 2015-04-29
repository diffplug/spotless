package com.diffplug.gradle.spotless;

import groovy.lang.Closure;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.gradle.api.GradleException;
import org.gradle.api.Project;

import com.diffplug.gradle.spotless.java.JavaExtension;

public class SpotlessExtension {
	final Project project;

	public SpotlessExtension(Project project) {
		this.project = project;
	}

	/** Line endings (if any). */
	LineEnding lineEndings = LineEnding.PLATFORM_NATIVE;

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = lineEndings;
	}

	Map<String, FormatExtension> formats = new LinkedHashMap<>();

	/** Configures the special java-specific extension. */
	public void java(Closure<JavaExtension> closure) {
		JavaExtension java = new JavaExtension(this);
		closure.setDelegate(java);
		closure.call();
	}

	/** Configures a custom extension. */
	public void java(Consumer<JavaExtension> closure) {
		JavaExtension java = new JavaExtension(this);
		closure.accept(java);
	}

	/** Configures a custom extension. */
	public void format(String name, Closure<FormatExtension> closure) {
		FormatExtension extension = new FormatExtension(name, this);
		closure.setDelegate(extension);
		closure.call();
	}

	/** Configures a custom extension. */
	public void format(String name, Consumer<FormatExtension> closure) {
		FormatExtension extension = new FormatExtension(name, this);
		closure.accept(extension);
	}

	/** Called by the FormatExtension constructor. */
	void addFormatExtension(FormatExtension extension) {
		FormatExtension former = formats.put(extension.name, extension);
		if (former != null) {
			throw new GradleException("Multiple spotless extensions with name '" + extension.name + "'");
		}
	}
}
