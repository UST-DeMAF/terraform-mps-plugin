package ust.tad.terraformmpsplugin.analysis.terraformParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;
import java.util.Set;

public class BaseTerraformParser {

    private static final Set<String> supportedJSONFileExtensions = Set.of("tf.json", "tofu.json");
    private static final Set<String> supportedNonJSONFileExtensions = Set.of("tf", "tofu");

    /**
     * Parse a Terraform file.
     * First, parse the file to a JSON Node object and then parse the JSON into Provider, Resource,
     * and Variable and add them to the TerraformDeploymentModel object.
     *
     * @param fileToParse              the file to parse.
     * @param terraformDeploymentModel the TerraformDeploymentModel to which to add the new objects.
     * @param commands                 the Terraform commands to execute the deployment model.
     * @return The TerraformDeploymentModel with the new parsed information.
     * @throws IOException                        if an I/O error occurred.
     * @throws FileExtensionNotSupportedException if the type of file to parse is not supported.
     * @throws InterruptedException               if the file import is interrupted.
     */
    public TerraformDeploymentModel parseFile(URL fileToParse,
                                              TerraformDeploymentModel terraformDeploymentModel,
                                              List<String> commands)
            throws IOException, FileExtensionNotSupportedException, InterruptedException {
        JsonNode jsonNode = parseFileToJsonNode(fileToParse);
        new ProviderParser().parseProvider(jsonNode.get("terraform"), jsonNode.get("provider"),
                terraformDeploymentModel);
        new ResourceParser().parseResources(jsonNode.get("resource"), terraformDeploymentModel);
        new VariableParser().parseVariables(jsonNode.get("variable"), terraformDeploymentModel,
                commands);
        return terraformDeploymentModel;
    }

    /**
     * Parse the given file to JSONNode representation.
     * Depending on the file extension, first converts the file into JSON format.
     *
     * @param fileToParse the file to parse.
     * @return the parsed JSONNode.
     * @throws IOException                        if an I/O error occurred.
     * @throws InterruptedException               if the file import is interrupted.
     * @throws FileExtensionNotSupportedException if the type of file to parse is not supported.
     */
    private JsonNode parseFileToJsonNode(URL fileToParse) throws IOException,
            InterruptedException, FileExtensionNotSupportedException {
        JsonNode jsonNode;
        if (supportedJSONFileExtensions.stream().anyMatch(fileToParse.toString()::endsWith)) {
            jsonNode = importJSONNodeFromJSONFile(fileToParse);
        } else if (supportedNonJSONFileExtensions.stream().anyMatch(fileToParse.toString()::endsWith)) {
            jsonNode = new HCL2JSONExecutor().executeHCL2JSONCommand(fileToParse.getFile());
        } else {
            throw new FileExtensionNotSupportedException("The file extension " +
                    StringUtils.getFilenameExtension(fileToParse.toString()) + " is not supported" +
                    ".");
        }
        return jsonNode;
    }

    /**
     * Parse a JSON file to JSONNode representation.
     *
     * @param file the file to parse.
     * @return the parsed JSONNode.
     * @throws IOException if an I/O error occurred.
     */
    private JsonNode importJSONNodeFromJSONFile(URL file) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(file.openStream()));
        return new ObjectMapper().readTree(reader);
    }

}
