package ru.finam.GradlePlugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.UnknownDomainObjectException
import org.gradle.api.artifacts.ProjectDependency
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
    void apply(Project p) {
        def vars = p.extensions.create("RFileGenerator", RFileGeneratorPluginExtension)
        if (System.env.ANDROID_HOME == null) {
            throw new IllegalStateException("System variable ANDROID_HOME not found")
        }
        p.dependencies {
            provided "$vars.androidPackage:$vars.androidName:$vars.androidVersion"
        }
        p.sourceSets {
            main {
                java {
                    srcDir vars.gen
                }
            }
        }




        p.task('generateRFile', type: Exec) {
            def params = [
                    'package', '-v', '-f', '-m',
                    '--auto-add-overlay',
                    "-J", p.file(vars.gen),
                    "-M", p.file(vars.manifest),
                    "-I", p.configurations.provided.find { it.name == "$vars.androidName-${vars.androidVersion}.jar" }
            ]

            getResDirs(p).each {
                params.add("-S")
                params.add(it)

                println("add resource dir $it")
            }

            executable = "$System.env.ANDROID_HOME/platform-tools/aapt"

            args = params

            standardOutput = new ByteArrayOutputStream()

            ext.output = {
                println standardOutput.toString()
            }
        }

        p.compileJava.dependsOn p.generateRFile
    }

    Set<File> getResDirs(Project p) {
        def files = new HashSet<File>()
        fillResources(files, p)
        return files
    }


    void fillResources(Set<File> resources, Project p) {
        println "fill dependency $p"
        def resourceFile = getProjectResources(p)
        if (resourceFile != null) {
            resources.add(resourceFile)
        }
        p.configurations.compile.getDependencies().each { dep ->
            println "check dependency $dep"
            if (dep instanceof ProjectDependency) {
                fillResources(resources, dep.getDependencyProject());
            }
        }

    }

    File getProjectResources(Project project) {
        try {
            def ext = project.getExtensions().getByType(ru.finam.GradlePlugins.RFileGeneratorPluginExtension.class)
            return project.file(ext.res)
        } catch (UnknownDomainObjectException e) {
            it.getDependencyProject()
        }
    }
}
