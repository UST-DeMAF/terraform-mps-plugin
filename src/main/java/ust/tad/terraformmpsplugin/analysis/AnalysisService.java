package ust.tad.terraformmpsplugin.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import ust.tad.terraformmpsplugin.analysistask.AnalysisTaskResponseSender;
import ust.tad.terraformmpsplugin.analysistask.Location;
import ust.tad.terraformmpsplugin.models.ModelsService;
import ust.tad.terraformmpsplugin.models.tadm.Component;
import ust.tad.terraformmpsplugin.models.tadm.ComponentType;
import ust.tad.terraformmpsplugin.models.tadm.Confidence;
import ust.tad.terraformmpsplugin.models.tadm.InvalidPropertyValueException;
import ust.tad.terraformmpsplugin.models.tadm.InvalidRelationException;
import ust.tad.terraformmpsplugin.models.tadm.Property;
import ust.tad.terraformmpsplugin.models.tadm.PropertyType;
import ust.tad.terraformmpsplugin.models.tadm.Relation;
import ust.tad.terraformmpsplugin.models.tadm.RelationType;
import ust.tad.terraformmpsplugin.models.tadm.TechnologyAgnosticDeploymentModel;
import ust.tad.terraformmpsplugin.models.tsdm.DeploymentModelContent;
import ust.tad.terraformmpsplugin.models.tsdm.InvalidAnnotationException;
import ust.tad.terraformmpsplugin.models.tsdm.InvalidNumberOfContentException;
import ust.tad.terraformmpsplugin.models.tsdm.InvalidNumberOfLinesException;
import ust.tad.terraformmpsplugin.models.tsdm.Line;
import ust.tad.terraformmpsplugin.models.tsdm.TechnologySpecificDeploymentModel;
import ust.tad.terraformmpsplugin.terraformmodel.Argument;
import ust.tad.terraformmpsplugin.terraformmodel.Block;
import ust.tad.terraformmpsplugin.terraformmodel.Provider;
import ust.tad.terraformmpsplugin.terraformmodel.Resource;
import ust.tad.terraformmpsplugin.terraformmodel.Variable;

@Service
public class AnalysisService {
    
    @Autowired
    ModelsService modelsService;

    @Autowired
    AnalysisTaskResponseSender analysisTaskResponseSender;
    
    private TechnologySpecificDeploymentModel tsdm;

    private TechnologyAgnosticDeploymentModel tadm;

    private Set<Integer> newEmbeddedDeploymentModelIndexes = new HashSet<>();

    private static final List<String> supportedProviders = List.of("azurerm");
    
    private Set<Variable> variables = new HashSet<>();

    private Set<Provider> providers = new HashSet<>();

    private Set<Resource> resources = new HashSet<>();

    /**
     * Start the analysis of the deployment model.
     * 1. Retrieve internal deployment models from models service
     * 2. Parse in technology-specific deployment model from locations
     * 3. Update tsdm with new information
     * 4. Transform to EDMM entities and update tadm
     * 5. Send updated models to models service
     * 6. Send AnalysisTaskResponse or EmbeddedDeploymentModelAnalysisRequests if present 
     * 
     * @param taskId
     * @param transformationProcessId
     * @param commands
     * @param locations
     */
    public void startAnalysis(UUID taskId, UUID transformationProcessId, List<String> commands, List<Location> locations) {
        TechnologySpecificDeploymentModel completeTsdm = modelsService.getTechnologySpecificDeploymentModel(transformationProcessId);
        this.tsdm = getExistingTsdm(completeTsdm, locations);
        if(tsdm == null) {
            analysisTaskResponseSender.sendFailureResponse(taskId, "No technology-specific deployment model found!");
            return;            
        }
        this.tadm = modelsService.getTechnologyAgnosticDeploymentModel(transformationProcessId);

        try {
            runAnalysis(locations);
        } catch (InvalidNumberOfContentException | URISyntaxException | IOException | InvalidNumberOfLinesException | InvalidAnnotationException | InvalidPropertyValueException | InvalidRelationException e) {
            e.printStackTrace();
            analysisTaskResponseSender.sendFailureResponse(taskId, e.getClass()+": "+e.getMessage());
            return;
        }

        updateDeploymentModels(this.tsdm, this.tadm);

        if(newEmbeddedDeploymentModelIndexes.isEmpty()) {
            analysisTaskResponseSender.sendSuccessResponse(taskId);
        } else {
            for (int index : newEmbeddedDeploymentModelIndexes) {
                analysisTaskResponseSender.sendEmbeddedDeploymentModelAnalysisRequestFromModel(
                    this.tsdm.getEmbeddedDeploymentModels().get(index), taskId); 
            }
            analysisTaskResponseSender.sendSuccessResponse(taskId);
        }
    }

