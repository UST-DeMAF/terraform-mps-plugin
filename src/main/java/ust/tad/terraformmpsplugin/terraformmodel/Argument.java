package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.Objects;

public class Argument {

  private String identifier;

  private String expression;

  public Argument() {}

  public Argument(String identifier, String expression) {
    this.identifier = identifier;
    this.expression = expression;
  }

  public String getIdentifier() {
    return this.identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public String getExpression() {
    return this.expression;
  }

  public void setExpression(String expression) {
    this.expression = expression;
  }

  public Argument identifier(String identifier) {
    setIdentifier(identifier);
    return this;
  }

  public Argument expression(String expression) {
    setExpression(expression);
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) return true;
    if (!(o instanceof Argument)) {
      return false;
    }
    Argument argument = (Argument) o;
    return Objects.equals(identifier, argument.identifier)
        && Objects.equals(expression, argument.expression);
  }

  @Override
  public int hashCode() {
    return Objects.hash(identifier, expression);
  }

  @Override
  public String toString() {
    return "{"
        + " identifier='"
        + getIdentifier()
        + "'"
        + ", expression='"
        + getExpression()
        + "'"
        + "}";
  }
}
