# terraform-mps-plugin
The terraform-mps-plugin is one of many plugins of the [DeMAF](https://github.com/UST-DeMAF) project.
It is designed to transform [Terraform deployment models](https://developer.hashicorp.com/terraform/docs) into an [EDMM](https://github.com/UST-EDMM) representation.
OpenTofu templates should work as well.

The plugin only works (without adaptions) in the context of the entire DeMAF application using the [deployment-config](https://github.com/UST-DeMAF/deployment-config).
The documentation for setting up the entire DeMAF application locally is [here](https://github.com/UST-DeMAF/EnPro-Documentation).

## Build and Run Application

You can run the application without the [deployment-config](https://github.com/UST-DeMAF/deployment-config), but it will not run as it needs to register itself at the [analysis-manager](https://github.com/UST-DeMAF/analysis-manager).

If you want to boot it locally nevertheless use the following commands.

```shell
./mvnw spring-boot:run
```
or to use the built package:
```shell
./mvnw package
java -jar target/terraform-plugin-0.0.1-SNAPSHOT.jar
```

## Init and Update Submodule
This plugin uses [JetBrains MPS](https://www.jetbrains.com/mps/) to facilitate the model-to-model transformation from Terraform to EDMM.
The [matching MPS project](https://github.com/UST-DeMAF/mps-transformation-terraform) is located in another git repository and must be added as a submodule (you can also clone via https):

```shell
git submodule add git@github.com:UST-DeMAF/mps-transformation-terraform.git mps-transformation-terraform
```

To update the MPS application to a new version, execute:
```shell
git submodule update --remote
```

## Terraform-Specific Configurations
This plugin has some minor specialities compared to other DeMAF transformation plugins:
1. Post-Processing Component-Types: The EDMM component types generated in the MPS transformation are unique and matching to each resource.
However, they are post-processed and merged into a single representation afterward, if possible.
2. Ontological Post-Processors: The transformation of MPS is not the final result, as there are several post-processors dealing with 
ontological parts of the transformation. Each post-processor scans the yaml file for specific resource-types
and refines the output if it matches.

## Debugging
When running the project locally using e.g. IntelliJ IDEA or from the command-line, make sure that the plugin is not also running
in a Docker container, launched by the deployment-config, otherwise the port is blocked.
