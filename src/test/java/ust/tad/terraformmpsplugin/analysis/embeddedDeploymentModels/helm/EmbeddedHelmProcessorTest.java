package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.helm;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import ust.tad.terraformmpsplugin.terraformmodel.Argument;
import ust.tad.terraformmpsplugin.terraformmodel.Block;
import ust.tad.terraformmpsplugin.terraformmodel.Resource;

public class EmbeddedHelmProcessorTest {

  private static Resource helmRelease(Set<Argument> arguments, Set<Block> blocks) {
    Resource resource = new Resource();
    resource.setResourceType("helm_release");
    resource.setResourceName("release");
    resource.setArguments(arguments);
    resource.setBlocks(blocks);
    return resource;
  }

  @Test
  public void versionAfterSetParameter() throws HelmCommandInformationMissingException {
    Resource resource =
        helmRelease(
            Set.of(
                new Argument("name", "my-release"),
                new Argument("repository", "oci://registry.example.com/charts"),
                new Argument("chart", "my-chart"),
                new Argument("version", "1.2.3")),
            Set.of(
                new Block(
                    "set",
                    Set.of(),
                    Set.of(
                        new Argument("name", "replicaCount"),
                        new Argument("value", "2")))));

    List<String> commands = new EmbeddedHelmProcessor().createHelmCommandsFromResource(resource);

    assertEquals(
        List.of(
            "helm install my-release oci://registry.example.com/charts/my-chart --set replicaCount=2"
                + " --version 1.2.3"),
        commands);
  }

  @Test
  public void versionWithoutSetParameter() throws HelmCommandInformationMissingException {
    Resource resource =
        helmRelease(
            Set.of(
                new Argument("name", "other-release"),
                new Argument("repository", "https://charts.example.com/"),
                new Argument("chart", "other-chart"),
                new Argument("version", "4.5.6")),
            Set.of());

    List<String> commands = new EmbeddedHelmProcessor().createHelmCommandsFromResource(resource);

    assertEquals(
        List.of(
            "helm repo add other-chart https://charts.example.com/",
            "helm install other-release other-chart/other-chart --version 4.5.6"),
        commands);
  }
}
