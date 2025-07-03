package ust.tad.terraformmpsplugin.analysis.terraformParser;

import com.fasterxml.jackson.databind.JsonNode;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;
import ust.tad.terraformmpsplugin.terraformmodel.Variable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VariableParser {

    /**
     * Parse Terraform Variables.
     * First parse variables specified from the given commands as they overwrite variable
     * specifications in the Terraform deployment model.
     * After that, iterate over each variable specified in the Terraform deployment model to parse
     * through the variables parameter.
     *
     * @param variables                the variables to parse as JSONNode.
     * @param terraformDeploymentModel the TerraformDeploymentModel to which to add the new
     *                                 providers.
     * @param commands                 the Terraform commands to execute the deployment model.
     */
    public void parseVariables(JsonNode variables,
                               TerraformDeploymentModel terraformDeploymentModel,
                               List<String> commands) {
        if (variables != null) {
            createVariablesFromCommands(commands, terraformDeploymentModel);
            Iterator<Map.Entry<String, JsonNode>> iter = variables.fields();
            while (iter.hasNext()) {
                terraformDeploymentModel.addVariableIfNotPresent(parseVariable(iter.next()));
            }
        }
    }

    /**
     * Parse a Terraform Variable from the file.
     * For the variable value, first check if the default value is set.
     * If not, set 'null' as variable expression.
     *
     * @param variable the variable to parse as JSONNode.
     * @return the parsed variable.
     */
    private Variable parseVariable(Map.Entry<String, JsonNode> variable) {
        Variable newVariable = new Variable();
        newVariable.setIdentifier(variable.getKey());
        JsonNode defaultValue = variable.getValue().findValue("default");
        if (defaultValue != null) {
            newVariable.setExpression(defaultValue.toString().trim());
        } else {
            newVariable.setExpression("null");
        }
        return newVariable;
    }

    /**
     * Create variables from the commands to execute the Terraform deployment model.
     * Only process terraform apply command and extract the -var arguments.
     *
     * @param commands                 the Terraform commands to execute the deployment model.
     * @param terraformDeploymentModel the TerraformDeploymentModel to which to add the new
     *                                 providers.
     */
    private void createVariablesFromCommands(List<String> commands,
                                             TerraformDeploymentModel terraformDeploymentModel) {
        if (!commands.isEmpty()) {
            for (String command : commands) {
                if (command.startsWith("terraform apply")) {
                    command = command.replaceFirst("terraform apply", "").trim();
                    String[] variableParameters = command.split("-var=");
                    for (String variableParameter : variableParameters) {
                        if (!variableParameter.isEmpty()) {
                            variableParameter = variableParameter
                                    .replaceAll("'", "")
                                    .replaceAll("\"", "")
                                    .trim();
                            terraformDeploymentModel.addVariableIfNotPresent(
                                    new Variable(variableParameter.split("=")[0],
                                            variableParameter.split("=")[1]));
                        }
                    }
                }
            }
        }

    }


}
