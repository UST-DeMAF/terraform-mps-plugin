package ust.tad.terraformmpsplugin.analysis;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import ust.tad.terraformmpsplugin.terraformmodel.*;

@SpringBootTest
public class SerializeXMLTest {

  @Value("${mps.inputModel.path}")
  private String mpsInputPath;

  @Test
  public void serializeTerraformToXML() throws JsonProcessingException {
    TerraformDeploymentModel modelToSerialize = createDummyModel();

    XmlMapper xmlMapper = new XmlMapper();
    String xml = xmlMapper.writeValueAsString(modelToSerialize);
    assertNotNull(xml);
    System.out.print(xml);
  }

  @Test
  public void serializeTerraformToXMLFile() throws IOException {
    TerraformDeploymentModel modelToSerialize = createDummyModel();

    XmlMapper xmlMapper = new XmlMapper();
    xmlMapper.writeValue(new File("dummyTerraformDM.xml"), modelToSerialize);
    File file = new File("dummyTerraformDM.xml");
    assertNotNull(file);
  }

  private TerraformDeploymentModel createDummyModel() {
    Argument argument = new Argument("key", "val");
    Argument argument2 = new Argument("key2", "val2");
    Argument argument3 = new Argument("key3", "val3");
    Argument argumentFromVariable = new Argument("keyVar", "var.key");
    Block block = new Block("newBlockType", Set.of(argument2, argument3));

    Resource resource =
        new Resource("newResourceType", "newResource", Set.of(argument), Set.of(block));
    Resource resource2 =
        new Resource("newResource2Type", "newResource2", Set.of(argumentFromVariable), Set.of());

    Variable variable = new Variable("var.key", "variableValue");

    Provider provider = new Provider("dummyProvider");

    return new TerraformDeploymentModel(Set.of(resource, resource2), Set.of(variable), Set.of(provider));
  }
}
