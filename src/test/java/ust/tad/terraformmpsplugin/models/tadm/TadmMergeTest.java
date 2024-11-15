package ust.tad.terraformmpsplugin.models.tadm;

import org.junit.jupiter.api.Test;

import java.util.List;


public class TadmMergeTest {

    @Test
    public void mergeTADMs() throws InvalidPropertyValueException, InvalidRelationException {
        TechnologyAgnosticDeploymentModel existingTADM = createExistingTADM();
        TechnologyAgnosticDeploymentModel newTADM = createNewTADM();

        existingTADM.addFromOtherTADM(newTADM);
        System.out.println(existingTADM);
    }

    private TechnologyAgnosticDeploymentModel createExistingTADM() {
        TechnologyAgnosticDeploymentModel tadm = new TechnologyAgnosticDeploymentModel();

        ComponentType baseType = new ComponentType();
        baseType.setName("Base Type");

        ComponentType cloudProvider = new ComponentType();
        cloudProvider.setName("Cloud Provider");
        cloudProvider.setParentType(baseType);

        RelationType dependsOn = new RelationType();
        dependsOn.setName("DependsOn");
        RelationType hostedOn = new RelationType();
        hostedOn.setName("HostedOn");
        hostedOn.setParentType(dependsOn);
        RelationType connectsTo = new RelationType();
        connectsTo.setName("ConnectsTo");
        connectsTo.setParentType(dependsOn);

        tadm.setComponentTypes(List.of(baseType, cloudProvider));
        tadm.setRelationTypes(List.of(dependsOn, hostedOn, connectsTo));

        return tadm;
    }

    private TechnologyAgnosticDeploymentModel createNewTADM() throws InvalidPropertyValueException, InvalidRelationException {
        TechnologyAgnosticDeploymentModel tadm = new TechnologyAgnosticDeploymentModel();

        ComponentType baseType = new ComponentType();
        baseType.setName("Base Type");

        ComponentType cloudProvider = new ComponentType();
        cloudProvider.setName("Cloud Provider");
        cloudProvider.setParentType(baseType);

        Property newProperty = new Property("key", PropertyType.STRING, false,
                "val", Confidence.SUSPECTED);
        cloudProvider.setProperties(List.of(newProperty));

        Operation newOperation = new Operation("name", List.of(), Confidence.SUSPECTED);
        cloudProvider.setOperations(List.of(newOperation));

        RelationType dependsOn = new RelationType();
        dependsOn.setName("DependsOn");
        RelationType hostedOn = new RelationType();
        hostedOn.setName("HostedOn");
        hostedOn.setParentType(dependsOn);
        RelationType connectsTo = new RelationType();
        connectsTo.setName("ConnectsTo");
        connectsTo.setParentType(dependsOn);

        Component newComponent = new Component();
        newComponent.setName("compSource");
        newComponent.setType(cloudProvider);

        Component newComponentTarget = new Component();
        newComponentTarget.setName("compTarget");
        newComponentTarget.setType(cloudProvider);

        Relation newRelation = new Relation();
        newRelation.setName("newSourceHostedOnNewTarget");
        newRelation.setType(hostedOn);
        newRelation.setSource(newComponent);
        newRelation.setTarget(newComponentTarget);

        tadm.setComponentTypes(List.of(baseType, cloudProvider));
        tadm.setRelationTypes(List.of(dependsOn, hostedOn, connectsTo));
        tadm.setComponents(List.of(newComponent, newComponentTarget));
        tadm.setRelations(List.of(newRelation));

        return tadm;
    }
}
