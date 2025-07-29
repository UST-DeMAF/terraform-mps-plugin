package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor;

import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.configStorageResources.PersistentVolumeClaim;
import ust.tad.terraformmpsplugin.terraformmodel.ArgumentNotFoundException;
import ust.tad.terraformmpsplugin.terraformmodel.Block;
import ust.tad.terraformmpsplugin.terraformmodel.BlockNotFoundException;
import ust.tad.terraformmpsplugin.terraformmodel.Resource;

import java.util.List;

public class PVCExtractor extends BaseExtractor {

    static PersistentVolumeClaim extractPVC(Resource kubernetesPVCResource) {
        PersistentVolumeClaim pvc = new PersistentVolumeClaim();

        extractPVCName(kubernetesPVCResource, pvc);
        extractVolumeName(kubernetesPVCResource, pvc);
        extractResources(kubernetesPVCResource, pvc);

        return pvc;
    }

    private static void extractPVCName(Resource pVCResource,
                                       PersistentVolumeClaim pvc) {
        try {
            Block metadata = pVCResource.getBlockByBlockType("metadata", List.of());
            pvc.setName(removeEnclosingTicks(metadata.findArgumentByIdentifier("name").getExpression()));
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractVolumeName(Resource pVCResource,
                                          PersistentVolumeClaim pvc) {
        try {
            Block spec = pVCResource.getBlockByBlockType("spec", List.of());
            pvc.setVolumeName(removeEnclosingTicks(spec.findArgumentByIdentifier("volume_name").getExpression()));
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractResources(Resource pVCResource,
                                         PersistentVolumeClaim pvc) {
        try {
            Block requests = pVCResource.getBlockByBlockType("spec", List.of("resources",
                    "requests"));
            pvc.setRequests(requests.findArgumentByIdentifier("storage").getExpression());
            Block limits = pVCResource.getBlockByBlockType("spec", List.of("resources", "limits"));
            pvc.setLimit(limits.findArgumentByIdentifier("storage").getExpression());
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }
}
