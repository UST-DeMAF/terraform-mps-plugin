package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor;

public class BaseExtractor {

    static String removeEnclosingTicks(String stringToTest) {
        return stringToTest.replaceAll("\"$", "").replaceAll("^\"", "");
    }
}
