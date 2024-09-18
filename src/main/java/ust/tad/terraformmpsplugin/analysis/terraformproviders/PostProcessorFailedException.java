package ust.tad.terraformmpsplugin.analysis.terraformproviders;

public class PostProcessorFailedException extends Exception {
  public PostProcessorFailedException(String errorMessage) {
    super(errorMessage);
  }
}
