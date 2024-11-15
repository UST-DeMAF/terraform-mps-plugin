package ust.tad.terraformmpsplugin.terraformmodel;

import java.util.Objects;

public class Provider {

    private String name;

    public Provider() {}

    public Provider(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Provider provider = (Provider) o;
        return Objects.equals(name, provider.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "{"
                + " name='"
                + getName()
                + "'"
                + "}";
    }
}
