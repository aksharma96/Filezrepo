package utils;

public class FileFactory implements AbstractFactory<StorageType> {

	public static String FILE_STORAGE="1";
	public static String S3_STORAGE="2";
	public static  String DB_STORAGE="3";
	@Override
    public StorageType create(String storageType) {
		
		try {
        if (FILE_STORAGE.equalsIgnoreCase(storageType)) {
            return new FileStorage();
        } else if (S3_STORAGE.equalsIgnoreCase(storageType)) {
        	 return new S3Storage();
        }
        else if (DB_STORAGE.equalsIgnoreCase(storageType)) {
       	 return new DBStore();
       }
        else {
        	//default
        	 return new FileStorage();
        }

        
    
	}catch(Exception e)
	{
		e.printStackTrace();
		System.out.println("Unable to create storage instance, Please make sure you have specified the right storage type in config.properties");
	}
		return null;
	}
}