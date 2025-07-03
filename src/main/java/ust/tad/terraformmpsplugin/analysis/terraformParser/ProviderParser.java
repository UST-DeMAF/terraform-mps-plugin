package ust.tad.terraformmpsplugin.analysis.terraformParser;

import com.fasterxml.jackson.databind.JsonNode;
import ust.tad.terraformmpsplugin.terraformmodel.Provider;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.util.Iterator;
import java.util.Map;

public class ProviderParser {

    /**
     * Parse Terraform Providers from the Terraform block.
     *
     * @param terraformBlock           the Terraform block as a JSONNode.
     * @param providerBlocks           the Provider blocks as a JSONNode.
     * @param terraformDeploymentModel the TerraformDeploymentModel to which to add the new
     *                                 providers.
     */
    public void parseProvider(JsonNode terraformBlock, JsonNode providerBlocks,
                              TerraformDeploymentModel terraformDeploymentModel) {
        if (terraformBlock != null) {
            parseProviderFromTerraformBlock(terraformBlock, terraformDeploymentModel);
        }
    }

    /**
     * Parse providers form the Terraform block.
     *
     * @param terraformBlock           the Terraform block as a JSONNode.
     * @param terraformDeploymentModel the TerraformDeploymentModel to which to add the new
     *                                 providers.
     */
    private void parseProviderFromTerraformBlock(JsonNode terraformBlock,
                                                 TerraformDeploymentModel terraformDeploymentModel) {
        JsonNode requiredProviders = terraformBlock.findValue("required_providers");
        for (JsonNode requiredProvider : requiredProviders) {
            Iterator<Map.Entry<String, JsonNode>> requiredProviderIter = requiredProvider.fields();
            while (requiredProviderIter.hasNext()) {
                String providerName =
                        requiredProviderIter.next().getValue().findValue("source").toString();
                if (providerName != null) {
                    terraformDeploymentModel.addProviderIfNotPresent(new Provider(providerName.replace("\"", "")));
                }
            }
        }
    }

}
