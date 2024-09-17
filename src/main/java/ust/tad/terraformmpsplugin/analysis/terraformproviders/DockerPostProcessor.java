package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import java.util.List;
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
      throws PostProcessorFailedException, InvalidPropertyValueException {
    createContainerRuntime(tadm);
    return tadm;
  }

  /**
   * Creates and adds the containeruntime object and type to tadm
   *
   * @param tadm the tadm to be modified
   */
  private void createContainerRuntime(TechnologyAgnosticDeploymentModel tadm) throws InvalidPropertyValueException {
    Property name = new Property("name", PropertyType.STRING, false, null, Confidence.SUSPECTED);
    Property version = new Property("version", PropertyType.STRING, false, null, null);
    ComponentType containerRuntimeType = new ComponentType("container_runtime", null, List.of(name, version), null, null);
    tadm.getComponentTypes().add(containerRuntimeType);

    Property runtimeName = new Property("name", PropertyType.STRING, false, "Docker Desktop", Confidence.SUSPECTED);
    Property runtimeVersion = new Property("version", PropertyType.STRING, false, "4.34.2", Confidence.SUSPECTED);
    Component containerRuntime = new Component("default-container-runtime", null, List.of(runtimeName, runtimeVersion), null, containerRuntimeType, null, Confidence.SUSPECTED);
    tadm.getComponents().add(containerRuntime);
  }
}
