package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.*;
import java.util.stream.Collectors;

public class Block {

  private String blockType;

  private Set<Block> nestedBlocks = new HashSet<>();

  private Set<Argument> arguments = new HashSet<>();

  public Block() {}

  public Block(String blockType, Set<Block> nestedBlocks, Set<Argument> arguments) {
    this.blockType = blockType;
    this.nestedBlocks = nestedBlocks;
    this.arguments = arguments;
  }

  public String getBlockType() {
    return this.blockType;
  }

  public void setBlockType(String blockType) {
    this.blockType = blockType;
  }

  public Set<Block> getNestedBlocks() { return this.nestedBlocks; }

  public void setNestedBlocks(Set<Block> nestedBlocks) { this.nestedBlocks = nestedBlocks; }

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

  public Block nestedBlocks(Set<Block> nestedBlocks) {
    setNestedBlocks(nestedBlocks);
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
    return Objects.equals(blockType, block.blockType) &&
            Objects.equals(nestedBlocks, block.nestedBlocks) &&
            Objects.equals(arguments, block.arguments);
  }

  @Override
  public int hashCode() {
    return Objects.hash(blockType, nestedBlocks, arguments);
  }

  @Override
  public String toString() {
    return "{"
        + " blockType='"
        + getBlockType()
        + "'"
        + ", nestedBlocks='"
        + getNestedBlocks()
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

  public Block findNestedBlockByBlockType(List<String> nestedBlockTypes) throws BlockNotFoundException {
    String nestedBlockType = nestedBlockTypes.get(0);
    Optional<Block> block =
            this.getNestedBlocks().stream().filter(nestedBlock -> nestedBlock.getBlockType().equals(nestedBlockType)).findFirst();
    if (block.isPresent()) {
      if (nestedBlockTypes.size() == 1) {
        return block.get();
      } else {
        return block.get().findNestedBlockByBlockType(nestedBlockTypes.subList(1,
                nestedBlockTypes.size()));
      }
    } else {
      throw new BlockNotFoundException("Nested block with type " + blockType + " not found in " + "block " + this.getBlockType());
    }
  }

  public List<Block> findAllNestedBlocksByBlockType(String nestedBlockType) throws BlockNotFoundException {
    List<Block> blocks =
            this.getNestedBlocks().stream().filter(nestedBlock -> nestedBlock.getBlockType().equals(nestedBlockType)).collect(Collectors.toList());
    if (!blocks.isEmpty()) {
      return blocks;
    } else {
      throw new BlockNotFoundException("Nested blocks with type " + blockType + " not found in " + "block " + this.getBlockType());
    }
  }

  public Argument findArgumentByIdentifier(String identifier) throws ArgumentNotFoundException {
    Optional<Argument> argument =
            this.getArguments().stream().filter(blockArgument -> blockArgument.getIdentifier().equals(identifier)).findFirst();
    if (argument.isPresent()) {
      return argument.get();
    } else
      throw new ArgumentNotFoundException("Block with type " + blockType + " does not have an " + "argument with identifier " + identifier);
  }

}
