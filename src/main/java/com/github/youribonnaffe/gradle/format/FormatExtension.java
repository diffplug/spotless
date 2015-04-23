package com.github.youribonnaffe.gradle.format;

import java.io.File;
import java.util.Collection;
import java.util.List;

import org.gradle.api.tasks.SourceSet;

public class FormatExtension {
	/** Source sets to perform search on, will default to all sourceSets in the project. */
	Collection<SourceSet> sourceSets;

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

	/** Line endings (if any). */
	LineEnding lineEndings = LineEnding.PLATFORM_NATIVE;

	public Collection<SourceSet> getSourceSets() {
		return sourceSets;
	}

	public void setSourceSets(Collection<SourceSet> sourceSets) {
		this.sourceSets = sourceSets;
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

	public LineEnding getLineEndings() {
		return lineEndings;
	}

	public void setLineEndings(LineEnding lineEndings) {
		this.lineEndings = lineEndings;
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
}
