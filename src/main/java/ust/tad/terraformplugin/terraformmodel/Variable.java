package ust.tad.terraformplugin.terraformmodel;

import java.util.Objects;

public class Variable {
    
    private String identifier;

    private String expression;


    public Variable() {
    }

    public Variable(String identifier, String expression) {
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

    public Variable identifier(String identifier) {
        setIdentifier(identifier);
        return this;
    }

    public Variable expression(String expression) {
        setExpression(expression);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Variable)) {
            return false;
        }
        Variable variable = (Variable) o;
        return Objects.equals(identifier, variable.identifier) && Objects.equals(expression, variable.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, expression);
    }

    @Override
    public String toString() {
        return "{" +
            " identifier='" + getIdentifier() + "'" +
            ", expression='" + getExpression() + "'" +
            "}";
    }

    
}
