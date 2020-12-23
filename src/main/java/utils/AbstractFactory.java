package utils;

public interface AbstractFactory<T> {
	StorageType create(String type) ;
}