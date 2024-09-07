package ust.tad.terraformmpsplugin.analysis.util;

import ust.tad.terraformmpsplugin.models.tadm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ComponentTypePostProcessor {
    public static TechnologyAgnosticDeploymentModel mergeDuplicateComponentTypes(TechnologyAgnosticDeploymentModel mpsResult) {

        // 1. Recreate the original ComponentType names without the ComponentName in it
        HashMap<Component, ComponentType> toBeRenamed = new HashMap<>();

        for (ComponentType componentType : mpsResult.getComponentTypes()) {
            for (Component component : mpsResult.getComponents()) {
                if (componentType.getName().endsWith(component.getName())) {
                    toBeRenamed.put(component, componentType);
                }
            }
        }

        if (toBeRenamed.isEmpty()) {
            return mpsResult;
        }

        mpsResult.getComponents().removeAll(toBeRenamed.keySet());
        mpsResult.getComponentTypes().removeAll(toBeRenamed.values());

        toBeRenamed.forEach((component, componentType) -> {
            componentType.setName(componentType.getName().replace(component.getName(), ""));
            componentType.setName(componentType.getName().substring(0, componentType.getName().length() - 1));
            mpsResult.getComponentTypes().add(componentType);
            mpsResult.getComponents().add(component);
        });

        // 2. Look if there are multiple ComponentTypes with the Same name and merge them
        HashMap<String, ComponentType> mergedComponentTypes = new HashMap<>();
        HashSet<ComponentType> toBeRemoved = new HashSet<>();

        for (ComponentType componentType : mpsResult.getComponentTypes()) {
            for (ComponentType componentTypeToCompare : mpsResult.getComponentTypes()) {
                if (componentType.getName().equals(componentTypeToCompare.getName()) && componentType != componentTypeToCompare) {
                    toBeRemoved.add(componentType);
                    toBeRemoved.add(componentTypeToCompare);

                    ComponentType alreadyMergedComponentType = mergedComponentTypes.get(componentType.getName());

                    ComponentType mergedComponentType = mergeComponentTypes(alreadyMergedComponentType, componentType, componentTypeToCompare);

                    mergedComponentTypes.put(mergedComponentType.getName(), mergedComponentType);
                }
            }
        }

        mpsResult.getComponentTypes().removeAll(toBeRemoved);
        mpsResult.getComponentTypes().addAll(mergedComponentTypes.values());

        for (Component component : mpsResult.getComponents()) {
            String typeName = component.getType().getName();
            if (mergedComponentTypes.get(typeName) != null) {
                component.setType(mergedComponentTypes.get(typeName));
            }
        }


        return mpsResult;
    }

    private static ComponentType mergeComponentTypes(ComponentType type1, ComponentType type2, ComponentType type3) {

        HashMap<String, Property> properties = new HashMap<>();
        HashMap<String, Operation> operations = new HashMap<>();

        if (type1 != null) {
            for (Property property : type1.getProperties()) {
                properties.put(property.getKey(), property);
            }
            for (Operation operation : type1.getOperations()) {
                operations.put(operation.getName(), operation);
            }
        }

        for (Property property : type2.getProperties()) {
            properties.put(property.getKey(), property);
        }

        for (Property property : type3.getProperties()) {
            properties.put(property.getKey(), property);
        }

        for (Operation operation : type2.getOperations()) {
            operations.put(operation.getName(), operation);
        }

        for (Operation operation : type3.getOperations()) {
            operations.put(operation.getName(), operation);
        }
        try {
            for (Property property : properties.values()) {
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

        return new ComponentType(
                type2.getName(),
                type2.getDescription(),
                new ArrayList<>(properties.values()),
                new ArrayList<>(operations.values()),
                type2.getParentType()
        );
    }
}