    private TechnologySpecificDeploymentModel getExistingTsdm(TechnologySpecificDeploymentModel tsdm, List<Location> locations) {
        for (DeploymentModelContent content : tsdm.getContent()) {
            for (Location location : locations) {
                if (location.getUrl().equals(content.getLocation())) {
                    return tsdm;
                }
            }
        }
        for (TechnologySpecificDeploymentModel embeddedDeploymentModel : tsdm.getEmbeddedDeploymentModels()) {
            TechnologySpecificDeploymentModel foundModel =  getExistingTsdm(embeddedDeploymentModel, locations);
            if (foundModel != null) {
                return foundModel;
            }
        }
        return null;
    }

    private void updateDeploymentModels(TechnologySpecificDeploymentModel tsdm, TechnologyAgnosticDeploymentModel tadm) {
        modelsService.updateTechnologySpecificDeploymentModel(tsdm);
        modelsService.updateTechnologyAgnosticDeploymentModel(tadm);
    }

    /**
     * Iterate over the locations and parse in all files that can be found.
     * The file has to have the fileextension ".tf", otherwise it will be ignored.
     * If the given location is a directory, iterate over all contained files.
     * Removes the deployment model content associated with the old directory locations
     * because it has been resolved to the contained files.
     * 
     * @param locations
     * @throws InvalidNumberOfContentException
     * @throws URISyntaxException
     * @throws InvalidAnnotationException
     * @throws InvalidNumberOfLinesException
     * @throws IOException
     * @throws InvalidPropertyValueException
     * @throws InvalidRelationException
     * @throws MalformedURLException
     */
    private void runAnalysis(List<Location> locations) throws InvalidNumberOfContentException, URISyntaxException, IOException, InvalidNumberOfLinesException, InvalidAnnotationException, InvalidPropertyValueException, InvalidRelationException {
        for(Location location : locations) {
            if ("file".equals(location.getUrl().getProtocol()) && new File(location.getUrl().toURI()).isDirectory()) {
                File directory = new File(location.getUrl().toURI());
                for (File file : directory.listFiles()) {
                    if("tf".equals(StringUtils.getFilenameExtension(file.toURI().toURL().toString()))) {                        
                        parseFile(file.toURI().toURL());
                    }
                }
                DeploymentModelContent contentToRemove = new DeploymentModelContent();
                for (DeploymentModelContent content : this.tsdm.getContent()) {
                    if (content.getLocation().equals(location.getUrl())) {
                        contentToRemove = content;
                    }
                }
                this.tsdm.removeDeploymentModelContent(contentToRemove);
            } else {
                if("tf".equals(StringUtils.getFilenameExtension(location.getUrl().toString()))) {  
                    parseFile(location.getUrl());
                }
            }
        }
        transformToEDMM();
    }

