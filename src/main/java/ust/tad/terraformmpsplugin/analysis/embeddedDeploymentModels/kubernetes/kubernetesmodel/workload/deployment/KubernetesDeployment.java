package ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.workload.deployment;

import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.common.types.StringStringMap;
import ust.tad.terraformmpsplugin.analysis.embeddedDeploymentModels.kubernetes.kubernetesmodel.workload.pods.KubernetesPodSpec;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class KubernetesDeployment {
    private String name;
    private int replicas;
    private int minReadySeconds;
    private int revisionHistorySeconds;
    private boolean paused;
    private Set<StringStringMap> labels = new HashSet<>();
    private Set<StringStringMap> selectorMatchLabels = new HashSet<>();
    private Set<KubernetesPodSpec> podSpecs = new HashSet<>();

    public KubernetesDeployment() {}

    public KubernetesDeployment(
            String name,
            int replicas,
            Set<StringStringMap> labels,
            Set<StringStringMap> selectorMatchLabels,
            Set<KubernetesPodSpec> podSpecs
    ) {
        this.name = name;
        this.replicas = replicas;
        this.labels = labels;
        this.selectorMatchLabels = selectorMatchLabels;
        this.podSpecs = podSpecs;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getReplicas() {
        return this.replicas;
    }

    public void setReplicas(int replicas) {
        this.replicas = replicas;
    }

    public Set<StringStringMap> getLabels() {
        return this.labels;
    }

    public void setLabels(Set<StringStringMap> labels) {
        this.labels = labels;
    }

    public Set<StringStringMap> getSelectorMatchLabels() {
        return this.selectorMatchLabels;
    }

    public void setSelectorMatchLabels(Set<StringStringMap> selectorMatchLabels) {
        this.selectorMatchLabels = selectorMatchLabels;
    }

    public Set<KubernetesPodSpec> getPodSpecs() {
        return this.podSpecs;
    }

    public void setPodSpecs(Set<KubernetesPodSpec> podSpecs) {
        this.podSpecs = podSpecs;
    }

    public int getMinReadySeconds() {
        return this.minReadySeconds;
    }

    public void setMinReadySeconds(int minReadySeconds) {
        this.minReadySeconds = minReadySeconds;
    }

    public int getRevisionHistorySeconds() {
        return this.revisionHistorySeconds;
    }

    public void setRevisionHistorySeconds(int revisionHistorySeconds) {
        this.revisionHistorySeconds = revisionHistorySeconds;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void setPause(boolean paused) {
        this.paused = paused;
    }

    public KubernetesDeployment name(String name) {
        setName(name);
        return this;
    }

    public KubernetesDeployment replicas(int replicas) {
        setReplicas(replicas);
        return this;
    }

    public KubernetesDeployment labels(Set<StringStringMap> labels) {
        setLabels(labels);
        return this;
    }

    public KubernetesDeployment selectorMatchLabels(Set<StringStringMap> selectorMatchLabels) {
        setSelectorMatchLabels(selectorMatchLabels);
        return this;
    }

    public KubernetesDeployment podSpecs(Set<KubernetesPodSpec> podSpecs) {
        setPodSpecs(podSpecs);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof KubernetesDeployment)) {
            return false;
        }
        KubernetesDeployment deployment = (KubernetesDeployment) o;
        return Objects.equals(name, deployment.name) &&
                replicas == deployment.replicas &&
                Objects.equals(labels, deployment.labels) &&
                Objects.equals(selectorMatchLabels, deployment.selectorMatchLabels) &&
                Objects.equals(podSpecs, deployment.podSpecs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, replicas, labels, selectorMatchLabels, podSpecs);
    }

    @Override
    public String toString() {
        return "{" +
                " name='" + getName() + "'" +
                ", replicas='" + getReplicas() + "'" +
                ", labels='" + getLabels() + "'" +
                ", labels='" + getSelectorMatchLabels() + "'" +
                ", podSpecs='" + getPodSpecs() + "'" +
                ", paused=" + isPaused() +
                ", revisionHistorySeconds=" + getRevisionHistorySeconds() +
                ", minReadySeconds=" + getMinReadySeconds() +
                "}";
    }


}
