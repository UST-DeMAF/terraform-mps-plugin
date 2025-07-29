package ust.tad.terraformmpsplugin.analysis.terraformParser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import ust.tad.terraformmpsplugin.terraformmodel.Argument;
import ust.tad.terraformmpsplugin.terraformmodel.Block;
import ust.tad.terraformmpsplugin.terraformmodel.Resource;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ResourceParser {

    /**
     * Parse Terraform Resources.
     * Resources are grouped by their resource type, therefore iterate of each resource type to add
     * the respective resources.
     *
     * @param resources                the resources as JSONNode
     * @param terraformDeploymentModel the TerraformDeploymentModel to which to add the new
     *                                 providers.
     */
    public void parseResources(JsonNode resources,
                               TerraformDeploymentModel terraformDeploymentModel) {
        if (resources != null && resources.isObject()) {
            ObjectNode objectNode = (ObjectNode) resources;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            while (iter.hasNext()) {
                terraformDeploymentModel.addAllResources(parseResourcesOfResourceType(iter.next()));
            }
        }
    }

    /**
     * Parse Terraform resources of a specific resource type.
     *
     * @param resourcesOfSpecificType the resources to parse.
     * @return the parsed resources.
     */
    private Set<Resource> parseResourcesOfResourceType(Map.Entry<String, JsonNode> resourcesOfSpecificType) {
        Set<Resource> resources = new HashSet<>();
        String resourceType = resourcesOfSpecificType.getKey();
        JsonNode resourcesOfSpecificTypeJson = resourcesOfSpecificType.getValue();
        Iterator<Map.Entry<String, JsonNode>> iter = resourcesOfSpecificTypeJson.fields();
        while (iter.hasNext()) {
            resources.add(parseResource(resourceType, iter.next()));
        }
        return resources;
    }

    /**
     * Parse a Terraform resource.
     * Creates a new resource and parses the contained blocks and arguments.
     *
     * @param resourceType the resource type.
     * @param resourceJson the body of the resource as JSONNode.
     * @return the parsed resource.
     */
    private Resource parseResource(String resourceType, Map.Entry<String, JsonNode> resourceJson) {
        Resource resource = new Resource();
        resource.setResourceType(resourceType);
        resource.setResourceName(resourceJson.getKey());
        Set<Argument> arguments = new HashSet<>();
        Set<Block> blocks = new HashSet<>();
        for (JsonNode objectWithFields : resourceJson.getValue()) {
            Iterator<Map.Entry<String, JsonNode>> iter = objectWithFields.fields();
            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> next = iter.next();
                String fieldIdentifier = next.getKey();
                JsonNode fieldValue = next.getValue();
                if (fieldValue.isArray() && isArrayContainingFurtherBlocks(fieldValue)) {
                    blocks.addAll(parseBlocks(fieldIdentifier, fieldValue));
                } else {
                    arguments.add(parseArgument(fieldIdentifier, fieldValue));
                }
            }
        }
        resource.setArguments(arguments);
        resource.setBlocks(blocks);
        return resource;
    }

    /**
     * Parse the blocks.
     * If the field is an array, iterate over all nodes to parse each node as a separate block.
     *
     * @param fieldIdentifier the identifier of the field with the blocks to parse.
     * @param fieldValue      the JsonNode to parse.
     * @return the Set of parsed Blocks.
     */
    private Set<Block> parseBlocks(String fieldIdentifier, JsonNode fieldValue) {
        Set<Block> blocks = new HashSet<>();
        if (fieldValue.isArray()) {
            for (JsonNode objectWithFields : fieldValue) {
                Iterator<Map.Entry<String, JsonNode>> iter = objectWithFields.fields();
                while (iter.hasNext()) {
                    parseBlock(fieldIdentifier, blocks, iter);
                }
            }
        } else {
            parseBlock(fieldIdentifier, blocks, fieldValue.fields());
        }
        return blocks;
    }

    /**
     * Parse a Terraform block.
     * Create a new block and parse all nested blocks and arguments.
     *
     * @param fieldIdentifier the block type.
     * @param blocks          the blocks of the TerraformDeploymentModel to which to add the block.
     * @param iter            an iterator over the block fields.
     */
    private void parseBlock(String fieldIdentifier, Set<Block> blocks, Iterator<Map.Entry<String,
            JsonNode>> iter) {
        Block block = new Block();
        block.setBlockType(fieldIdentifier);
        Set<Argument> arguments = new HashSet<>();
        Set<Block> nestedBlocks = new HashSet<>();

        while (iter.hasNext()) {
            Map.Entry<String, JsonNode> next = iter.next();
            String nestedFieldIdentifier = next.getKey();
            JsonNode nestedFieldValue = next.getValue();
            if ((nestedFieldValue.isArray() && isArrayContainingFurtherBlocks(nestedFieldValue))
                    || nestedFieldValue.isObject()) {
                nestedBlocks.addAll(parseBlocks(nestedFieldIdentifier, nestedFieldValue));
            } else {
                arguments.add(parseArgument(nestedFieldIdentifier, nestedFieldValue));
            }
        }
        block.setNestedBlocks(nestedBlocks);
        block.setArguments(arguments);
        blocks.add(block);
    }

    /**
     * Test if a JSONNode field of type Array contains objects that must be parsed into blocks or
     * if it contains only primitive types so that can be parsed to arguments.
     *
     * @param fieldToTest the array field to test.
     * @return if the field contains further blocks or not.
     */
    private boolean isArrayContainingFurtherBlocks(JsonNode fieldToTest) {
        for (JsonNode childField : fieldToTest) {
            if (childField.isObject()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Parse a Terraform argument.
     *
     * @param fieldIdentifier the argument identifier.
     * @param fieldValue      the JSONNode from which to parse the argument expression.
     * @return the parsed argument.
     */
    private Argument parseArgument(String fieldIdentifier, JsonNode fieldValue) {
        return new Argument(fieldIdentifier, fieldValue.toString().trim());
    }
}
