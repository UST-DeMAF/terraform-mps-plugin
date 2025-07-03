package ust.tad.terraformmpsplugin.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ust.tad.terraformmpsplugin.analysis.terraformParser.BaseTerraformParser;
import ust.tad.terraformmpsplugin.analysis.terraformParser.FileExtensionNotSupportedException;
import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;
import ust.tad.terraformmpsplugin.analysistask.Location;
import ust.tad.terraformmpsplugin.models.ModelsService;
import ust.tad.terraformmpsplugin.models.tadm.InvalidPropertyValueException;
import ust.tad.terraformmpsplugin.models.tadm.InvalidRelationException;
import ust.tad.terraformmpsplugin.models.tadm.TechnologyAgnosticDeploymentModel;
import ust.tad.terraformmpsplugin.models.tsdm.*;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.*;

@Service
public class AnalysisService {

    private static final Set<String> supportedFileExtensions = Set.of("tf", "tf.json", "tofu",
            "tofu.json");

    @Autowired
    ModelsService modelsService;

    @Autowired
    AnalysisTaskResponseSender analysisTaskResponseSender;

    @Autowired
    private TransformationService transformationService;

    private TechnologySpecificDeploymentModel tsdm;
    private TechnologyAgnosticDeploymentModel tadm;
    private Set<Integer> newEmbeddedDeploymentModelIndexes = new HashSet<>();

    /**
     * Start the analysis of the deployment model. 1. Retrieve internal deployment models from
     * models
     * service 2. Parse in technology-specific deployment model from locations 3. Update tsdm
     * with new
     * information 4. Transform to EDMM entities and update tadm 5. Send updated models to models
     * service 6. Send AnalysisTaskResponse or EmbeddedDeploymentModelAnalysisRequests if present
     *
     * @param taskId
     * @param transformationProcessId
     * @param commands
     * @param locations
     */
    public void startAnalysis(
            UUID taskId,
            UUID transformationProcessId,
            List<String> commands,
            List<String> options,
            List<Location> locations) {
        TechnologySpecificDeploymentModel completeTsdm =
                modelsService.getTechnologySpecificDeploymentModel(transformationProcessId);
        this.tsdm = getExistingTsdm(completeTsdm, locations);
        if (tsdm == null) {
            analysisTaskResponseSender.sendFailureResponse(
                    taskId, "No technology-specific " + "deployment model found!");
            return;
        }
        this.tadm = modelsService.getTechnologyAgnosticDeploymentModel(transformationProcessId);

        try {
            runAnalysis(taskId, locations, commands);
        } catch (InvalidNumberOfContentException | URISyntaxException | IOException |
                 InvalidNumberOfLinesException | InvalidAnnotationException |
                 InvalidPropertyValueException | InvalidRelationException |
                 FileExtensionNotSupportedException | InterruptedException e) {
            e.printStackTrace();
            analysisTaskResponseSender.sendFailureResponse(taskId,
                    e.getClass() + ": " + e.getMessage());
            return;
        }

        updateDeploymentModels(this.tsdm, this.tadm);

        if (!newEmbeddedDeploymentModelIndexes.isEmpty()) {
            for (int index : newEmbeddedDeploymentModelIndexes) {
                analysisTaskResponseSender.sendEmbeddedDeploymentModelAnalysisRequestFromModel(
                        this.tsdm.getEmbeddedDeploymentModels().get(index), taskId);
            }
        }

        clearVariables();
        analysisTaskResponseSender.sendSuccessResponse(taskId);
    }

    private TechnologySpecificDeploymentModel getExistingTsdm(
            TechnologySpecificDeploymentModel tsdm, List<Location> locations) {
        for (DeploymentModelContent content : tsdm.getContent()) {
            for (Location location : locations) {
                if (location.getUrl().equals(content.getLocation())) {
                    return tsdm;
                }
            }
        }
        for (TechnologySpecificDeploymentModel embeddedDeploymentModel :
                tsdm.getEmbeddedDeploymentModels()) {
            TechnologySpecificDeploymentModel foundModel =
                    getExistingTsdm(embeddedDeploymentModel, locations);
            if (foundModel != null) {
                return foundModel;
            }
        }
        return null;
    }

    private void updateDeploymentModels(
            TechnologySpecificDeploymentModel tsdm, TechnologyAgnosticDeploymentModel tadm) {
        modelsService.updateTechnologySpecificDeploymentModel(tsdm);
        modelsService.updateTechnologyAgnosticDeploymentModel(tadm);
    }

    /**
     * Iterate over the locations and parse in all files that can be found. The file has to have the
     * fileextension ".tf", otherwise it will be ignored. If the given location is a directory,
     * iterate over all contained files. Removes the deployment model content associated with the
     * old
     * directory locations because it has been resolved to the contained files.
     *
     * @param locations
     * @param commands
     * @throws InvalidNumberOfContentException
     * @throws URISyntaxException
     * @throws InvalidAnnotationException
     * @throws InvalidNumberOfLinesException
     * @throws IOException
     * @throws InvalidPropertyValueException
     * @throws InvalidRelationException
     * @throws MalformedURLException
     */
    private void runAnalysis(UUID taskId, List<Location> locations, List<String> commands)
            throws InvalidNumberOfContentException,
            URISyntaxException,
            IOException,
            InvalidNumberOfLinesException,
            InvalidAnnotationException,
            InvalidPropertyValueException,
            InvalidRelationException, FileExtensionNotSupportedException, InterruptedException {
        BaseTerraformParser terraformParser = new BaseTerraformParser();
        TerraformDeploymentModel terraformDeploymentModel = new TerraformDeploymentModel();
        for (Location location : locations) {
            if ("file".equals(location.getUrl().getProtocol())
                    && new File(location.getUrl().toURI()).isDirectory()) {
                File directory = new File(location.getUrl().toURI());
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    String fileExtension =
                            StringUtils.getFilenameExtension(file.toURI().toURL().toString());
                    if (fileExtension != null && supportedFileExtensions.contains(fileExtension)) {
                        terraformDeploymentModel = terraformParser.parseFile(file.toURI().toURL()
                                , terraformDeploymentModel, commands);
                    }
                }
            } else {
                String fileExtension =
                        StringUtils.getFilenameExtension(location.getUrl().toString());
                if (fileExtension != null && supportedFileExtensions.contains(fileExtension)) {
                    terraformDeploymentModel = terraformParser.parseFile(location.getUrl(), terraformDeploymentModel, commands);
                }
            }
        }
        System.out.println("Parsed Terraform Model: " + terraformDeploymentModel);
        this.tadm =
                transformationService.transformInternalToTADM(taskId, this.tadm, terraformDeploymentModel);
    }

    /**
     * Clears the variables and resources set to avoid side effects between different transformation
     * processes.
     */
    private void clearVariables() {
        newEmbeddedDeploymentModelIndexes.clear();
    }
}
