package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor;

import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.KubernetesDeploymentModel;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.configStorageResources.PersistentVolumeClaim;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.service.KubernetesService;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.workload.deployment.KubernetesDeployment;
import ust.tad.terraformmpsplugin.terraformmodel.Resource;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class EmbeddedKubernetesExtractor {

    public static KubernetesDeploymentModel extractKubernetesDeploymentModel(TerraformDeploymentModel terraformDeploymentModel, UUID transformationProcessId) {
        List<Resource> kubernetesResources =
                terraformDeploymentModel.getResources().stream().filter(resource -> resource.getResourceType().startsWith("kubernetes_")).collect(Collectors.toList());
        KubernetesDeploymentModel kubernetesDeploymentModel = new KubernetesDeploymentModel();
        kubernetesDeploymentModel.setTransformationProcessId(transformationProcessId);

        Set<KubernetesDeployment> deployments = new HashSet<>();
        Set<KubernetesService> services = new HashSet<>();
        Set<PersistentVolumeClaim> persistentVolumeClaims = new HashSet<>();

        for (Resource kubernetesResource : kubernetesResources) {
            switch (kubernetesResource.getResourceType()) {
                case "kubernetes_deployment":
                case "kubernetes_deployment_v1":
                case "kubernetes_stateful_set":
                case "kubernetes_stateful_set_v1":
                    deployments.add(DeploymentExtractor.extractKubernetesDeployment(kubernetesResource));
                    break;
                case "kubernetes_service":
                    services.add(ServiceExtractor.extractKubernetesService(kubernetesResource));
                    break;
                case "kubernetes_persistence_volume_claim":
                case "kubernetes_persistence_volume_claim_v1":
                    persistentVolumeClaims.add(PVCExtractor.extractPVC(kubernetesResource));
                    break;
            }
        }

        kubernetesDeploymentModel.setDeployments(deployments);
        kubernetesDeploymentModel.setServices(services);
        kubernetesDeploymentModel.setPersistentVolumeClaims(persistentVolumeClaims);

        return kubernetesDeploymentModel;
    }

}
