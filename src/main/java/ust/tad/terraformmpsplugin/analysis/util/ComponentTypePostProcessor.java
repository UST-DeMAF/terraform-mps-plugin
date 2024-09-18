package ust.tad.terraformmpsplugin.analysis.util;

import java.util.*;
import org.springframework.stereotype.Service;
import ust.tad.terraformmpsplugin.models.tadm.*;

/**
 * Utility class for post-processing and merging duplicate ComponentTypes in the
 * TechnologyAgnosticDeploymentModel.
 */
@Service
public class ComponentTypePostProcessor {

  /**
   * Merges duplicate ComponentTypes in the given TechnologyAgnosticDeploymentModel.
   *
   * @param tadm the TechnologyAgnosticDeploymentModel to process, i.e. the result from the MPS
   *     transformation
   * @return the processed TechnologyAgnosticDeploymentModel with merged ComponentTypes
   */
  public static TechnologyAgnosticDeploymentModel mergeDuplicateComponentTypes(
      TechnologyAgnosticDeploymentModel tadm) {

    // Step 1: Recreate original ComponentType names by removing the ComponentName from them
    Map<Component, ComponentType> toBeRenamed = findComponentsToRename(tadm);

    if (!toBeRenamed.isEmpty()) {
      updateModelAfterRenaming(tadm, toBeRenamed);
    }

    // Step 2: Merge ComponentTypes with the same name
    mergeComponentTypesByName(tadm);

    return tadm;
  }

  /**
   * Finds the components that need renaming by comparing ComponentType and Component names.
   *
   * @param tadm the TechnologyAgnosticDeploymentModel
   * @return a map of Components and their corresponding ComponentTypes to be renamed
   */
  private static Map<Component, ComponentType> findComponentsToRename(
      TechnologyAgnosticDeploymentModel tadm) {
    Map<Component, ComponentType> toBeRenamed = new HashMap<>();
    for (ComponentType componentType : tadm.getComponentTypes()) {
      for (Component component : tadm.getComponents()) {
        if (componentType.getName().endsWith(component.getName())) {
          toBeRenamed.put(component, componentType);
        }
      }
    }
    return toBeRenamed;
  }

  /**
   * Updates the model after renaming the ComponentTypes and their corresponding Components.
   *
   * @param tadm the TechnologyAgnosticDeploymentModel
   * @param toBeRenamed the map of Components and their corresponding ComponentTypes to be renamed
   */
  private static void updateModelAfterRenaming(
      TechnologyAgnosticDeploymentModel tadm, Map<Component, ComponentType> toBeRenamed) {
    tadm.getComponents().removeAll(toBeRenamed.keySet());
    tadm.getComponentTypes().removeAll(toBeRenamed.values());

    toBeRenamed.forEach(
        (component, componentType) -> {
          String newName = componentType.getName().replace(component.getName(), "");
          newName = newName.substring(0, newName.length() - 1); // Remove trailing "_"
          componentType.setName(newName);
          tadm.getComponentTypes().add(componentType);
          tadm.getComponents().add(component);
        });
  }

  /**
   * Merges ComponentTypes with the same name in the TechnologyAgnosticDeploymentModel.
   *
   * @param tadm the TechnologyAgnosticDeploymentModel
   */
  private static void mergeComponentTypesByName(TechnologyAgnosticDeploymentModel tadm) {
    Map<String, ComponentType> mergedComponentTypes = new HashMap<>();
    List<ComponentType> toBeRemoved = new ArrayList<>();

    for (ComponentType componentType : tadm.getComponentTypes()) {
      mergedComponentTypes.merge(
          componentType.getName(), componentType, ComponentTypePostProcessor::mergeComponentTypes);
      toBeRemoved.add(componentType);
    }

    tadm.getComponentTypes().removeAll(toBeRemoved);
    tadm.getComponentTypes().addAll(mergedComponentTypes.values());

    for (Component component : tadm.getComponents()) {
      String typeName = component.getType().getName();
      if (mergedComponentTypes.get(typeName) != null) {
        component.setType(mergedComponentTypes.get(typeName));
      }
    }
  }

  /**
   * Merges two ComponentTypes into one by combining their properties and operations.
   *
   * @param type1 the first ComponentType
   * @param type2 the second ComponentType
   * @return a merged ComponentType
   */
  private static ComponentType mergeComponentTypes(ComponentType type1, ComponentType type2) {
    Map<String, Property> properties = new HashMap<>();
    Map<String, Operation> operations = new HashMap<>();

    // Merge properties and operations from both types
    addPropertiesAndOperations(type1, properties, operations);
    addPropertiesAndOperations(type2, properties, operations);
    cleanPropertyValues(properties.values());

    return new ComponentType(
        type2.getName(),
        type2.getDescription(),
        new ArrayList<>(properties.values()),
        new ArrayList<>(operations.values()),
        type2.getParentType());
  }

  /**
   * Adds properties and operations from a ComponentType to the respective maps.
   *
   * @param type the ComponentType to extract data from
   * @param properties the map of properties to be updated
   * @param operations the map of operations to be updated
   */
  private static void addPropertiesAndOperations(
      ComponentType type, Map<String, Property> properties, Map<String, Operation> operations) {
    if (type != null) {
      for (Property property : type.getProperties()) {
        properties.put(property.getKey(), property);
      }
      for (Operation operation : type.getOperations()) {
        operations.put(operation.getName(), operation);
      }
    }
  }

  /**
   * Cleans the values of the properties from a ComponentType, as they should be generic.
   *
   * @param properties the map of properties to be updated
   */
  private static void cleanPropertyValues(Collection<Property> properties) {
    try {
      for (Property property : properties) {
        switch (property.getType()) {
          case STRING:
            property.setValue("");
            break;
          case DOUBLE:
            property.setValue(0.0);
            break;
          case BOOLEAN:
            property.setValue(false);
            break;
          case INTEGER:
            property.setValue(0);
            break;
        }
      }
    } catch (InvalidPropertyValueException e) {
      throw new RuntimeException(e);
    }
  }
}
