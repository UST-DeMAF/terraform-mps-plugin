package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.helm;

public class HelmCommandInformationMissingException extends Exception{
    public HelmCommandInformationMissingException(String errorMessage) { super(errorMessage); }
}
