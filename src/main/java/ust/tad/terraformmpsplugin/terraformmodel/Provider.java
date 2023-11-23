package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Provider {

    private String name;

    private Set<Argument> arguments = new HashSet<>();

    private Set<Block> blocks = new HashSet<>();
    

    public Provider() {
    }

    public Provider(String name, Set<Argument> arguments, Set<Block> blocks) {
        this.name = name;
        this.arguments = arguments;
        this.blocks = blocks;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
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

    public Provider name(String name) {
        setName(name);
        return this;
    }

    public Provider arguments(Set<Argument> arguments) {
        setArguments(arguments);
        return this;
    }

    public Provider blocks(Set<Block> blocks) {
        setBlocks(blocks);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Provider)) {
            return false;
        }
        Provider provider = (Provider) o;
        return Objects.equals(name, provider.name) && Objects.equals(arguments, provider.arguments) && Objects.equals(blocks, provider.blocks);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, arguments, blocks);
    }

    @Override
    public String toString() {
        return "{" +
            " name='" + getName() + "'" +
            ", arguments='" + getArguments() + "'" +
            ", blocks='" + getBlocks() + "'" +
            "}";
    }

    public Boolean addArgument(Argument argument) {
        return this.arguments.add(argument);
    }

    public Boolean addBlock(Block block) {
        return this.blocks.add(block);
    }

}
