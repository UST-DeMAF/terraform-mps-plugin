package ust.tad.terraformmpsplugin.models.tadm;

public class InvalidRelationException extends Exception{
    public InvalidRelationException(String errorMessage) {
        super(errorMessage);
    }
}
