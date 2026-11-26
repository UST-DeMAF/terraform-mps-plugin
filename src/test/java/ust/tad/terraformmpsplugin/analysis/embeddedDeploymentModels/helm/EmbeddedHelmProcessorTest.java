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
                new Argument("name", "content-service-db"),
                new Argument("repository", "oci://registry-1.docker.io/bitnamicharts"),
                new Argument("chart", "postgresql"),
                new Argument("version", "16.7.17")),
            Set.of(
                new Block(
                    "set",
                    Set.of(),
                    Set.of(
                        new Argument("name", "global.postgresql.auth.username"),
                        new Argument("value", "gits")))));

    List<String> commands = new EmbeddedHelmProcessor().createHelmCommandsFromResource(resource);

    assertEquals(
        List.of(
            "helm install content-service-db oci://registry-1.docker.io/bitnamicharts/postgresql"
                + " --set global.postgresql.auth.username=gits --version 16.7.17"),
        commands);
  }

  @Test
  public void versionWithoutSetParameter() throws HelmCommandInformationMissingException {
    Resource resource =
        helmRelease(
            Set.of(
                new Argument("name", "dapr"),
                new Argument("repository", "https://dapr.github.io/helm-charts/"),
                new Argument("chart", "dapr"),
                new Argument("version", "1.15.6")),
            Set.of());

    List<String> commands = new EmbeddedHelmProcessor().createHelmCommandsFromResource(resource);

    assertEquals(
        List.of(
            "helm repo add dapr https://dapr.github.io/helm-charts/",
            "helm install dapr dapr/dapr --version 1.15.6"),
        commands);
  }
}
