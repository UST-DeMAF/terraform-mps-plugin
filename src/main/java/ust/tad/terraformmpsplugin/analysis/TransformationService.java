package ust.tad.terraformmpsplugin.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;
import ust.tad.terraformmpsplugin.models.tadm.*;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

@Service
public class TransformationService {
  @Value("${mps.location}")
  private String mpsLocation;

  @Value("${mps.inputModel.path}")
  private String mpsInputPath;

  @Value("${mps.inputTADM.path}")
  private String mpsInputTADMPath;

  @Value("${mps.result.path}")
  private String mpsOutputPath;

  @Autowired private AnalysisTaskResponseSender analysisTaskResponseSender;

  /**
   * Transforms given the internal Terraform model to an EDMM model. Uses the MPS project for a
   * model-to-model transformation. In the first step, creates a file containing the tsdm model in
   * the MPS Terraform language from the given internal Terraform model. Then, the MPS
   * transformation is run, using the Gradle build scripts. After that, the resulting EDMM model is
   * imported.
   *
   * @param tadm the technology-agnostic deployment model that the transformation result shall be
   *     added to
   * @param terraformDeploymentModel the Terraform deployment model to transform
   * @return the modified technology-agnostic deployment model.
   * @throws IOException if the MPS transformation cannot be executed or the deserialization of the
   *     transformation result fails.
   */
  public TechnologyAgnosticDeploymentModel transformInternalToTADM(
      final UUID taskId,
      TechnologyAgnosticDeploymentModel tadm,
      final TerraformDeploymentModel terraformDeploymentModel)
      throws IOException {
    createMPSTerraformDeploymentModel(terraformDeploymentModel);
    createMPSExistingTADMInput(tadm);
    runMPSTransformation();
    TechnologyAgnosticDeploymentModel transformationResult = importMPSResult();
    transformationResult.setTransformationProcessId(tadm.getTransformationProcessId());
    transformationResult.setId(tadm.getId());
    removeTransformationMarker(transformationResult);
    sendDockerImageAnalysisTask(taskId, transformationResult, tadm);
    return transformationResult;
  }

  /**
   * Output the Terraform Deployment Model to an XML file on the file system using the Jackson
   * ObjectMapper for XML. The location on the file system is where the MPS project expects the
   * input model.
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
   * Output the existing TADM to an XML file on the file system using the Jackson
   * ObjectMapper for XML. The location on the file system is where the MPS project expects the
   * input model.
   *
   * @param tadm the existing TADM to transform.
   * @throws IOException if the XML file cannot be created.
   */
  private void createMPSExistingTADMInput(
          final TechnologyAgnosticDeploymentModel tadm) throws IOException {
    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.writeValue(new File(mpsInputTADMPath), tadm);
  }

  /**
   * Run the model-to-model transformation using the MPS project by executing two Gradle tasks. The
   * first task ensures that MPS is ready to execute the transformation. The second task executes
   * the transformation by running the supplied build scripts.
   *
   * @throws IOException if the execution of the Gradle tasks fails.
   */
  private void runMPSTransformation() throws IOException {
    CommandLine prepareMps =
        CommandLine.parse("./" + mpsLocation + "/gradlew -p " + mpsLocation + " prepareMps");
    CommandLine mpsBuild =
        CommandLine.parse("./" + mpsLocation + "/gradlew -p " + mpsLocation + " mpsBuild");
    DefaultExecutor executor = new DefaultExecutor();
    executor.execute(prepareMps);
    executor.execute(mpsBuild);
  }

  /**
   * Imports the result from the MPS transformation. The result is a YAML file located at the
   * mpsOutputPath. Uses Jackson Databind ObjectMapper to deserialize the YAML into Java Objects.
   * Adds Mixins to the ObjectMapper for deserializing the transformation result as it contains
   * references through the name field of components, component types, and relation types instead of
   * the full POJO.
   *
   * @return the transformation result.
   * @throws IOException if the deserialization fails.
   */
  public TechnologyAgnosticDeploymentModel importMPSResult() throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    mapper.addMixIn(Component.class, ComponentMixIn.class);
    mapper.addMixIn(ComponentType.class, ComponentTypeMixIn.class);
    mapper.addMixIn(RelationType.class, RelationTypeMixIn.class);
    mapper.addMixIn(
        TechnologyAgnosticDeploymentModel.class, TechnologyAgnosticDeploymentModelMixIn.class);
    return mapper.readValue(new File(mpsOutputPath), TechnologyAgnosticDeploymentModel.class);
  }

  /**
   * Remove the property used as transformation marker from the transformation result
   *
   * @param transformationResult the tadm with the transformation result.
   */
  private void removeTransformationMarker(TechnologyAgnosticDeploymentModel transformationResult) {
    List<Property> properties = transformationResult.getProperties();
    properties.removeIf(property -> property.getKey().equals("transformationMarker"));
    transformationResult.setProperties(properties);
  }

  /**
   * Create an analysis task for analyzing Docker images if present in the transformation result.
   *
   * @param taskId the ID of the current analysis task.
   * @param transformationResult the TADM containing only the result of the transformation.
   * @param tadm the complete TADM of the current transformation process.
   */
  private void sendDockerImageAnalysisTask(UUID taskId,
                                           TechnologyAgnosticDeploymentModel transformationResult,
                                           TechnologyAgnosticDeploymentModel tadm) {
    List<String> componentsToAnalyze = new ArrayList<>();
    for (Component component : transformationResult.getComponents()) {
      if (!tadm.getComponents().contains(component) && component.getArtifacts().stream().anyMatch(
              artifact -> artifact.getType().equals("docker_image"))) {
        componentsToAnalyze.add(component.getId());
      }
    }
    if (!componentsToAnalyze.isEmpty()) {
      analysisTaskResponseSender.sendEmbeddedDeploymentModelAnalysisRequestFromTADMEntities(
              Map.of("Component", componentsToAnalyze), taskId, tadm.getTransformationProcessId()
              , "docker");
    }
  }
}