    /**
     * Parses in a file.
     * Creates entities for the terraform model for the resources, providers and variables it can find.
     * At the same time, updates the technology-specific deployment model.
     * Iterates over the lines in the file and adds corresponding Line entities to a new DeploymentModelContent.
     * In the end it adds the DeploymentModelContent to the technology-specific deployment model.
     * 
     * @param url
     * @throws IOException
     * @throws InvalidNumberOfLinesException
     * @throws InvalidAnnotationException
     */
    private void parseFile(URL url) throws IOException, InvalidNumberOfLinesException, InvalidAnnotationException {
        DeploymentModelContent deploymentModelContent = new DeploymentModelContent();
        deploymentModelContent.setLocation(url);

        List<Line> lines = new ArrayList<>();
        int lineNumber = 1;
        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
        while(reader.ready()) {
            String nextline = reader.readLine();
            if (nextline.startsWith("provider")) {
                String providerName = nextline.split(" ")[1].replaceAll("(^\")|(\"$)", "");
                Provider provider = new Provider();
                provider.setName(providerName);
                Line line = new Line();
                line.setNumber(lineNumber);
                line.setAnalyzed(true);
                double comprehensibility = 0D;
                if (supportedProviders.contains(providerName)) {
                    comprehensibility = 1D;
                    line.setComprehensibility(comprehensibility);
                } else {
                    line.setComprehensibility(0.5D);
                }
                lines.add(line);
                lineNumber++;
                nextline = reader.readLine();
                while (!nextline.startsWith("}")) {   
                    if (nextline.contains("=")) {
                        String[] tokens = nextline.split("=");
                        provider.addArgument(new Argument(tokens[0].trim(), tokens[1].trim()));
                        lines.add(new Line(lineNumber, comprehensibility, true));
                    } else {
                        String[] tokens = nextline.trim().split(" ");
                        Block block = new Block();
                        block.setBlockType(tokens[0]);
                        lines.add(new Line(lineNumber, comprehensibility, true));
                        while (!nextline.endsWith("}")) {
                            lineNumber++;
                            nextline = reader.readLine();  
                            if (nextline.contains("=")) {
                                String[] argumentTokens = nextline.split("=");
                                block.addArgument(new Argument(argumentTokens[0], argumentTokens[1]));
                                lines.add(new Line(lineNumber, comprehensibility, true));
                            }
                        } 
                        provider.addBlock(block);         
                    }
                    lineNumber++;                        
                    nextline = reader.readLine();  
                }
                this.providers.add(provider);
            } else if (nextline.startsWith("resource")) {
                Resource resource = new Resource();
                String[] lineTokens = nextline.split(" ");
                String resourceType = lineTokens[1].replaceAll("(^\")|(\"$)", "");
                resource.setResourceType(resourceType);
                String resourceName = lineTokens[2].replaceAll("(^\")|(\"$)", "");
                resource.setResourceName(resourceName);
                Line line = new Line();
                line.setNumber(lineNumber);
                line.setAnalyzed(true);
                double comprehensibility = 0D;       
                //set comprehensibility based on resource type   
                String providerOfResource = resourceType.split("_")[0];
                if (supportedProviders.contains(providerOfResource)) {
                    comprehensibility = 1D;
                    line.setComprehensibility(comprehensibility);   
                } else {
                    line.setComprehensibility(0.5D);
                }
                lines.add(line);
                lineNumber++;
                nextline = reader.readLine();
                while (!nextline.startsWith("}")) {   
                    if (nextline.contains("=")) {
                        String[] tokens = nextline.split("=");
                        resource.addArgument(new Argument(tokens[0].trim(), tokens[1].trim()));
                        lines.add(new Line(lineNumber, comprehensibility, true));
                    } else if (nextline.contains("{")) {
                        nextline = nextline.trim();
                        String[] tokens = nextline.split(" ");
                        Block block = new Block();
                        block.setBlockType(tokens[0]);
                        lines.add(new Line(lineNumber, comprehensibility, true));      
                        if (!nextline.endsWith("}")) {
                            lineNumber++;
                            nextline = reader.readLine();  
                            while (!nextline.trim().startsWith("}")) {
                                if (nextline.contains("=")) {
                                    String[] argumentTokens = nextline.split("=");
                                    block.addArgument(new Argument(argumentTokens[0].trim(), argumentTokens[1].trim()));
                                    lines.add(new Line(lineNumber, comprehensibility, true));
                                }
                                lineNumber++;
                                nextline = reader.readLine();  
                            } 
                        }                        
                        resource.addBlock(block);         
                    }
                    lineNumber++;
                    nextline = reader.readLine();  
                }                
                this.resources.add(resource);
            } else if (nextline.startsWith("variable")) {
                String identifier = nextline.split(" ")[1].replaceAll("(^\")|(\"$)", "");
                lines.add(new Line(lineNumber, 1D, true));   

                String expression = "";
                lineNumber++;
                nextline = reader.readLine();
                while (!nextline.startsWith("}")) {   
                    if (nextline.trim().split(" ")[0].equals("default")) {
                        String[] tokens = nextline.split("=");
                        expression = tokens[1].trim().replaceAll("(^\")|(\"$)", "");
                        lines.add(new Line(lineNumber, 1D, true));
                    } else {
                        lines.add(new Line(lineNumber, 0D, true));
                    }
                    lineNumber++;
                    nextline = reader.readLine();  
                }
                this.variables.add(new Variable(identifier, expression));
            }     
            lineNumber++;
        }
        reader.close();

        deploymentModelContent.setLines(lines);
        this.tsdm.addDeploymentModelContent(deploymentModelContent);
    }


    private void transformToEDMM() throws InvalidPropertyValueException, InvalidRelationException {
        for (Resource resource : this.resources) {
            if (resource.getResourceType().equals("azurerm_kubernetes_cluster")) {
                transformAKS(resource);
            }
        }
    }    

