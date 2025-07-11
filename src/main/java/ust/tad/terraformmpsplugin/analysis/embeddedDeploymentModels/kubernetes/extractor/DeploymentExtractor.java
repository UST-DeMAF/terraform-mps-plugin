package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor;

import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.common.types.StringStringMap;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.workload.deployment.KubernetesDeployment;
import ust.tad.terraformmpsplugin.terraformmodel.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeploymentExtractor extends BaseExtractor {

    static KubernetesDeployment extractKubernetesDeployment(Resource kubernetesDeploymentResource) {
        KubernetesDeployment deployment = new KubernetesDeployment();

        extractName(kubernetesDeploymentResource, deployment);
        extractReplicas(kubernetesDeploymentResource, deployment);
        extractLabels(kubernetesDeploymentResource, deployment);
        extractSelectorMatchLabels(kubernetesDeploymentResource, deployment);
        try {
            deployment.setPodSpecs(Set.of(PodSpecExtractor.extractPodSpec(
                    kubernetesDeploymentResource.getBlockByBlockType("spec", List.of("template")))));
        } catch (BlockNotFoundException e) {
            throw new RuntimeException(e);
        }
        return deployment;
    }

    private static void extractName(Resource kubernetesDeploymentResource,
                                    KubernetesDeployment deployment) {
        try {
            Block metadata = kubernetesDeploymentResource.getBlockByBlockType("metadata",
                    List.of());
            deployment.setName(removeEnclosingTicks(metadata.findArgumentByIdentifier("name").getExpression()));
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractReplicas(Resource kubernetesDeploymentResource,
                                        KubernetesDeployment deployment) {
        try {
            Block spec = kubernetesDeploymentResource.getBlockByBlockType("spec", List.of());
            deployment.setReplicas(Integer.parseInt(spec.findArgumentByIdentifier("replicas").getExpression()));
        } catch (BlockNotFoundException | ArgumentNotFoundException |
                 NumberFormatException ignored) {
        }
    }

    private static void extractLabels(Resource kubernetesDeploymentResource,
                                      KubernetesDeployment deployment) {
        try {
            Block labelsBlock = kubernetesDeploymentResource.getBlockByBlockType("metadata",
                    List.of("labels"));
            Set<StringStringMap> labels = new HashSet<>();
            for (Argument argument : labelsBlock.getArguments()) {
                StringStringMap label =
                        new StringStringMap(removeEnclosingTicks(argument.getIdentifier()),
                                removeEnclosingTicks(argument.getExpression()));
                labels.add(label);
            }
            deployment.setLabels(labels);
        } catch (BlockNotFoundException ignored) {
        }
    }

    private static void extractSelectorMatchLabels(Resource kubernetesDeploymentResource,
                                                   KubernetesDeployment deployment) {
        try {
            Block matchLabelsBlock = kubernetesDeploymentResource.getBlockByBlockType("spec",
                    List.of("selector", "match_labels"));
            Set<StringStringMap> labels = new HashSet<>();
            for (Argument argument : matchLabelsBlock.getArguments()) {
                StringStringMap label =
                        new StringStringMap(removeEnclosingTicks(argument.getIdentifier()),
                                removeEnclosingTicks(argument.getExpression()));
                labels.add(label);
            }
            deployment.setSelectorMatchLabels(labels);
        } catch (BlockNotFoundException ignored) {
        }
    }
}
