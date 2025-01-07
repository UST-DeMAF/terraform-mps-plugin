package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
    trimArrayStrings(tadm);
    trimEnv(tadm);
    createDockerEngineComponentAndType(tadm);
    createHostedOnRelations(tadm);
    createConnectsToRelations(tadm);
    return tadm;
  }

  /**
   * Trims the key property as it might have issues from parsing.
   *
   * @param tadm the tadm to be modified
   */
  private void trimEnv(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidPropertyValueException {
    for (Component component : tadm.getComponents()) {
      Optional<Property> env =
          component.getProperties().stream()
              .filter(prop -> "env".equals(prop.getKey()))
              .findFirst();
      if (env.isPresent()) {
        String value = (String) env.get().getValue();
        value =
            value
                .trim()
                .replace("[, ", "[")
                .replace("]]", "]")
                .replace("\"", "")
                .replace("\\", "")
                .replace("\n", "");
        env.get().setValue(value);
      }
    }
  }

  /**
   * Trims the key property as it might have issues from parsing and flattens them into own
   * properties. Currently, not in use, but technically also possible.
   *
   * @param tadm the tadm to be modified
   */
  private void trimAndFlattenEnv(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidPropertyValueException {
    for (Component component : tadm.getComponents()) {
      Optional<Property> env =
          component.getProperties().stream()
              .filter(prop -> "env".equals(prop.getKey()))
              .findFirst();
      if (env.isPresent()) {
        String value = (String) env.get().getValue();
        List<String> envValues =
            List.of(
                value
                    .trim()
                    .replace("[, ", "[")
                    .replace("[", "")
                    .replace("]", "")
                    .replace("\"", "")
                    .replace("\\", "")
                    .replace("\n", "")
                    .split(","));

        component.getProperties().remove(env.get());
        for (String envValue : envValues) {
          String[] envParts = envValue.split("=");
          if (envParts.length == 2) {
            component
                .getProperties()
                .add(
                    new Property(
                        envParts[0].trim(),
                        PropertyType.STRING,
                        false,
                        envParts[1].trim(),
                        Confidence.CONFIRMED));
          }
        }
      }
    }
  }

  /**
   * Trims the array string properties as they might have issues from parsing.
   *
   * @param tadm the tadm to be modified
   */
  private void trimArrayStrings(TechnologyAgnosticDeploymentModel tadm)
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

      // Commands are collapsed to single line and then re-formated in the output
      List<Property> arrayStrings =
          component.getProperties().stream()
              .filter(
                  prop ->
                      "healthcheck.test".equals(prop.getKey()) || "command".equals(prop.getKey()))
              .collect(Collectors.toCollection(ArrayList::new));
      for (Property arrayString : arrayStrings) {
        String value = (String) arrayString.getValue();
        value =
            value
                .replace("]", "")
                .replace("[", "")
                .replace("\\", "")
                .replace("\n", "")
                .replace("\"", "")
                .replace(",", "")
                .trim();
        arrayString.setValue(value);
      }
    }
  }

  /**
   * Creates and adds the docker engine component and type to tadm
   *
   * @param tadm the tadm to be modified
   */
  private void createDockerEngineComponentAndType(TechnologyAgnosticDeploymentModel tadm) {
    ComponentType dockerEngineType = new ComponentType();
    dockerEngineType.setName("DockerEngine");
    dockerEngineType.setParentType(tadm.getComponentTypes().stream()
            .filter(cmpType -> "BaseType".equals(cmpType.getName()))
            .findFirst()
            .orElseThrow());
    tadm.getComponentTypes().add(dockerEngineType);

    Component dockerEngine =
        new Component(
            "default-docker-engine",
            null,
            null,
            null,
            dockerEngineType,
            null,
            Confidence.SUSPECTED);
    tadm.getComponents().add(dockerEngine);
  }

  /**
   * Creates hostedOn relations from each docker_container to the default-docker-engine.
   *
   * @param tadm the tadm to be modified
   */
  private void createHostedOnRelations(TechnologyAgnosticDeploymentModel tadm)
      throws InvalidRelationException {
    List<Relation> hostedOnRelations = new ArrayList<>();
    Component host =
        tadm.getComponents().stream()
            .filter(cmp -> "default-docker-engine".equals(cmp.getName()))
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

    RelationType connectsToRelationType =
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
                    connectsToRelationType,
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
