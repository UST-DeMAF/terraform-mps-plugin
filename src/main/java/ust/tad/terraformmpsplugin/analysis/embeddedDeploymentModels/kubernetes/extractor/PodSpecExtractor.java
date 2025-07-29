package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor;

import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.common.types.StringStringMap;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.configStorageResources.Volume;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.workload.pods.*;
import ust.tad.terraformmpsplugin.terraformmodel.Argument;
import ust.tad.terraformmpsplugin.terraformmodel.ArgumentNotFoundException;
import ust.tad.terraformmpsplugin.terraformmodel.Block;
import ust.tad.terraformmpsplugin.terraformmodel.BlockNotFoundException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PodSpecExtractor extends BaseExtractor {

    public static KubernetesPodSpec extractPodSpec(Block podSpec) {
        KubernetesPodSpec kubernetesPodSpec = new KubernetesPodSpec();

        extractRestartPolicy(podSpec, kubernetesPodSpec);
        extractContainers(podSpec, kubernetesPodSpec);
        extractLabels(podSpec, kubernetesPodSpec);
        extractVolumes(podSpec, kubernetesPodSpec);

        return kubernetesPodSpec;
    }

    private static void extractRestartPolicy(Block podSpec, KubernetesPodSpec kubernetesPodSpec) {
        try {
            Block spec = podSpec.findNestedBlockByBlockType(List.of("spec"));
            kubernetesPodSpec.setRestartPolicy(removeEnclosingTicks(spec.findArgumentByIdentifier("restart_policy").getExpression()));
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractContainers(Block podSpec, KubernetesPodSpec kubernetesPodSpec) {
        try {
            Block containerBlock = podSpec.findNestedBlockByBlockType(List.of("spec", "container"));
            Container container = new Container();
            extractContainerArguments(containerBlock, container);
            extractContainerPort(containerBlock, container);
            extractContainerEnv(containerBlock, container);
            extractContainerVolumeMounts(containerBlock, container);
            kubernetesPodSpec.setContainers(Set.of(container));
        } catch (BlockNotFoundException ignored) {
        }
    }

    private static void extractContainerArguments(Block containerBlock, Container container) {
        try {
            container.setName(removeEnclosingTicks(containerBlock.findArgumentByIdentifier("name").getExpression()));
            container.setImage(containerBlock.findArgumentByIdentifier("image").getExpression());
            container.setImagePullPolicy(containerBlock.findArgumentByIdentifier(
                    "image_pull_policy").getExpression());
            container.setCommand(Arrays.asList(containerBlock.findArgumentByIdentifier("command").getExpression().replace("[", "").replace("]", "").split(",")));
            container.setArgs(Arrays.asList(containerBlock.findArgumentByIdentifier("args").getExpression().replace("[", "").replace("]", "").split(",")));
        } catch (ArgumentNotFoundException ignored) {
        }
    }

    private static void extractContainerPort(Block containerBlock, Container container) {
        try {
            List<Block> portBlocks = containerBlock.findAllNestedBlocksByBlockType("port");
            Set<ContainerPort> ports = new HashSet<>();
            for (Block portBlock : portBlocks) {
                ContainerPort port = new ContainerPort();
                port.setPort(Integer.parseInt(portBlock.findArgumentByIdentifier("container_port").getExpression()));
                port.setName(portBlock.findArgumentByIdentifier("name").getExpression());
                ports.add(port);
            }
            container.setContainerPorts(ports);
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractContainerEnv(Block containerBlock, Container container) {
        try {
            List<Block> envBlocks = containerBlock.findAllNestedBlocksByBlockType("env");
            Set<EnvironmentVariable> envVars = new HashSet<>();
            for (Block envBlock : envBlocks) {
                EnvironmentVariable envVar = new EnvironmentVariable(
                        removeEnclosingTicks(envBlock.findArgumentByIdentifier("name").getExpression()),
                        envBlock.findArgumentByIdentifier("value").getExpression());
                envVars.add(envVar);
            }
            container.setEnvironmentVariables(envVars);
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractContainerVolumeMounts(Block containerBlock, Container container) {
        try {
            List<Block> volumeMountBlocks = containerBlock.findAllNestedBlocksByBlockType(
                    "volume_mount");
            Set<VolumeMount> volumeMounts = new HashSet<>();
            for (Block volumeMountBlock : volumeMountBlocks) {
                VolumeMount volumeMount = new VolumeMount();
                volumeMount.setMountPath(volumeMountBlock.findArgumentByIdentifier("mount_path").getExpression());
                volumeMount.setName(removeEnclosingTicks(volumeMountBlock.findArgumentByIdentifier("name").getExpression()));
                volumeMount.setMountPropagation(volumeMountBlock.findArgumentByIdentifier(
                        "mount_path").getExpression());
                volumeMount.setReadOnly(Boolean.parseBoolean(volumeMountBlock.findArgumentByIdentifier("read_only").getExpression()));
                volumeMount.setSubPath(volumeMountBlock.findArgumentByIdentifier("sub_path").getExpression());
                volumeMounts.add(volumeMount);
            }
            container.setVolumeMounts(volumeMounts);
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }


    private static void extractLabels(Block podSpec, KubernetesPodSpec kubernetesPodSpec) {
        try {
            Block labelsBlock = podSpec.findNestedBlockByBlockType(List.of("metadata", "labels"));
            Set<StringStringMap> labels = new HashSet<>();
            for (Argument argument : labelsBlock.getArguments()) {
                StringStringMap label = new StringStringMap(argument.getIdentifier(),
                        argument.getExpression());
                labels.add(label);
            }
            kubernetesPodSpec.setLabels(labels);
        } catch (BlockNotFoundException ignored) {
        }
    }

    private static void extractVolumes(Block podSpec, KubernetesPodSpec kubernetesPodSpec) {
        try {
            Block specBlock = podSpec.findNestedBlockByBlockType(List.of("spec"));
            List<Block> volumeBlocks = specBlock.findAllNestedBlocksByBlockType("volume");
            Set<Volume> volumes = new HashSet<>();
            for (Block volumeBlock : volumeBlocks) {
                Volume volume = new Volume();
                volume.setName(volumeBlock.findArgumentByIdentifier("name").getExpression());
                Block pvcBlock = podSpec.findNestedBlockByBlockType(List.of(
                        "persistent_volume_claim"));
                volume.setPersistentVolumeClaimName(pvcBlock.findArgumentByIdentifier("claim_name"
                ).getExpression());
                volume.setPersistentVolumeClaimReadOnly(Boolean.parseBoolean(pvcBlock.findArgumentByIdentifier("read_only").getExpression()));
                volumes.add(volume);
            }
            kubernetesPodSpec.setVolumes(volumes);
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }
}
