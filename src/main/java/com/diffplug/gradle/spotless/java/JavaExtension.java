package com.diffplug.gradle.spotless.java;

import java.io.File;
import java.util.List;

import org.testng.collections.Lists;

import com.diffplug.gradle.spotless.FormatTask;
import com.diffplug.gradle.spotless.FormatterStep;
import com.diffplug.gradle.spotless.SpotlessExtension;

public class JavaExtension extends SpotlessExtension {
	/** Header string for the file. */
	String licenseHeader;

	/** Header file to be appended to the file. */
	Object licenseHeaderFile;

	/** The imports ordering. */
	List<String> importsOrder;

	/** The imports ordering file. */
	Object importsOrderFile;

	/** Eclipse format file. */
	Object eclipseFormatFile;

	public JavaExtension() {
		super("java");
	}

	public List<String> getImportsOrder() {
		return importsOrder;
	}

	public void setImportsOrder(List<String> importsOrder) {
		this.importsOrder = importsOrder;
	}

	public Object getImportsOrderFile() {
		return importsOrderFile;
	}

	public void setImportsOrderFile(Object importsOrderFile) {
		this.importsOrderFile = importsOrderFile;
	}

	public Object getEclipseFormatFile() {
		return eclipseFormatFile;
	}

	public void setEclipseFormatFile(Object eclipseFormatFile) {
		this.eclipseFormatFile = eclipseFormatFile;
	}

	public String getLicenseHeader() {
		return licenseHeader;
	}

	public void setLicenseHeader(String licenseHeader) {
		this.licenseHeader = licenseHeader;
	}

	public Object getLicenseHeaderFile() {
		return licenseHeaderFile;
	}

	public void setLicenseHeaderFile(Object licenseHeaderFile) {
		this.licenseHeaderFile = licenseHeaderFile;
	}

	@Override
	protected void setupTask(FormatTask task) throws Exception {
		super.setupTask(task);

		// create the Java steps
		List<FormatterStep> javaSteps = Lists.newArrayList();
		LicenseHeaderStep.load(licenseHeader, resolve(task, licenseHeaderFile)).ifPresent(javaSteps::add);
		ImportSorterStep.load(importsOrder, resolve(task, importsOrderFile)).ifPresent(javaSteps::add);
		if (eclipseFormatFile != null) {
			javaSteps.add(EclipseFormatterStep.load(resolve(task, eclipseFormatFile)));
		}

		// prefix them to the previous steps
		task.steps.addAll(0, javaSteps);
	}

	/** Resolves the given file object using the current project. */
	private static File resolve(FormatTask task, Object file) {
		return file == null ? null : task.getProject().file(file);
	}
}
