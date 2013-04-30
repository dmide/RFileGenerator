package ru.finam.GradlePlugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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
    Logger logger = Logging.getLogger(Class)
    RFileGeneratorPluginExtension extension

    @Override
    void apply(Project p) {
        def vars = p.extensions.create("RFileGenerator", RFileGeneratorPluginExtension)
        extension = vars;
        def dirs
        if (System.env.ANDROID_HOME == null) {
            throw new IllegalStateException("System variable ANDROID_HOME not found")
        }

        p.gradle.taskGraph.whenReady { taskGraph ->
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
            dirs = getResDirs(p)
            logger.info('Resource dirs {}', dirs)
        }

        p.compileJava {
            doFirst {
                p.exec {
                    def params = [
                            'package', '-v', '-f', '-m',
                            '--auto-add-overlay',
                            "-J", p.file(vars.gen),
                            "-M", p.file(vars.manifest),
                            "-I", p.configurations.provided.find { it.name == "$vars.androidName-${vars.androidVersion}.jar" }
                    ]

                    dirs.each {
                        params.add("-S")
                        params.add(it)
                        logger.debug('add resource dir {}', it)
                    }

                    executable = "$System.env.ANDROID_HOME/platform-tools/aapt"

                    args = params

                    standardOutput = new ByteArrayOutputStream()

                    ext.output = {
                        println standardOutput.toString()
                    }
                }
            }
        }
    }

    Set<File> getResDirs(Project p) {
        def files = new HashSet<File>()
        fillResources(files, p)
        return files
    }


    void fillResources(Set<File> resources, Project p) {
        logger.debug('fill dependency {}', p)
        def resourceFile = getProjectResources(p)
        logger.debug("retrieved res path {}", resourceFile)
        if (resourceFile != null) {
            resources.add(resourceFile)
        }
        p.configurations.compile.getDependencies().each { dep ->
            logger.debug('check dependency {}', dep)
            if (dep instanceof ProjectDependency) {
                fillResources(resources, dep.getDependencyProject());
            }
        }

    }

    static File getProjectResources(Project project) {
        def plugin = project.getPlugins().findPlugin("RFileGenerator")
        if (plugin != null) {
            return project.file(plugin.extension.res)
        }
        return null
    }
}
