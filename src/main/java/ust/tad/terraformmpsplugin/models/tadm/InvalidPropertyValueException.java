package ust.tad.terraformmpsplugin.models.tadm;

public class InvalidPropertyValueException extends Exception{
    public InvalidPropertyValueException(String errorMessage) {
        super(errorMessage);
    }    
}
