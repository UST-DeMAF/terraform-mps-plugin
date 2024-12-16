package ust.tad.terraformmpsplugin.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
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

@Service
public class AnalysisService {

  private static final List<String> supportedProviders = List.of("azurerm");
  @Autowired ModelsService modelsService;
  @Autowired AnalysisTaskResponseSender analysisTaskResponseSender;
  @Autowired private TransformationService transformationService;
  private TechnologySpecificDeploymentModel tsdm;
  private TechnologyAgnosticDeploymentModel tadm;
  private Set<Integer> newEmbeddedDeploymentModelIndexes = new HashSet<>();
  private Set<Variable> variables = new HashSet<>();
  private Set<Resource> resources = new HashSet<>();
  private Set<Provider> providers = new HashSet<>();

  /**
   * Start the analysis of the deployment model. 1. Retrieve internal deployment models from models
   * service 2. Parse in technology-specific deployment model from locations 3. Update tsdm with new
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
      runAnalysis(taskId, locations);
    } catch (InvalidNumberOfContentException
        | URISyntaxException
        | IOException
        | InvalidNumberOfLinesException
        | InvalidAnnotationException
        | InvalidPropertyValueException
        | InvalidRelationException e) {
      e.printStackTrace();
      analysisTaskResponseSender.sendFailureResponse(taskId, e.getClass() + ": " + e.getMessage());
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
   * iterate over all contained files. Removes the deployment model content associated with the old
   * directory locations because it has been resolved to the contained files.
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
  private void runAnalysis(UUID taskId, List<Location> locations)
      throws InvalidNumberOfContentException,
          URISyntaxException,
          IOException,
          InvalidNumberOfLinesException,
          InvalidAnnotationException,
          InvalidPropertyValueException,
          InvalidRelationException {
    for (Location location : locations) {
      if ("file".equals(location.getUrl().getProtocol())
          && new File(location.getUrl().toURI()).isDirectory()) {
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
    this.tadm =
        transformationService.transformInternalToTADM(taskId,
            this.tadm, new TerraformDeploymentModel(resources, variables, providers));
  }

  /**
   * Parses in a file. Creates entities of the Terraform model for the resources and variables it
   * can find. At the same time, updates the technology-specific deployment model. Iterates over the
   * lines in the file and adds corresponding Line entities to a new DeploymentModelContent. In the
   * end it adds the DeploymentModelContent to the technology-specific deployment model.
   *
   * @param url
   * @throws IOException
   * @throws InvalidNumberOfLinesException
   * @throws InvalidAnnotationException
   */
  private void parseFile(URL url)
      throws IOException, InvalidNumberOfLinesException, InvalidAnnotationException {
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
        // set comprehensibility based on resource type
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
            resource.addArguments(
                parseArgument(reader, nextline, lines, lineNumber, comprehensibility, ""));
            lineNumber =
                lines.stream().max(Comparator.comparing(Line::getNumber)).get().getNumber();
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
                  block.addArguments(
                      parseArgument(reader, nextline, lines, lineNumber, comprehensibility, ""));
                  lineNumber =
                      lines.stream().max(Comparator.comparing(Line::getNumber)).get().getNumber();
                } // TODO else nested block?
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
        lineNumber++;
        nextline = reader.readLine();
        while (!nextline.startsWith("}")) {
          // TODO check if expression overwritten by command
          if (nextline.trim().split(" ")[0].equals("default")) {
            Set<Argument> arguments =
                parseArgument(reader, nextline, lines, lineNumber, 1D, identifier);
            lineNumber =
                lines.stream().max(Comparator.comparing(Line::getNumber)).get().getNumber();
            for (Argument argument : arguments) {
              this.variables.add(new Variable(argument.getIdentifier(), argument.getExpression()));
            }
          } else {
            lines.add(new Line(lineNumber, 0D, true));
          }
          lineNumber++;
          nextline = reader.readLine();
        }
      } else if (nextline.startsWith("terraform")) {
        lines.add(new Line(lineNumber, 1D, true));
          lineNumber++;
        nextline = reader.readLine();
        while (!nextline.startsWith("}")) {
          if (nextline.trim().split(" ")[0].equals("source")) {
            this.providers.add(new Provider(nextline.split("=")[1].trim().replaceAll("(^\")|(\"$)", "")));
            lines.add(new Line(lineNumber, 1D, true));
          } else {
            lines.add(new Line(lineNumber, 0D, true));
          }
          lineNumber++;
          nextline = reader.readLine();
        }
      }
      lineNumber++;
    }
    reader.close();

    deploymentModelContent.setLines(lines);
    this.tsdm.addDeploymentModelContent(deploymentModelContent);
  }

  /**
   * Parse arguments from a Terraform file. Argument expressions can be primitive types, but also
   * maps or lists. This method tests which argument expression type is used and parses in the
   * argument accordingly. Lists and maps can span across several lines, therefore read in all
   * relevant lines. For lists, one argument is created with the argument expression set to the
   * complete list. For maps, one argument for each key-value pair in the map is created.
   *
   * @param reader the BufferedReader that reads the file.
   * @param currentLine the line in the file that is currently read.
   * @param lines the lines of the internal tsdm.
   * @param lineNumber the number of the line the reader is currently at.
   * @param comprehensibility that should be set for the parsed arguments.
   * @param identifier an optional identifier for the argument.
   * @return a set of parsed arguments.
   * @throws IOException
   * @throws InvalidAnnotationException
   */
  private Set<Argument> parseArgument(
      BufferedReader reader,
      String currentLine,
      List<Line> lines,
      int lineNumber,
      double comprehensibility,
      String identifier)
      throws IOException, InvalidAnnotationException {
    Set<Argument> arguments = new HashSet<>();
    String[] tokens = currentLine.split("=", 2);
    String argumentIdentifier = tokens[0].trim();
    if (!identifier.isEmpty()) {
      argumentIdentifier = identifier;
    }
    String argumentExpression = tokens[1].trim();
    // list: comma seperated in same line or in different lines, comma after the final value is
    // allowed, but not required
    if (argumentExpression.startsWith("[")) {
      List<String> listElements =
          Arrays.stream(argumentExpression.split(","))
              .map(String::trim)
              .collect(Collectors.toList());
      lines.add(new Line(lineNumber, comprehensibility, true));
      while (!(listElements.get(listElements.size() - 1)).trim().endsWith("]")) {
        currentLine = reader.readLine();
        lineNumber++;
        listElements.addAll(
            Arrays.stream(currentLine.split(",")).map(String::trim).collect(Collectors.toList()));
        lines.add(new Line(lineNumber, comprehensibility, true));
      }
      arguments.add(
          new Argument(
              argumentIdentifier,
              listElements.toString().replaceFirst("\\[\\[", "[").replaceFirst(", ,*]]", "]")));
      // map Key/value pairs can be separated by either a comma or a line break.
      // The values in a map can be arbitrary expressions.
    } else if (argumentExpression.startsWith("{")) {
      String argumentExpressionLine = argumentExpression.replaceFirst("\\{", "");
      while (!argumentExpressionLine.contains("}")) {
        if (argumentExpressionLine.contains("=")) {
          List<String> listOfArguments =
              Arrays.stream(argumentExpressionLine.split(","))
                  .map(String::trim)
                  .collect(Collectors.toList());
          for (String argumentString : listOfArguments) {
            String[] argumentFields = argumentString.split("=");
            arguments.add(
                new Argument(
                    argumentIdentifier.concat(".").concat(argumentFields[0].trim()),
                    argumentFields[1].trim()));
          }
          lines.add(new Line(lineNumber, comprehensibility, true));
        }
        argumentExpressionLine = reader.readLine();
        lineNumber++;
      }
      if (argumentExpressionLine.contains("=")) {
        argumentExpressionLine = argumentExpressionLine.replace("}", "");
        List<String> listOfArguments =
            Arrays.stream(argumentExpressionLine.split(","))
                .map(String::trim)
                .collect(Collectors.toList());
        for (String argumentString : listOfArguments) {
          String[] argumentFields = argumentString.split("=");
          arguments.add(
              new Argument(
                  argumentIdentifier.concat(".").concat(argumentFields[0].trim()),
                  argumentFields[1].trim()));
        }
        lines.add(new Line(lineNumber, comprehensibility, true));
      }
      // primitive types
    } else {
      arguments.add(new Argument(argumentIdentifier, argumentExpression));
      lines.add(new Line(lineNumber, comprehensibility, true));
    }
    return arguments;
  }

  /**
   * Clears the variables and resources set to avoid side effects between different transformation
   * processes.
   */
  private void clearVariables() {
    resources.clear();
    variables.clear();
  }
}
