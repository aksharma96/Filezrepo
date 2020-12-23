package utils;

public class FactoryProvider {
    public static AbstractFactory<?> getFactory(){
        
        
        return new FileFactory();
    }
}