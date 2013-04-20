package ru.finam.GradlePlugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Exec

/**
 * User: aksenov
 * Date: 20.04.13
 * Time: 16:58
 */
class RFileGeneratorPluginExtension {
    String gen = 'gen'
    String res = 'res'
    String manifest = 'AndroidManifest.xml'
    String androidPackage = 'com.google.android'
    String androidName = 'android'
    String androidVersion = '4.1.1.4'

}

class RFileGeneratorPlugin implements Plugin<Project> {
    @Override
    void apply(Project t) {
        def ext = t.extensions.create("RFileGenerator", RFileGeneratorPluginExtension)
        if (System.env.ANDROID_HOME == null) {
            throw new IllegalStateException("System variable ANDROID_HOME not found")
        }
        t.dependencies.provided = "$ext.androidPackage:$ext.androidName:$ext.androidVersion"
        t.sourceSets.main.java.srcDir = ext.gen
        t.task('generateRFile', type: Exec) << {
            executable = "$System.env.ANDROID_HOME/platform-tools/aapt"
            args = [
                    'package', '-v', '-f', '-m',
                    "-S", file(ext.res),
                    "-J", file(ext.gen),
                    "-M", file(ext.manifest),
                    "-I", project.configurations.provided.find { it.name == "$ext.androidName-${ext.androidVersion}.jar" }
            ]
            standardOutput = new ByteArrayOutputStream()

            ext.output = {
                println standardOutput.toString()
            }
        }
    }
}
