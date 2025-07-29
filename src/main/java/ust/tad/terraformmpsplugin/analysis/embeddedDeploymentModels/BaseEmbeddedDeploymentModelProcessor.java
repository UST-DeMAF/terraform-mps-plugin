package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels;

import ust.tad.terraformmpsplugin.terraformmodel.*;

import java.util.Optional;
import java.util.Set;

public class BaseEmbeddedDeploymentModelProcessor {

    private static String findVariableReference(TerraformDeploymentModel terraformDeploymentModel
            , String reference) {
        for (Variable variable : terraformDeploymentModel.getVariables()) {
            if (reference.substring(4).equals(variable.getIdentifier())) {
                return variable.getExpression();
            }
        }
        return reference;
    }

    private static String findReferenceInResources(TerraformDeploymentModel terraformDeploymentModel, String reference) {
        String type = reference.substring(0, reference.indexOf("."));
        String rest = reference.substring(reference.indexOf(".") + 1);
        if (rest.contains(".")) {
            String resourceName = rest.substring(0, rest.indexOf("."));
            Optional<Resource> referencedResource =
                    terraformDeploymentModel.getResources().stream().filter(resource -> resource.getResourceType().equals(type) && resource.getResourceName().equals(resourceName)).findFirst();
            if (referencedResource.isPresent()) {
                String argumentIdentifier = rest.substring(rest.indexOf(".") + 1);
                Optional<Argument> referencedArgument =
                        referencedResource.get().getArguments().stream().filter(argument -> argument.getIdentifier().equals(argumentIdentifier)).findFirst();
                if (referencedArgument.isPresent()) {
                    return referencedArgument.get().getExpression();
                } else if (argumentIdentifier.contains(".") && !referencedResource.get().getBlocks().isEmpty()) {
                    return findReferenceInBlocks(referencedResource.get().getBlocks(),
                            argumentIdentifier, reference);
                }
            }
        }
        return reference;
    }

    private static String findReferenceInBlocks(Set<Block> blocks, String identifier,
                                                String reference) {
        String blockType = identifier.substring(0, identifier.indexOf("."));
        Optional<Block> referencedBlock =
                blocks.stream().filter(block -> block.getBlockType().equals(blockType)).findFirst();
        if (referencedBlock.isPresent()) {
            String argumentIdentifier = identifier.substring(identifier.indexOf(".") + 1);
            Optional<Argument> referencedArgument =
                    referencedBlock.get().getArguments().stream().filter(argument -> argument.getIdentifier().equals(argumentIdentifier)).findFirst();
            if (referencedArgument.isPresent()) {
                return referencedArgument.get().getExpression();
            } else if (argumentIdentifier.contains(".") && !referencedBlock.get().getNestedBlocks().isEmpty()) {
                return findReferenceInBlocks(referencedBlock.get().getNestedBlocks(),
                        argumentIdentifier, reference);
            } else {
                return reference;
            }
        } else {
            return reference;
        }
    }

    private static String removeEnclosingTicks(String stringToTest) {
        return stringToTest.replaceAll("\"$", "").replaceAll("^\"", "");
    }

    protected void resolveReferences(TerraformDeploymentModel terraformDeploymentModel) {
        for (Resource resource : terraformDeploymentModel.getResources()) {
            for (Argument argument : resource.getArguments()) {
                argument.setExpression(resolveReferenceInArgument(argument,
                        terraformDeploymentModel));
            }
            for (Block block : resource.getBlocks()) {
                resolveReferencesInBlocks(block, terraformDeploymentModel);
            }
        }
    }

    private void resolveReferencesInBlocks(Block block,
                                           TerraformDeploymentModel terraformDeploymentModel) {
        for (Argument argument : block.getArguments()) {
            argument.setExpression(resolveReferenceInArgument(argument, terraformDeploymentModel));
        }
        for (Block nestedBlock : block.getNestedBlocks()) {
            resolveReferencesInBlocks(nestedBlock, terraformDeploymentModel);
        }
    }

    private String resolveReferenceInArgument(Argument argument,
                                              TerraformDeploymentModel terraformDeploymentModel) {
        String expression =
                removeEnclosingTicks(argument.getExpression().trim().replaceAll("\\s" + "{2,}",
                        "").replace("\n", ""));
        if (expression.startsWith("${") && expression.endsWith("}")) {
            expression = expression.replace("${", "").replace("}", "");
        }
        if (expression.startsWith("var.")) {
            return findVariableReference(terraformDeploymentModel, expression);
        } else if (argument.getIdentifier().equals("depends_on")) {
            return expression.replace("\"${", "").replace("}\"", "").replaceAll("[a-zA-Z_]+\\.",
                    "").trim();
        } else if (expression.contains(".")) {
            if (expression.startsWith("random_") && expression.endsWith(".result")) {
                return "randomGeneratedValue";
            }
            String referencedExpression = findReferenceInResources(terraformDeploymentModel,
                    expression);
            if (referencedExpression.startsWith("var.")) {
                return findVariableReference(terraformDeploymentModel, expression);
            } else {
                return referencedExpression;
            }
        }
        return expression;
    }
}