    private void transformAKS(Resource resource) throws InvalidPropertyValueException, InvalidRelationException {
        ComponentType physicalNodeType = ComponentTypeProvider.createPhysicalNodeType();   
        ComponentType operatingSystemType = ComponentTypeProvider.createOperatingSystemType();
        ComponentType containerRuntimeType = ComponentTypeProvider.createContainerRuntimeType();
        List<ComponentType> types =  this.tadm.getComponentTypes();     
        types.add(physicalNodeType);  
        types.add(operatingSystemType);  
        types.add(containerRuntimeType);
        this.tadm.setComponentTypes(types);   

        for (Block block : resource.getBlocks()) {
            if (block.getBlockType().equals("default_node_pool")) {  
                String physicalNodeComponentName = "";
                int nodeCount = 0;
                String nodeType = "";
                for (Argument argument : block.getArguments()) {
                    switch (argument.getIdentifier()) {
                        case "name":   
                            physicalNodeComponentName = argument.getExpression().trim().replaceAll("(^\")|(\"$)", "");                         
                            break;
                        case "node_count":  
                            nodeCount = Integer.parseInt(argument.getExpression());                          
                            break;
                        case "vm_size": 
                            nodeType = argument.getExpression().trim().replaceAll("(^\")|(\"$)", "");                          
                            break;
                        default:
                            break;
                    }
                }
                for (int i = 0; i < nodeCount; i++) {
                    Component physicalNodeComponent = new Component();
                    physicalNodeComponent.setName(physicalNodeComponentName);
                    physicalNodeComponent.setType(physicalNodeType);
                    physicalNodeComponent.setConfidence(Confidence.CONFIRMED);
                    physicalNodeComponent.setProperties(getPropertiesForVMSize(nodeType));
                    
                    Component operatingSystemComponent = new Component();
                    operatingSystemComponent.setName("default-operating-system");
                    operatingSystemComponent.setType(operatingSystemType);
                    operatingSystemComponent.setConfidence(Confidence.CONFIRMED);
                    operatingSystemComponent.setProperties(createPropertiesForDefaultOperatingSystem());
                    
                    Component containerRuntimeComponent = new Component();
                    containerRuntimeComponent.setName("default-container-runtime");
                    containerRuntimeComponent.setType(containerRuntimeType);
                    containerRuntimeComponent.setConfidence(Confidence.CONFIRMED);
                    containerRuntimeComponent.setProperties(createPropertiesForDefaultContainerRuntime());

                    List<Component> components = this.tadm.getComponents();
                    components.add(physicalNodeComponent);
                    components.add(operatingSystemComponent);
                    components.add(containerRuntimeComponent);
                    this.tadm.setComponents(components);
                    
                    RelationType hostedOnRelationType = new RelationType();
                    Optional<RelationType> hostedOnRelationTypeOpt = this.tadm.getRelationTypes().stream().filter(relationType -> relationType.getName().equals("HostedOn")).findFirst();
                    if (hostedOnRelationTypeOpt.isPresent()) {
                        hostedOnRelationType = hostedOnRelationTypeOpt.get();
                    }

                    Relation osOnNodeRelation = new Relation(); //(type, source, target, confidence)
                    osOnNodeRelation.setName(operatingSystemComponent.getName()+"_"+hostedOnRelationType.getName()+"_"+physicalNodeComponent.getName());
                    osOnNodeRelation.setType(hostedOnRelationType);
                    osOnNodeRelation.setSource(operatingSystemComponent);
                    osOnNodeRelation.setTarget(physicalNodeComponent);
                    osOnNodeRelation.setConfidence(Confidence.CONFIRMED);

                    Relation crOnOSRelation = new Relation();
                    crOnOSRelation.setName(containerRuntimeComponent.getName()+"_"+hostedOnRelationType.getName()+"_"+operatingSystemComponent.getName());
                    crOnOSRelation.setType(hostedOnRelationType);
                    crOnOSRelation.setSource(containerRuntimeComponent);
                    crOnOSRelation.setTarget(operatingSystemComponent);
                    crOnOSRelation.setConfidence(Confidence.CONFIRMED);

                    List<Relation> relations = this.tadm.getRelations();
                    relations.add(osOnNodeRelation);
                    relations.add(crOnOSRelation);
                    this.tadm.setRelations(relations);
                }
            }
        }
    }

    public static List<Property> createPropertiesForDefaultContainerRuntime() throws InvalidPropertyValueException {
        List<Property> properties = new ArrayList<>();

        Property name = new Property();
        name.setKey("name");
        name.setType(PropertyType.STRING);
        name.setValue("containerd");
        name.setConfidence(Confidence.SUSPECTED);
        
        properties.add(name);

        return properties;
    }

    private List<Property> createPropertiesForDefaultOperatingSystem() throws InvalidPropertyValueException {
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


    private List<Property> getPropertiesForVMSize(String vmSize) throws InvalidPropertyValueException {
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
