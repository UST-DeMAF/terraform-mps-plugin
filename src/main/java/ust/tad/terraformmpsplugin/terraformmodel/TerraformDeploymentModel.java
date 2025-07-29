package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TerraformDeploymentModel {

  private Set<Resource> resources = new HashSet<>();

  private Set<Variable> variables = new HashSet<>();

  private Set<Provider> providers = new HashSet<>();

  public TerraformDeploymentModel() {}

  public TerraformDeploymentModel(Set<Resource> resources, Set<Variable> variables, Set<Provider> providers) {
    this.resources = resources;
    this.variables = variables;
    this.providers = providers;
  }

  public Set<Resource> getResources() {
    return resources;
  }

  public void setResources(Set<Resource> resources) {
    this.resources = resources;
  }

  public Set<Variable> getVariables() {
    return variables;
  }

  public void setVariables(Set<Variable> variables) {
    this.variables = variables;
  }

  public Set<Provider> getProviders() {
    return providers;
  }

  public void setProviders(Set<Provider> providers) {
    this.providers = providers;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TerraformDeploymentModel that = (TerraformDeploymentModel) o;
    return Objects.equals(resources, that.resources) && Objects.equals(variables, that.variables) && Objects.equals(providers, that.providers);
  }

  @Override
  public int hashCode() {
    return Objects.hash(resources, variables, providers);
  }

  @Override
  public String toString() {
    return "TerraformDeploymentModel{"
        + "resources="
        + resources
        + ", variables="
        + variables
        + ", providers="
        + providers
        + '}';
  }

  public Set<Resource> addAllResources(Set<Resource> resourcesToAdd) {
    this.resources.addAll(resourcesToAdd);
    return this.getResources();
  }

  public Set<Variable> addVariableIfNotPresent(Variable variableToAdd) {
    if (this.getVariables().stream().noneMatch(variable ->
            variable.getIdentifier().equals(variableToAdd.getIdentifier()))) {
      this.variables.add(variableToAdd);
    }
    return this.getVariables();
  }

  public Set<Provider> addProviderIfNotPresent(Provider providerToAdd) {
    if (this.getProviders().stream().noneMatch(provider ->
            provider.getName().equals(providerToAdd.getName()))) {
      this.providers.add(providerToAdd);
    }
    return this.getProviders();
  }
}
