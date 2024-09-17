package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.models.tadm.*;

import javax.sound.sampled.Port;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

@Service
public class DockerPostProcessor {
    private final String PROVIDERNAME = "docker_container";

    /**
     * Tests if this Post-Processor is applicable for the given technology-agnostic deployment
     * model.
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
    public TechnologyAgnosticDeploymentModel runPostProcessor(TechnologyAgnosticDeploymentModel tadm) throws PostProcessorFailedException, InvalidPropertyValueException {
        createContainerRuntime(tadm);
        return tadm;
    }

    /**
     * Creates and adds the containeruntime object and type to tadm
     *
     * @param tadm
     */
    private void createContainerRuntime(TechnologyAgnosticDeploymentModel tadm) throws InvalidPropertyValueException {
        Property name = new Property();
        name.setKey("name");
        name.setType(PropertyType.STRING);
        name.setRequired(false);
        Property version = new Property();
        version.setKey("version");
        version.setType(PropertyType.STRING);
        version.setRequired(false);
        List<Property> properties = new ArrayList<>();
        properties.add(name);
        properties.add(version);

        ComponentType containerRuntimeType = new ComponentType();
        containerRuntimeType.setName("container_runtime");
        containerRuntimeType.setProperties(null);
        tadm.getComponentTypes().add(containerRuntimeType);

        Component containerRuntime = new Component();
        containerRuntime.setType(containerRuntimeType);
        containerRuntime.setName("default-container-runtime");
        Property runtimeName = new Property();
        runtimeName.setKey("name");
        runtimeName.setValue("Docker Desktop");
        List<Property> runtimeProperties = new ArrayList<>();
        runtimeProperties.add(runtimeName);
        containerRuntime.setProperties(runtimeProperties);
        tadm.getComponents().add(containerRuntime);
    }
}
