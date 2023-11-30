package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.models.tadm.*;

import java.util.List;
import java.util.Optional;

import static ust.tad.terraformmpsplugin.analysis.terraformproviders.AzureRMComponentTypeProvider.*;

@Service
public class AzureRMPostProcessor {
    private final String PROVIDERNAME = "azurerm";

    /**
     * Tests if this Post-Processor is applicable for the given technology-agnostic deployment
     * model.
     *
     * @param tadm the technology-agnostic deployment model to test.
     * @return true if applicable, false otherwise.
     */
    public boolean isPostProcessorApplicable(TechnologyAgnosticDeploymentModel tadm) {
        for (ComponentType componentType : tadm.getComponentTypes()) {
            if (componentType.getName().startsWith(PROVIDERNAME)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runs the post-processing steps for this provider.
     *
     * @param tadm the technology-agnostic deployment model to post-process.
     * @return the modified technology-agnostic deployment model.
     * @throws PostProcessorFailedException if the post-processing fails.
     */
    public TechnologyAgnosticDeploymentModel runPostProcessor(TechnologyAgnosticDeploymentModel tadm) throws PostProcessorFailedException {
        for (Component component : tadm.getComponents()) {
            if (component.getType().getName().equals("azurerm_kubernetes_cluster")) {
                try {
                    replaceAKS(tadm, component);
                } catch (Exception e) {
                    throw new PostProcessorFailedException("The AzureRM Post Processor failed " + "with reason: " + e.getMessage());
                }
            }
        }
        return tadm;
    }

    /**
     * Replaces EDMM components of type AzureRM Kubernetes Cluster with generic components
     * describing physical nodes, operating systems, and container runtimes for each Kubernetes
     * cluster node configured.
     * For each component a corresponding component type is created.
     * New relations between the components are created and existing relations to the aksComponent
     * are redirected to the newly created container runtime component.
     *
     * @param tadm         the technology-agnostic deployment model to modify
     * @param aksComponent the AzureRM Kubernetes Cluster to replace
     * @throws InvalidRelationException      if a new relation has invalid source or target
     *                                       components
     *                                       configured
     * @throws InvalidPropertyValueException if a new property is invalid
     */
    private void replaceAKS(TechnologyAgnosticDeploymentModel tadm, Component aksComponent) throws InvalidRelationException, InvalidPropertyValueException {
        ComponentType physicalNodeType = AzureRMComponentTypeProvider.createPhysicalNodeType();
        ComponentType operatingSystemType =
                AzureRMComponentTypeProvider.createOperatingSystemType();
        ComponentType containerRuntimeType =
                AzureRMComponentTypeProvider.createContainerRuntimeType();
        List<ComponentType> types = tadm.getComponentTypes();
        types.add(physicalNodeType);
        types.add(operatingSystemType);
        types.add(containerRuntimeType);

        int nodeCount = calculateNodeCount(aksComponent);
        //iterate over nodes and create components
        for (int i = 0; i < nodeCount; i++) {
            Component physicalNodeComponent = new Component();
            physicalNodeComponent.setName(aksComponent.getName().concat("_node_").concat(String.valueOf(nodeCount++)));
            physicalNodeComponent.setType(physicalNodeType);
            physicalNodeComponent.setConfidence(Confidence.CONFIRMED);
            Optional<Property> vmSizeProperty =
                    aksComponent.getProperties().stream().filter(property -> property.getKey().contains("vm_size")).findFirst();
            if (vmSizeProperty.isPresent()) {
                physicalNodeComponent.setProperties(getPropertiesForVMSize(String.valueOf(vmSizeProperty.get().getValue())));
            }

            Component operatingSystemComponent = new Component();
            operatingSystemComponent.setName(aksComponent.getName().concat("_operating-system_").concat(String.valueOf(nodeCount++)));
            operatingSystemComponent.setType(operatingSystemType);
            operatingSystemComponent.setConfidence(Confidence.CONFIRMED);
            operatingSystemComponent.setProperties(createPropertiesForDefaultOperatingSystem());

            Component containerRuntimeComponent = new Component();
            containerRuntimeComponent.setName(aksComponent.getName().concat("_container-runtime_").concat(String.valueOf(nodeCount++)));
            containerRuntimeComponent.setType(containerRuntimeType);
            containerRuntimeComponent.setConfidence(Confidence.CONFIRMED);
            containerRuntimeComponent.setProperties(createPropertiesForDefaultContainerRuntime());

            List<Component> components = tadm.getComponents();
            components.add(physicalNodeComponent);
            components.add(operatingSystemComponent);
            components.add(containerRuntimeComponent);

            RelationType hostedOnRelationType = new RelationType();
            Optional<RelationType> hostedOnRelationTypeOpt =
                    tadm.getRelationTypes().stream().filter(relationType -> relationType.getName().equals("HostedOn")).findFirst();
            if (hostedOnRelationTypeOpt.isPresent()) {
                hostedOnRelationType = hostedOnRelationTypeOpt.get();
            }

            Relation osOnNodeRelation = new Relation();
            osOnNodeRelation.setName(operatingSystemComponent.getName() + "_" + hostedOnRelationType.getName() + "_" + physicalNodeComponent.getName());
            osOnNodeRelation.setType(hostedOnRelationType);
            osOnNodeRelation.setSource(operatingSystemComponent);
            osOnNodeRelation.setTarget(physicalNodeComponent);
            osOnNodeRelation.setConfidence(Confidence.CONFIRMED);

            Relation crOnOSRelation = new Relation();
            crOnOSRelation.setName(containerRuntimeComponent.getName() + "_" + hostedOnRelationType.getName() + "_" + operatingSystemComponent.getName());
            crOnOSRelation.setType(hostedOnRelationType);
            crOnOSRelation.setSource(containerRuntimeComponent);
            crOnOSRelation.setTarget(operatingSystemComponent);
            crOnOSRelation.setConfidence(Confidence.CONFIRMED);

            List<Relation> relations = tadm.getRelations();
            relations.add(osOnNodeRelation);
            relations.add(crOnOSRelation);

            for (Relation relation : tadm.getRelations()) {
                if (relation.getSource().equals(aksComponent)) {
                    relations.remove(relation);
                    relation.setSource(containerRuntimeComponent);
                    relations.add(relation);
                } else if (relation.getTarget().equals(aksComponent)) {
                    relations.remove(relation);
                    relation.setTarget(containerRuntimeComponent);
                    relations.add(relation);
                }
            }

            types.remove(aksComponent.getType());
            components.remove(aksComponent);
            tadm.setComponentTypes(types);
            tadm.setComponents(components);
            tadm.setRelations(relations);
        }

    }

    /**
     * Calculates how many nodes are configured for a given AKS Kubernetes Cluster.
     *
     * @param aksComponent a component that describes an AKS Kubernetes Cluster.
     * @return the number of nodes.
     */
    private int calculateNodeCount(Component aksComponent) {
        int nodeCount = 0;
        for (Property property : aksComponent.getProperties()) {
            if (property.getKey().contains("node_count") && property.getType() != PropertyType.BOOLEAN) {
                nodeCount += Integer.parseInt(property.getValue().toString());
            }
        }
        return nodeCount;
    }

}
