package ust.tad.terraformmpsplugin.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.analysis.terraformproviders.AzureRMPostProcessor;
import ust.tad.terraformmpsplugin.analysis.terraformproviders.PostProcessorFailedException;
import ust.tad.terraformmpsplugin.models.tadm.*;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.io.File;
import java.io.IOException;

import static ust.tad.terraformmpsplugin.analysis.util.ComponentTypePostProcessor.mergeDuplicateComponentTypes;


@Service
public class TransformationService {
    @Value("${mps.location}")
    private String mpsLocation;

    @Value("${mps.inputModel.path}")
    private String mpsInputPath;

    @Value("${mps.result.path}")
    private String mpsOutputPath;

    @Value("${runProviderPostProcessors}")
    private boolean runProviderPostProcessors;

    @Autowired
    private AzureRMPostProcessor azureRMPostProcessor;

    /**
     * Transforms given the internal Terraform model to an EDMM model.
     * Uses the MPS project for a model-to-model transformation.
     * In the first step, creates a file containing the tsdm model in the MPS Terraform
     * language from the given internal Terraform model.
     * Then, the MPS transformation is run, using the Gradle build scripts.
     * After that, the resulting EDMM model is imported and added to the already existing
     * technology-agnostic deployment model.
     * If configured, provider-specific post-processors are run.
     *
     * @param tadm                     the technology-agnostic deployment model that the
     *                                 transformation result shall be added to
     * @param terraformDeploymentModel the Terraform deployment model to transform
     * @return the modified technology-agnostic deployment model.
     * @throws IOException if the MPS transformation cannot be executed or the deserialization of
     *                     the transformation result fails.
     */
    public TechnologyAgnosticDeploymentModel transformInternalToTADM(
            TechnologyAgnosticDeploymentModel tadm,
            final TerraformDeploymentModel terraformDeploymentModel)
            throws IOException {
        createMPSTerraformDeploymentModel(terraformDeploymentModel);
        runMPSTransformation();
        TechnologyAgnosticDeploymentModel transformationResult = importMPSResult();
        TechnologyAgnosticDeploymentModel mergedResult = mergeDuplicateComponentTypes(transformationResult);
        tadm.addFromOtherTADM(mergedResult);
        if (runProviderPostProcessors) {
            tadm = postProcessTADM(tadm);
        }
        return tadm;
    }

    /**
     * Output the Terraform Deployment Model to an XML file on the file system using the
     * Jackson ObjectMapper for XML.
     * The location on the file system is where the MPS project expects the input model.
     *
     * @param terraformDeploymentModel the Terraform deployment model to transform.
     * @throws IOException if the XML file cannot be created.
     */
    private void createMPSTerraformDeploymentModel(
            final TerraformDeploymentModel terraformDeploymentModel) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.writeValue(new File(mpsInputPath), terraformDeploymentModel);
    }

    /**
     * Run the model-to-model transformation using the MPS project by executing two Gradle tasks.
     * The first task ensures that MPS is ready to execute the transformation.
     * The second task executes the transformation by running the supplied build scripts.
     *
     * @throws IOException if the execution of the Gradle tasks fails.
     */
    private void runMPSTransformation() throws IOException {
        CommandLine prepareMps = CommandLine.parse("./"
                + mpsLocation
                + "/gradlew -p "
                + mpsLocation
                + " prepareMps");
        CommandLine mpsBuild = CommandLine.parse("./"
                + mpsLocation
                + "/gradlew -p "
                + mpsLocation
                + " mpsBuild");
        DefaultExecutor executor = new DefaultExecutor();
        executor.execute(prepareMps);
        executor.execute(mpsBuild);
    }

    /**
     * Imports the result from the MPS transformation.
     * The result is a YAML file located at the mpsOutputPath.
     * Uses Jackson Databind ObjectMapper to deserialize the YAML into Java Objects.
     * Adds Mixins to the ObjectMapper for deserializing the transformation result as it contains
     * references through the name field of components, component types, and relation types
     * instead of the full POJO.
     *
     * @return the transformation result.
     * @throws IOException if the deserialization fails.
     */
    private TechnologyAgnosticDeploymentModel importMPSResult() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.findAndRegisterModules();
        mapper.addMixIn(Component.class, ComponentMixIn.class);
        mapper.addMixIn(ComponentType.class, ComponentTypeMixIn.class);
        mapper.addMixIn(RelationType.class, RelationTypeMixIn.class);
        mapper.addMixIn(TechnologyAgnosticDeploymentModel.class,
                TechnologyAgnosticDeploymentModelMixIn.class);
        return mapper.readValue(new File(mpsOutputPath), TechnologyAgnosticDeploymentModel.class);
    }

    /**
     * Run post-processing steps of the technology-agnostic deployment model for specific Terraform
     * Providers.
     * Each available post-processor is only run if it is applicable for the given
     * technology-agnostic deployment model
     * If no post-processor is applicable or the post-processor fails, the technology-agnostic
     * deployment model remains unchanged.
     *
     * @param tadm the technology-agnostic deployment model to post-process
     * @return the modified technology-agnostic deployment model or the original one of the
     * processing failed
     */
    private TechnologyAgnosticDeploymentModel postProcessTADM(TechnologyAgnosticDeploymentModel tadm) {
        if (azureRMPostProcessor.isPostProcessorApplicable(tadm)) {
            try {
                return azureRMPostProcessor.runPostProcessor(tadm);
            } catch (PostProcessorFailedException e) {
                return tadm;
            }
        }
        return tadm;
    }

}
