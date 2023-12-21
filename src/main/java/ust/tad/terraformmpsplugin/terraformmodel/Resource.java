package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Resource {

    private String resourceType;

    private String resourceName;

    private Set<Argument> arguments = new HashSet<>();

    private Set<Block> blocks = new HashSet<>();


    public Resource() {
    }

    public Resource(String resourceType, String resourceName, Set<Argument> arguments, Set<Block> blocks) {
        this.resourceType = resourceType;
        this.resourceName = resourceName;
        this.arguments = arguments;
        this.blocks = blocks;
    }

    public String getResourceType() {
        return this.resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getResourceName() {
        return this.resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public Set<Argument> getArguments() {
        return this.arguments;
    }

    public void setArguments(Set<Argument> arguments) {
        this.arguments = arguments;
    }

    public Set<Block> getBlocks() {
        return this.blocks;
    }

    public void setBlocks(Set<Block> blocks) {
        this.blocks = blocks;
    }

    public Resource resourceType(String resourceType) {
        setResourceType(resourceType);
        return this;
    }

    public Resource resourceName(String resourceName) {
        setResourceName(resourceName);
        return this;
    }

    public Resource arguments(Set<Argument> arguments) {
        setArguments(arguments);
        return this;
    }

    public Resource blocks(Set<Block> blocks) {
        setBlocks(blocks);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Resource)) {
            return false;
        }
        Resource resource = (Resource) o;
        return Objects.equals(resourceType, resource.resourceType) && Objects.equals(resourceName, resource.resourceName) && Objects.equals(arguments, resource.arguments) && Objects.equals(blocks, resource.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resourceType, resourceName, arguments, blocks);
    }

    @Override
    public String toString() {
        return "{" +
            " resourceType='" + getResourceType() + "'" +
            ", resourceName='" + getResourceName() + "'" +
            ", arguments='" + getArguments() + "'" +
            ", blocks='" + getBlocks() + "'" +
            "}";
    }
    
    public void addArgument(Argument argument) {
        this.arguments.add(argument);
    }

    public void addArguments(Set<Argument> arguments) {
        for (Argument argument: arguments) {
            this.addArgument(argument);
        }
    }

    public void addBlock(Block block) {
        this.blocks.add(block);
    }

    
}
