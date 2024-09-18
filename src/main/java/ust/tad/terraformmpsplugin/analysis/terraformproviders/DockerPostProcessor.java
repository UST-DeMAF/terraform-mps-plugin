package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.models.tadm.*;

@Service
public class DockerPostProcessor {
  private final String PROVIDERNAME = "docker_container";

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
      throws PostProcessorFailedException, InvalidPropertyValueException, InvalidRelationException {
    trimDependsOn(tadm);
    createContainerRuntime(tadm);
    createHostedOnRelations(tadm);
    createConnectsToRelations(tadm);
    return tadm;
  }

  /**
   * Trims the depends_on property as it might have issues from parsing.
   *
   * @param tadm the tadm to be modified
   */
  private void trimDependsOn(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidPropertyValueException {
    for (Component component : tadm.getComponents()) {
      Optional<Property> dependsOn =
          component.getProperties().stream()
              .filter(prop -> "depends_on".equals(prop.getKey()))
              .findFirst();
      if (dependsOn.isPresent()) {
        String value = (String) dependsOn.get().getValue();
        value = value.trim().replace("]]", "]").replace("\\", "").replace("\n", "");
        value = value.replace("docker_container.", ""); // Remove prefix
        dependsOn.get().setValue(value);
      }
    }
  }

  /**
   * Creates and adds the container runtime object and type to tadm
   *
   * @param tadm the tadm to be modified
   */
  private void createContainerRuntime(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidPropertyValueException {
    Property name = new Property("name", PropertyType.STRING, false, null, Confidence.SUSPECTED);
    Property version = new Property("version", PropertyType.STRING, false, null, null);
    ComponentType containerRuntimeType =
        new ComponentType("container_runtime", null, List.of(name, version), null, null);
    tadm.getComponentTypes().add(containerRuntimeType);

    Property runtimeName =
        new Property("name", PropertyType.STRING, false, "Docker Desktop", Confidence.SUSPECTED);
    Property runtimeVersion =
        new Property("version", PropertyType.STRING, false, "4.34.2", Confidence.SUSPECTED);
    Component containerRuntime =
        new Component(
            "default-container-runtime",
            null,
            List.of(runtimeName, runtimeVersion),
            null,
            containerRuntimeType,
            null,
            Confidence.SUSPECTED);
    tadm.getComponents().add(containerRuntime);
  }

  /**
   * Creates hostedOn relations from each docker_container to the default-container-runtime.
   *
   * @param tadm the tadm to be modified
   */
  private void createHostedOnRelations(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidRelationException {
    List<Relation> hostedOnRelations = new ArrayList<>();
    Component host =
        tadm.getComponents().stream()
            .filter(cmp -> "default-container-runtime".equals(cmp.getName()))
            .findFirst()
            .orElseThrow();
    RelationType hostedOnRelationType =
        tadm.getRelationTypes().stream()
            .filter(type -> "HostedOn".equals(type.getName()))
            .findFirst()
            .orElseThrow();

    for (Component component : tadm.getComponents()) {
      if (component.getType().getName().equals("docker_container")) {
        hostedOnRelations.add(
            new Relation(
                component.getName() + "_hostedOn_" + host.getName(),
                null,
                List.of(),
                List.of(),
                hostedOnRelationType,
                component,
                host,
                Confidence.CONFIRMED));
      }
    }
    tadm.getRelations().addAll(hostedOnRelations);
  }

  /**
   * Creates connectsTo relations from each docker_container component to each of the components he
   * has marked as "depends_on"
   *
   * @param tadm the tadm to be modified
   */
  private void createConnectsToRelations(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidRelationException {
    List<Relation> connectsToRelations = new ArrayList<>();

    RelationType conntectsToRelationType =
        tadm.getRelationTypes().stream()
            .filter(type -> "ConnectsTo".equals(type.getName()))
            .findFirst()
            .orElseThrow();

    for (Component component : tadm.getComponents()) {
      if (component.getType().getName().equals("docker_container")) {
        Optional<Property> dependsOn =
            component.getProperties().stream()
                .filter(prop -> "depends_on".equals(prop.getKey()))
                .findFirst();
        if (dependsOn.isPresent()) {
          List<String> dependentComponents =
              List.of(
                  dependsOn
                      .get()
                      .getValue()
                      .toString()
                      .replace("[", "")
                      .replace("]", "")
                      .replace(" ", "")
                      .split(","));
          for (String dependentComponent : dependentComponents) {

            Component target =
                tadm.getComponents().stream()
                    .filter(cmp -> dependentComponent.equals(cmp.getName()))
                    .findFirst()
                    .orElseThrow();

            connectsToRelations.add(
                new Relation(
                    component.getName() + "_connectsTo_" + dependentComponent,
                    null,
                    List.of(),
                    List.of(),
                    conntectsToRelationType,
                    component,
                    target,
                    Confidence.CONFIRMED));
          }
        }
      }
    }
    tadm.getRelations().addAll(connectsToRelations);
  }
}
