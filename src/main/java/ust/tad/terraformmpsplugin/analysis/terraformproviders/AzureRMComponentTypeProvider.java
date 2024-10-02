package ust.tad.terraformmpsplugin.analysis.terraformproviders;

import java.util.ArrayList;
import java.util.List;
import ust.tad.terraformmpsplugin.models.tadm.*;

public class AzureRMComponentTypeProvider {

  public static ComponentType createPhysicalNodeType() throws InvalidPropertyValueException {
    List<Property> properties = createPropertiesForPhysicalNodeType();

    ComponentType physicalNodeType = new ComponentType();
    physicalNodeType.setName("physical_node");
    physicalNodeType.setProperties(properties);

    return physicalNodeType;
  }

  public static List<Property> createPropertiesForPhysicalNodeType()
      throws InvalidPropertyValueException {
    List<Property> properties = new ArrayList<>();

    Property cpuCount = new Property();
    cpuCount.setKey("cpu_count");
    cpuCount.setType(PropertyType.INTEGER);

    Property ram = new Property();
    ram.setKey("ram_GiB");
    ram.setType(PropertyType.INTEGER);

    Property storage = new Property();
    storage.setKey("storage_GiB");
    storage.setType(PropertyType.INTEGER);

    properties.add(cpuCount);
    properties.add(ram);
    properties.add(storage);

    return properties;
  }

  public static ComponentType createOperatingSystemType() throws InvalidPropertyValueException {
    List<Property> properties = createPropertiesForOperatingSystemType();

    ComponentType operatingSystemType = new ComponentType();
    operatingSystemType.setName("operating_system");
    operatingSystemType.setProperties(properties);

    return operatingSystemType;
  }

  public static List<Property> createPropertiesForOperatingSystemType()
      throws InvalidPropertyValueException {
    List<Property> properties = new ArrayList<>();

    Property name = new Property();
    name.setKey("name");
    name.setType(PropertyType.STRING);

    Property version = new Property();
    version.setKey("version");
    version.setType(PropertyType.STRING);

    Property osFamily = new Property();
    osFamily.setKey("os_family");
    osFamily.setType(PropertyType.STRING);

    properties.add(name);
    properties.add(version);
    properties.add(osFamily);

    return properties;
  }

  public static ComponentType createContainerRuntimeType() throws InvalidPropertyValueException {
    List<Property> properties = createPropertiesForContainerRuntimeType();

    ComponentType operatingSystemType = new ComponentType();
    operatingSystemType.setName("container_runtime");
    operatingSystemType.setProperties(properties);

    return operatingSystemType;
  }

  public static List<Property> createPropertiesForContainerRuntimeType()
      throws InvalidPropertyValueException {
    List<Property> properties = new ArrayList<>();

    Property name = new Property();
    name.setKey("name");
    name.setType(PropertyType.STRING);

    Property version = new Property();
    version.setKey("version");
    version.setType(PropertyType.STRING);

    properties.add(name);
    properties.add(version);

    return properties;
  }

  public static List<Property> createPropertiesForDefaultContainerRuntime()
      throws InvalidPropertyValueException {
    List<Property> properties = new ArrayList<>();
    Property name = new Property();
    name.setKey("name");
    name.setType(PropertyType.STRING);
    name.setValue("containerd");
    name.setConfidence(Confidence.SUSPECTED);
    properties.add(name);
    return properties;
  }

  public static List<Property> createPropertiesForDefaultOperatingSystem()
      throws InvalidPropertyValueException {
    List<Property> properties = new ArrayList<>();

    Property name = new Property();
    name.setKey("name");
    name.setType(PropertyType.STRING);
    name.setValue("Ubuntu");
    name.setConfidence(Confidence.SUSPECTED);

    Property version = new Property();
    version.setKey("version");
    version.setType(PropertyType.STRING);
    version.setValue("18.04");
    version.setConfidence(Confidence.SUSPECTED);

    Property osFamily = new Property();
    osFamily.setKey("os_family");
    osFamily.setType(PropertyType.STRING);
    osFamily.setValue("Linux");
    osFamily.setConfidence(Confidence.SUSPECTED);

    properties.add(name);
    properties.add(version);
    properties.add(osFamily);

    return properties;
  }

  public static List<Property> getPropertiesForVMSize(String vmSize)
      throws InvalidPropertyValueException {
    List<Property> properties = new ArrayList<>();

    Property cpuCount = new Property();
    cpuCount.setKey("cpu_count");
    cpuCount.setType(PropertyType.INTEGER);

    Property ram = new Property();
    ram.setKey("ram_GiB");
    ram.setType(PropertyType.INTEGER);

    Property storage = new Property();
    storage.setKey("storage_GiB");
    storage.setType(PropertyType.INTEGER);

    if (vmSize.equals("standard_b4ms")) {
      cpuCount.setValue(4);
      cpuCount.setConfidence(Confidence.SUSPECTED);
      ram.setValue(16);
      ram.setConfidence(Confidence.SUSPECTED);
      storage.setValue(32);
      storage.setConfidence(Confidence.SUSPECTED);
    }

    properties.add(cpuCount);
    properties.add(ram);
    properties.add(storage);

    return properties;
  }
}
