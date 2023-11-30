package ust.tad.terraformmpsplugin.analysis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;
import ust.tad.terraformmpsplugin.analysistask.Location;
import ust.tad.terraformmpsplugin.models.ModelsService;
import ust.tad.terraformmpsplugin.models.tadm.InvalidPropertyValueException;
import ust.tad.terraformmpsplugin.models.tadm.InvalidRelationException;
import ust.tad.terraformmpsplugin.models.tadm.TechnologyAgnosticDeploymentModel;
import ust.tad.terraformmpsplugin.models.tsdm.*;
import ust.tad.terraformmpsplugin.terraformmodel.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;

@Service
public class AnalysisService {

    private static final List<String> supportedProviders = List.of("azurerm");
    @Autowired
    ModelsService modelsService;
    @Autowired
    AnalysisTaskResponseSender analysisTaskResponseSender;
    @Autowired
    private TransformationService transformationService;
    private TechnologySpecificDeploymentModel tsdm;
    private TechnologyAgnosticDeploymentModel tadm;
    private Set<Integer> newEmbeddedDeploymentModelIndexes = new HashSet<>();
    private Set<Variable> variables = new HashSet<>();

    private Set<Resource> resources = new HashSet<>();

    /**
     * Start the analysis of the deployment model.
     * 1. Retrieve internal deployment models from models service
     * 2. Parse in technology-specific deployment model from locations
     * 3. Update tsdm with new information
     * 4. Transform to EDMM entities and update tadm
     * 5. Send updated models to models service
     * 6. Send AnalysisTaskResponse or EmbeddedDeploymentModelAnalysisRequests if present
     *
     * @param taskId
     * @param transformationProcessId
     * @param commands
     * @param locations
     */
    public void startAnalysis(UUID taskId, UUID transformationProcessId, List<String> commands,
                              List<Location> locations) {
        TechnologySpecificDeploymentModel completeTsdm =
                modelsService.getTechnologySpecificDeploymentModel(transformationProcessId);
        this.tsdm = getExistingTsdm(completeTsdm, locations);
        if (tsdm == null) {
            analysisTaskResponseSender.sendFailureResponse(taskId, "No technology-specific " +
                    "deployment model found!");
            return;
        }
        this.tadm = modelsService.getTechnologyAgnosticDeploymentModel(transformationProcessId);

        try {
            runAnalysis(locations);
        } catch (InvalidNumberOfContentException | URISyntaxException | IOException |
                 InvalidNumberOfLinesException | InvalidAnnotationException |
                 InvalidPropertyValueException | InvalidRelationException e) {
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
        analysisTaskResponseSender.sendSuccessResponse(taskId);
    }

    private TechnologySpecificDeploymentModel getExistingTsdm(TechnologySpecificDeploymentModel tsdm, List<Location> locations) {
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

    private void updateDeploymentModels(TechnologySpecificDeploymentModel tsdm,
                                        TechnologyAgnosticDeploymentModel tadm) {
        modelsService.updateTechnologySpecificDeploymentModel(tsdm);
        modelsService.updateTechnologyAgnosticDeploymentModel(tadm);
    }

    /**
     * Iterate over the locations and parse in all files that can be found.
     * The file has to have the fileextension ".tf", otherwise it will be ignored.
     * If the given location is a directory, iterate over all contained files.
     * Removes the deployment model content associated with the old directory locations
     * because it has been resolved to the contained files.
     *
     * @param locations
     * @throws InvalidNumberOfContentException
     * @throws URISyntaxException
     * @throws InvalidAnnotationException
     * @throws InvalidNumberOfLinesException
     * @throws IOException
     * @throws InvalidPropertyValueException
     * @throws InvalidRelationException
     * @throws MalformedURLException
     */
    private void runAnalysis(List<Location> locations) throws InvalidNumberOfContentException,
            URISyntaxException, IOException, InvalidNumberOfLinesException,
            InvalidAnnotationException, InvalidPropertyValueException, InvalidRelationException {
        for (Location location : locations) {
            if ("file".equals(location.getUrl().getProtocol()) && new File(location.getUrl().toURI()).isDirectory()) {
                File directory = new File(location.getUrl().toURI());
                for (File file : Objects.requireNonNull(directory.listFiles())) {
                    if ("tf".equals(StringUtils.getFilenameExtension(file.toURI().toURL().toString()))) {
                        parseFile(file.toURI().toURL());
                    }
                }
                DeploymentModelContent contentToRemove = new DeploymentModelContent();
                for (DeploymentModelContent content : this.tsdm.getContent()) {
                    if (content.getLocation().equals(location.getUrl())) {
                        contentToRemove = content;
                    }
                }
                this.tsdm.removeDeploymentModelContent(contentToRemove);
            } else {
                if ("tf".equals(StringUtils.getFilenameExtension(location.getUrl().toString()))) {
                    parseFile(location.getUrl());
                }
            }
        }
        this.tadm = transformationService.transformInternalToTADM(this.tadm,
                new TerraformDeploymentModel(resources, variables));
    }

    /**
     * Parses in a file.
     * Creates entities of the Terraform model for the resources, providers and variables it can
     * find.
     * At the same time, updates the technology-specific deployment model.
     * Iterates over the lines in the file and adds corresponding Line entities to a new
     * DeploymentModelContent.
     * In the end it adds the DeploymentModelContent to the technology-specific deployment model.
     *
     * @param url
     * @throws IOException
     * @throws InvalidNumberOfLinesException
     * @throws InvalidAnnotationException
     */
    private void parseFile(URL url) throws IOException, InvalidNumberOfLinesException,
            InvalidAnnotationException {
        DeploymentModelContent deploymentModelContent = new DeploymentModelContent();
        deploymentModelContent.setLocation(url);

        List<Line> lines = new ArrayList<>();
        int lineNumber = 1;
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        while (reader.ready()) {
            String nextline = reader.readLine();
            if (nextline.startsWith("resource")) {
                Resource resource = new Resource();
                String[] lineTokens = nextline.split(" ");
                String resourceType = lineTokens[1].replaceAll("(^\")|(\"$)", "");
                resource.setResourceType(resourceType);
                String resourceName = lineTokens[2].replaceAll("(^\")|(\"$)", "");
                resource.setResourceName(resourceName);
                Line line = new Line();
                line.setNumber(lineNumber);
                line.setAnalyzed(true);
                double comprehensibility = 0D;
                //set comprehensibility based on resource type   
                String providerOfResource = resourceType.split("_")[0];
                if (supportedProviders.contains(providerOfResource)) {
                    comprehensibility = 1D;
                    line.setComprehensibility(comprehensibility);
                } else {
                    line.setComprehensibility(0.5D);
                }
                lines.add(line);
                lineNumber++;
                nextline = reader.readLine();
                while (!nextline.startsWith("}")) {
                    // Parse Argument
                    if (nextline.contains("=")) {
                        String[] tokens = nextline.split("=");
                        resource.addArgument(new Argument(tokens[0].trim(), tokens[1].trim()));
                        lines.add(new Line(lineNumber, comprehensibility, true));
                        // Parse Block
                    } else if (nextline.contains("{")) {
                        nextline = nextline.trim();
                        String[] tokens = nextline.split(" ");
                        Block block = new Block();
                        block.setBlockType(tokens[0]);
                        lines.add(new Line(lineNumber, comprehensibility, true));
                        if (!nextline.endsWith("}")) {
                            lineNumber++;
                            nextline = reader.readLine();
                            while (!nextline.trim().startsWith("}")) {
                                if (nextline.contains("=")) {
                                    String[] argumentTokens = nextline.split("=");
                                    block.addArgument(new Argument(argumentTokens[0].trim(),
                                            argumentTokens[1].trim()));
                                    lines.add(new Line(lineNumber, comprehensibility, true));
                                }// TODO else nested block?
                                lineNumber++;
                                nextline = reader.readLine();
                            }
                        }
                        resource.addBlock(block);
                    }
                    lineNumber++;
                    nextline = reader.readLine();
                }
                this.resources.add(resource);
            } else if (nextline.startsWith("variable")) {
                String identifier = nextline.split(" ")[1].replaceAll("(^\")|(\"$)", "");
                lines.add(new Line(lineNumber, 1D, true));

                String expression = "";
                lineNumber++;
                nextline = reader.readLine();
                while (!nextline.startsWith("}")) {
                    if (nextline.trim().split(" ")[0].equals("default")) {
                        String[] tokens = nextline.split("=");
                        expression = tokens[1].trim().replaceAll("(^\")|(\"$)", "");
                        lines.add(new Line(lineNumber, 1D, true));
                    } else {
                        lines.add(new Line(lineNumber, 0D, true));
                    }
                    lineNumber++;
                    nextline = reader.readLine();
                }
                // TODO check if expression overwritten by command
                this.variables.add(new Variable(identifier, expression));
            }
            lineNumber++;
        }
        reader.close();

        deploymentModelContent.setLines(lines);
        this.tsdm.addDeploymentModelContent(deploymentModelContent);
    }

}
