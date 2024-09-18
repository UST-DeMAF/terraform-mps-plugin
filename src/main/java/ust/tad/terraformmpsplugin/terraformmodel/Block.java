package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Block {

  private String blockType;

  private Set<Argument> arguments = new HashSet<>();

  public Block() {}

  public Block(String blockType, Set<Argument> arguments) {
    this.blockType = blockType;
    this.arguments = arguments;
  }

  public String getBlockType() {
    return this.blockType;
  }

  public void setBlockType(String blockType) {
    this.blockType = blockType;
  }

  public Set<Argument> getArguments() {
    return this.arguments;
  }

  public void setArguments(Set<Argument> arguments) {
    this.arguments = arguments;
  }

  public Block blockType(String blockType) {
    setBlockType(blockType);
    return this;
  }

  public Block arguments(Set<Argument> arguments) {
    setArguments(arguments);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Block)) {
      return false;
    }
    Block block = (Block) o;
    return Objects.equals(blockType, block.blockType) && Objects.equals(arguments, block.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blockType, arguments);
  }

  @Override
  public String toString() {
    return "{"
        + " blockType='"
        + getBlockType()
        + "'"
        + ", arguments='"
        + getArguments()
        + "'"
        + "}";
  }

  public void addArgument(Argument argument) {
    this.arguments.add(argument);
  }

  public void addArguments(Set<Argument> arguments) {
    for (Argument argument : arguments) {
      this.addArgument(argument);
    }
  }
}
