package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.helm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.BaseEmbeddedDeploymentModelProcessor;
import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;
import ust.tad.terraformmpsplugin.models.tsdm.InvalidNumberOfLinesException;
import ust.tad.terraformmpsplugin.models.tsdm.TechnologySpecificDeploymentModel;
import ust.tad.terraformmpsplugin.terraformmodel.Argument;
import ust.tad.terraformmpsplugin.terraformmodel.Block;
import ust.tad.terraformmpsplugin.terraformmodel.Resource;
import ust.tad.terraformmpsplugin.terraformmodel.TerraformDeploymentModel;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmbeddedHelmProcessor extends BaseEmbeddedDeploymentModelProcessor {

    @Autowired
    private AnalysisTaskResponseSender analysisTaskResponseSender;

    public void processEmbeddedHelmDeploymentModel(UUID taskId, UUID transformationProcessId,
                                                   TerraformDeploymentModel terraformDeploymentModel) {
        List<Resource> helmResources =
                terraformDeploymentModel.getResources().stream().filter(resource -> resource.getResourceType().equals("helm_release")).collect(Collectors.toList());
        if (!helmResources.isEmpty()) {
            resolveReferences(terraformDeploymentModel);
            for (Resource helmResource : helmResources) {
                try {
                    analysisTaskResponseSender.sendEmbeddedDeploymentModelAnalysisRequestFromModel(
                            createEmbeddedDeploymentModelToAnalyze(transformationProcessId,
                                    helmResource), taskId);
                } catch (HelmCommandInformationMissingException | MalformedURLException |
                         InvalidNumberOfLinesException ignored) {
                }
            }
        }
    }

    private TechnologySpecificDeploymentModel createEmbeddedDeploymentModelToAnalyze(
            UUID transformationProcessId, Resource helmResource)
            throws HelmCommandInformationMissingException, MalformedURLException, InvalidNumberOfLinesException {
        TechnologySpecificDeploymentModel embeddedDeploymentModel =
                new TechnologySpecificDeploymentModel();
        embeddedDeploymentModel.setTransformationProcessId(
                transformationProcessId);
        embeddedDeploymentModel.setTechnology("helm");
        embeddedDeploymentModel.setCommands(createHelmCommandsFromResource(helmResource));
        return embeddedDeploymentModel;
    }

    private List<String> createHelmCommandsFromResource(Resource helmResource) throws HelmCommandInformationMissingException {
        List<String> commands = new ArrayList<>();
        String release = "";
        Optional<Argument> releaseArg =
                helmResource.getArguments().stream().filter(argument -> argument.getIdentifier().equals("name")).findFirst();
        if (releaseArg.isPresent()) {
            release = releaseArg.get().getExpression();
        } else {
            throw new HelmCommandInformationMissingException("The Helm release does not provide a" +
                    " release name");
        }

        String chart = "";
        Optional<Argument> chartArg =
                helmResource.getArguments().stream().filter(argument -> argument.getIdentifier().equals("chart")).findFirst();
        if (chartArg.isPresent()) {
            chart = chartArg.get().getExpression();
        } else {
            throw new HelmCommandInformationMissingException("The Helm release does not provide a" +
                    " chart name");
        }

        String repo = "";
        Optional<Argument> repoArg =
                helmResource.getArguments().stream().filter(argument -> argument.getIdentifier().equals("repository")).findFirst();
        if (repoArg.isPresent()) {
            repo = repoArg.get().getExpression();
        }

        if (repo.startsWith("http")) {
            String repoCommand = "helm repo add " + chart + " " + repo;
            commands.add(repoCommand);
            repo = chart;
        }

        StringBuilder command = new StringBuilder("helm install " + release + " " + repo);
        if (!repo.endsWith("/")) {
            command.append("/");
        }
        command.append(chart);

        for (Block setBlock :
                helmResource.getBlocks().stream().filter(block -> block.getBlockType().equals(
                        "set")).collect(Collectors.toList())) {
            command.append(" ").append(createHelmSetParameter(setBlock));
        }

        Optional<Argument> versionArg =
                helmResource.getArguments().stream().filter(argument -> argument.getIdentifier().equals("version")).findFirst();
        if (versionArg.isPresent()) {
            String version = versionArg.get().getExpression();
            command.append("--version ").append(version);
        }
        commands.add(command.toString());
        return commands;
    }

    private String createHelmSetParameter(Block setBlock) throws HelmCommandInformationMissingException {
        String key = "";
        Optional<Argument> keyArg =
                setBlock.getArguments().stream().filter(argument -> argument.getIdentifier().equals("name")).findFirst();
        if (keyArg.isPresent()) {
            key = keyArg.get().getExpression();
        } else {
            throw new HelmCommandInformationMissingException("The Set block does not provide a " +
                    "key");
        }

        String value = "";
        Optional<Argument> valueArg =
                setBlock.getArguments().stream().filter(argument -> argument.getIdentifier().equals("value")).findFirst();
        if (valueArg.isPresent()) {
            value = valueArg.get().getExpression();
        } else {
            throw new HelmCommandInformationMissingException("The Set block does not provide a " +
                    "value");
        }

        if (key.startsWith("global.imagePullSecrets")) {
            key = "global.security.allowInsecureImages";
            value = "true";
        }

        return "--set " + key + "=" + value;
    }

}
