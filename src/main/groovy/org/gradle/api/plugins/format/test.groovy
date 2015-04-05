package org.gradle.api.plugins.format


def source = new File("/home/youri/dev/gradle-format-plugin/src/test/resources/JavaCodeUnformatted.java").text
def res = new ImportSorterAdapter(["java", "org", "\\#org"]).sortImports(source)


println res