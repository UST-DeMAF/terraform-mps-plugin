package ust.tad.terraformmpsplugin.analysis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ust.tad.terraformmpsplugin.models.tadm.TechnologyAgnosticDeploymentModel;

import java.io.IOException;

@SpringBootTest
public class TransformationServiceTest {

    @Autowired
    private TransformationService transformationService;

    @Test
    public void importMPSResultTest() throws IOException {
       TechnologyAgnosticDeploymentModel tadm = transformationService.importMPSResult();
    }
}
