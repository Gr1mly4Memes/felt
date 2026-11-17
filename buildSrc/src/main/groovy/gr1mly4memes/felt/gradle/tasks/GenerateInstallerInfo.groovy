package gr1mly4memes.felt.gradle.tasks

import groovy.json.JsonOutput
import gr1mly4memes.felt.gradle.Utils
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

class GenerateInstallerInfo extends DefaultTask {

    private String minecraftVersion, fabricLoaderVersion
    private Configuration configuration, fabricExtra

    @Classpath
    Configuration getConfiguration() {
        return configuration
    }

    void setConfiguration(Configuration configuration) {
        this.configuration = configuration
    }

    @Classpath
    Configuration getFabricExtra() {
        return fabricExtra
    }

    void setFabricExtra(Configuration fabricExtra) {
        this.fabricExtra = fabricExtra
    }

    @Input
    String getMinecraftVersion() {
        return minecraftVersion
    }

    void setMinecraftVersion(String minecraftVersion) {
        this.minecraftVersion = minecraftVersion
    }

    @Input
    String getFabricLoaderVersion() {
        return fabricLoaderVersion
    }

    void setFabricLoaderVersion(String fabricLoaderVersion) {
        this.fabricLoaderVersion = fabricLoaderVersion
    }

    private static Map<String, String> configurationArtifacts(Configuration conf) {
        def artifacts = new TreeMap<String, String>()
        conf.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            if (artifact.id.componentIdentifier instanceof ModuleComponentIdentifier && artifact.extension == 'jar') {
                def module = artifact.moduleVersion.id
                def coordinate = "${module.group}:${module.name}:${module.version}"
                if (artifact.classifier) {
                    coordinate += ":${artifact.classifier}"
                }
                artifacts.put(coordinate.toString(), Utils.sha1(artifact.file))
            }
        }
        return artifacts
    }

    @TaskAction
    void run() {
        def fabricLoaderUrl = "https://maven.fabricmc.net/net/fabricmc/fabric-loader/$fabricLoaderVersion/fabric-loader-${fabricLoaderVersion}.jar"
        def tmpFabric = Files.createTempFile("fabric", "jar")
        Utils.download(fabricLoaderUrl, tmpFabric.toFile())
        def libraries = configurationArtifacts(this.configuration)
        def fabricLibraries = configurationArtifacts(this.fabricExtra)
        libraries.each { coordinate, hash ->
            if (fabricLibraries.get(coordinate) != hash) {
                throw new IllegalStateException("Fabric installer overrides shared library ${coordinate}")
            }
            fabricLibraries.remove(coordinate)
        }
        def output = [
                installer  : [
                        minecraft       : minecraftVersion,
                        fabricLoader    : fabricLoaderVersion,
                        fabricLoaderHash: Utils.sha1(tmpFabric.toFile()),
                ],
                libraries  : libraries,
                fabricExtra: fabricLibraries
        ]
        outputs.files.singleFile.text = JsonOutput.toJson(output)
    }
}
