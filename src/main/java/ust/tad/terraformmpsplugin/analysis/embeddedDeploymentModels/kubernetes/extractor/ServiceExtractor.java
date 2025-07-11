package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.extractor;

import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.common.types.StringStringMap;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.service.KubernetesService;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.service.ServicePort;
import ust.tad.terraformmpsplugin.terraformmodel.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServiceExtractor extends BaseExtractor {

    static KubernetesService extractKubernetesService(Resource kubernetesServiceResource) {
        KubernetesService service = new KubernetesService();

        extractServiceName(kubernetesServiceResource, service);
        extractSpecArguments(kubernetesServiceResource, service);
        extractServicePorts(kubernetesServiceResource, service);
        extractSelectors(kubernetesServiceResource, service);

        return service;
    }

    private static void extractServiceName(Resource kubernetesServiceResource,
                                           KubernetesService service) {
        try {
            Block metadata = kubernetesServiceResource.getBlockByBlockType("metadata", List.of());
            service.setName(removeEnclosingTicks(metadata.findArgumentByIdentifier("name").getExpression()));
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractSpecArguments(Resource kubernetesServiceResource,
                                             KubernetesService service) {
        try {
            Block spec = kubernetesServiceResource.getBlockByBlockType("spec", List.of());
            service.setType(removeEnclosingTicks(spec.findArgumentByIdentifier("type").getExpression()));
            service.setIpFamilyPolicy(spec.findArgumentByIdentifier("ip_family_policy").getExpression());
            service.setClusterIP(spec.findArgumentByIdentifier("cluster_ip").getExpression());
            service.setSessionAffinity(spec.findArgumentByIdentifier("session_affinity").getExpression());
            service.setLoadBalancerIP(spec.findArgumentByIdentifier("load_balancer_ip").getExpression());
            service.setExternalName(spec.findArgumentByIdentifier("external_name").getExpression());
            service.setExternalTrafficPolicy(spec.findArgumentByIdentifier(
                    "external_traffic_policy").getExpression());
            service.setInternalTrafficPolicy(spec.findArgumentByIdentifier(
                    "internal_traffic_policy").getExpression());
            service.setHealthCheckNodePort(Integer.parseInt(spec.findArgumentByIdentifier(
                    "health_check_node_port").getExpression()));
            service.setIpFamilies(new HashSet<>(Arrays.asList(spec.findArgumentByIdentifier(
                    "ip_families").getExpression().replace("[", "").replace("]", "").split(","))));
            service.setClusterIPs(new HashSet<>(Arrays.asList(spec.findArgumentByIdentifier(
                    "cluster_ips").getExpression().replace("[", "").replace("]", "").split(","))));
            service.setExternalIPs(new HashSet<>(Arrays.asList(spec.findArgumentByIdentifier(
                    "external_ips").getExpression().replace("[", "").replace("]", "").split(","))));
            service.setLoadBalancerSourceRanges(new HashSet<>(Arrays.asList(spec.findArgumentByIdentifier("load_balancer_source_ranges").getExpression().replace("[", "").replace("]", "").split(","))));
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractServicePorts(Resource kubernetesServiceResource,
                                            KubernetesService service) {
        try {
            Block spec = kubernetesServiceResource.getBlockByBlockType("spec", List.of());
            List<Block> portBlocks = spec.findAllNestedBlocksByBlockType("port");
            Set<ServicePort> servicePorts = new HashSet<>();
            for (Block portBlock : portBlocks) {
                ServicePort servicePort = new ServicePort();
                servicePort.setName(removeEnclosingTicks(portBlock.findArgumentByIdentifier("name"
                ).getExpression()));
                servicePort.setPort(Integer.parseInt(removeEnclosingTicks(portBlock.findArgumentByIdentifier("port").getExpression())));
                servicePort.setTargetPort(removeEnclosingTicks(portBlock.findArgumentByIdentifier("target_port").getExpression()));
                servicePorts.add(servicePort);
            }
            service.setServicePorts(servicePorts);
        } catch (BlockNotFoundException | ArgumentNotFoundException ignored) {
        }
    }

    private static void extractSelectors(Resource kubernetesServiceResource,
                                         KubernetesService service) {
        try {
            Block selectorsBlock = kubernetesServiceResource.getBlockByBlockType("spec", List.of(
                    "selector"));
            Set<StringStringMap> selectors = new HashSet<>();
            for (Argument argument : selectorsBlock.getArguments()) {
                StringStringMap selector =
                        new StringStringMap(removeEnclosingTicks(argument.getIdentifier()),
                                argument.getExpression());
                selectors.add(selector);
            }
            service.setSelectors(selectors);
        } catch (BlockNotFoundException ignored) {
        }
    }
}
