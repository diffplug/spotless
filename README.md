# Gradle Format plugin #

The Gradle Format plugin allows you to format your Java sources. It operates in place and can be used for instance
before pushing changes to enforce a coding style. The formatting can be customized using a properties or XML file with
the keys defined in [DefaultCodeFormatterConstants](http://help.eclipse.org/indigo/index.jsp?topic=%2Forg.eclipse.jdt.doc.isv%2Freference%2Fapi%2Forg%2Feclipse%2Fjdt%2Fcore%2Fformatter%2FDefaultCodeFormatterConstants.html)
 from Eclipse. See src/test/resources/ for examples.
 Eventually you should be able to export Eclipse settings to this file.

## Usage

To use the Format plugin, include in your build script:

    apply plugin: 'java'
    apply plugin: 'format'

Note that the Java plugin must be applied too.

To run it:

    gradle format

The plugin JAR needs to be accessible in the classpath of your build script. It is directly available on
[BinTray](https://bintray.com/youribonnaffe/maven/gradle-format-plugin/).
Alternatively, you can download it from GitHub and deploy it to your local repository. The following code snippet shows an
example on how to retrieve it from Bintray:

    buildscript {
        repositories {
            maven { url { 'http://dl.bintray.com/youribonnaffe/maven' } }
        }

        dependencies {
            classpath 'org.gradle.api.plugins:gradle-format-plugin:1.1'
        }
    }

## Tasks

The Format plugin defines the following tasks:

* `format`: Format Java source code

## Properties

The Format task defines the following properties:

* `configurationFile`: The formatter configuration (File)
* `files`: The files to format (FileCollection), defaults are Java sources from main and test SourceSets

### Example

    format {
        configurationFile = file('myformatsettings.properties')
        files = sourceSets.main.java
    }
## Acknowledgements

Using [Hibernate Tools](http://hibernate.org/tools/) JavaFormatter

Readme format from https://github.com/bmuschko/gradle-gae-plugin
