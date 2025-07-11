package ust.tad.terraformmpsplugin.analysis;

import org.junit.jupiter.api.Test;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.helm.EmbeddedHelmProcessor;
import ust.tad.terraformmpsplugin.analysis.terraformParser.BaseTerraformParser;
import ust.tad.terraformmpsplugin.analysis.terraformParser.FileExtensionNotSupportedException;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class EmbeddedHelmTest {

    @Test
    public void detectEmbeddedHelm() throws IOException, InterruptedException,
            FileExtensionNotSupportedException {
        URL fileLocation = new URL("file:./src/test/resources/gke-resources.tf");

        TerraformDeploymentModel terraformDeploymentModel = new TerraformDeploymentModel();

        BaseTerraformParser terraformParser = new BaseTerraformParser();
        terraformDeploymentModel = terraformParser.parseFile(fileLocation,
                terraformDeploymentModel, List.of("terraform apply -var='account_id=1234'"));

        EmbeddedHelmProcessor embeddedHelmProcessor = new EmbeddedHelmProcessor();
        embeddedHelmProcessor.processEmbeddedHelmDeploymentModel(UUID.randomUUID(), UUID.randomUUID(), terraformDeploymentModel);

        System.out.print("Parsed the Terraform Model: " + terraformDeploymentModel);
        assertFalse(terraformDeploymentModel.getResources().isEmpty());
        assertFalse(terraformDeploymentModel.getProviders().isEmpty());
    }
}
