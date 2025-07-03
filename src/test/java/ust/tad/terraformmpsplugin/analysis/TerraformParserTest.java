package ust.tad.terraformmpsplugin.analysis;

import org.junit.jupiter.api.Test;
import ust.tad.terraformmpsplugin.analysis.terraformParser.BaseTerraformParser;
import ust.tad.terraformmpsplugin.analysis.terraformParser.FileExtensionNotSupportedException;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

public class TerraformParserTest {

    @Test
    public void parseTerraform() throws IOException, InterruptedException,
            FileExtensionNotSupportedException {
        URL fileLocation = new URL("file:./src/test/resources/gke-resources.tf");

        TerraformDeploymentModel terraformDeploymentModel = new TerraformDeploymentModel();

        BaseTerraformParser terraformParser = new BaseTerraformParser();
        terraformDeploymentModel = terraformParser.parseFile(fileLocation,
                terraformDeploymentModel, List.of("terraform apply -var='account_id=1234'"));

        System.out.print("Parsed the Terraform Model: " + terraformDeploymentModel);
        assertFalse(terraformDeploymentModel.getResources().isEmpty());
        assertFalse(terraformDeploymentModel.getProviders().isEmpty());
    }

}
