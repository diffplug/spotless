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
	File licenseHeaderFile;

	/** The imports ordering. */
	List<String> importsOrder;

	/** The imports ordering file. */
	File importsOrderFile;

	/** Eclipse format file. */
	File eclipseFormatFile;

	public JavaExtension() {
		super("java");
	}

	public List<String> getImportsOrder() {
		return importsOrder;
	}

	public void setImportsOrder(List<String> importsOrder) {
		this.importsOrder = importsOrder;
	}

	public File getImportsOrderFile() {
		return importsOrderFile;
	}

	public void setImportsOrderFile(File importsOrderFile) {
		this.importsOrderFile = importsOrderFile;
	}

	public File getEclipseFormatFile() {
		return eclipseFormatFile;
	}

	public void setEclipseFormatFile(File eclipseFormatFile) {
		this.eclipseFormatFile = eclipseFormatFile;
	}

	public String getLicenseHeader() {
		return licenseHeader;
	}

	public void setLicenseHeader(String licenseHeader) {
		this.licenseHeader = licenseHeader;
	}

	public File getLicenseHeaderFile() {
		return licenseHeaderFile;
	}

	public void setLicenseHeaderFile(File licenseHeaderFile) {
		this.licenseHeaderFile = licenseHeaderFile;
	}

	@Override
	protected void setupTask(FormatTask task) throws Exception {
		super.setupTask(task);

		// create the Java steps
		List<FormatterStep> javaSteps = Lists.newArrayList();
		LicenseHeaderStep.load(licenseHeader, licenseHeaderFile).ifPresent(javaSteps::add);
		ImportSorterStep.load(importsOrder, importsOrderFile).ifPresent(javaSteps::add);
		if (eclipseFormatFile != null) {
			javaSteps.add(EclipseFormatterStep.load(eclipseFormatFile));
		}

		// prefix them to the previous steps
		task.steps.addAll(0, javaSteps);
	}
}
