package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.BaseEmbeddedDeploymentModelProcessor;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor.EmbeddedKubernetesExtractor;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.KubernetesDeploymentModel;
import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;
import ust.tad.terraformmpsplugin.models.ModelsService;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.util.UUID;

@Service
public class EmbeddedKubernetesProcessor extends BaseEmbeddedDeploymentModelProcessor {

    @Autowired
    private AnalysisTaskResponseSender analysisTaskResponseSender;

    @Autowired
    private ModelsService modelsService;

    private static boolean isEmbeddedKubernetesDeploymentModelPresent(TerraformDeploymentModel terraformDeploymentModel) {
        return terraformDeploymentModel.getResources().stream().anyMatch(resource -> resource.getResourceType().startsWith("kubernetes_"));
    }

    public void processEmbeddedKubernetesDeploymentModel(UUID taskId,
                                                         UUID transformationProcessId,
                                                         TerraformDeploymentModel terraformDeploymentModel) {
        if (isEmbeddedKubernetesDeploymentModelPresent(terraformDeploymentModel)) {
            resolveReferences(terraformDeploymentModel);
            KubernetesDeploymentModel kubernetesDeploymentModel =
                    EmbeddedKubernetesExtractor.extractKubernetesDeploymentModel(terraformDeploymentModel, transformationProcessId);
            modelsService.updateKubernetesDeploymentModel(kubernetesDeploymentModel);
            sendKubernetesAnalysisTask(taskId, transformationProcessId, kubernetesDeploymentModel);
        }
    }

    private void sendKubernetesAnalysisTask(UUID taskId, UUID transformationProcessId,
                                            KubernetesDeploymentModel kubernetesDeploymentModel) {
        analysisTaskResponseSender.sendEmbeddedDeploymentModelAnalysisRequestWithEmbeddedDM(
                taskId, transformationProcessId, "kubernetes", kubernetesDeploymentModel.getId());
    }


}
