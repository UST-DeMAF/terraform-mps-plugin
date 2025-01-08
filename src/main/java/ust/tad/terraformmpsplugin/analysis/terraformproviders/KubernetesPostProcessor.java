package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.models.tadm.Component;
import ust.tad.terraformmpsplugin.models.tadm.ComponentType;
import ust.tad.terraformmpsplugin.models.tadm.TechnologyAgnosticDeploymentModel;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class KubernetesPostProcessor {

  private final String PROVIDERNAME = "kubernetes";

  private final Set<String> RESOURCESTOREMOVE = Set.of("kubernetes_namespace");

  /**
   * Tests if this Post-Processor is applicable for the given technology-agnostic deployment model.
   *
   * @param tadm the technology-agnostic deployment model to test.
   * @return true if applicable, false otherwise.
   */
  public boolean isPostProcessorApplicable(TechnologyAgnosticDeploymentModel tadm) {
    for (ComponentType componentType : tadm.getComponentTypes()) {
      if (componentType.getName().startsWith(PROVIDERNAME)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Runs the post-processing steps for this provider.
   *
   * @param tadm the technology-agnostic deployment model to post-process.
   * @return the modified technology-agnostic deployment model.
   * @throws PostProcessorFailedException if the post-processing fails.
   */
  public TechnologyAgnosticDeploymentModel runPostProcessor(TechnologyAgnosticDeploymentModel tadm)
      throws PostProcessorFailedException {
    removeComponentTypes(tadm);
    return tadm;
  }

  /**
   * Remove specific ComponentTypes from a given technology-agnostic deployment model.
   * The ComponentTypes are filtered based on specific resources of the Terraform Kubernetes
   * provider.
   * For example, the "kubernetes_namespace" is removed, because it is not an actual software
   * component but a logical construct of Kubernetes.
   *
   * @param tadm the technology-agnostic deployment model from which to remove the ComponentTypes.
   */
  private void removeComponentTypes(TechnologyAgnosticDeploymentModel tadm) {
    for (ComponentType componentType: tadm.getComponentTypes()) {
      if (RESOURCESTOREMOVE.contains(componentType.getName())) {
        removeComponentsAndRelationsOfComponentType(tadm, componentType);
        tadm.setComponentTypes(tadm.getComponentTypes().stream().filter(componentType1 ->
                !componentType1.equals(componentType)).collect(Collectors.toList()));
      }
    }
  }

  /**
   * Remove the Components of a given ComponentType and the associated Relations of these Components.
   *
   * @param tadm the technology-agnostic deployment model from which to remove the Components and
   *             Relations.
   * @param componentType the ComponentType for which the Components should be removed.
   */
  private void removeComponentsAndRelationsOfComponentType(TechnologyAgnosticDeploymentModel tadm,
                                                          ComponentType componentType) {
    List<Component> componentsToRemove = tadm.getComponents().stream().filter(component ->
            component.getType().equals(componentType)).collect(Collectors.toList());
    tadm.setRelations(tadm.getRelations().stream().filter(relation ->
                    !(componentsToRemove.contains(relation.getSource())
                            || componentsToRemove.contains(relation.getTarget())))
            .collect(Collectors.toList()));
    tadm.setComponents(tadm.getComponents().stream().filter(
            Predicate.not(componentsToRemove::contains)).collect(Collectors.toList()));
  }
}
