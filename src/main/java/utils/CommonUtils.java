package utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

public class CommonUtils {

	CommonUtils()
	{

	}

	public static File tempFile(String fileName)
	{
		File file;
		if( fileName.lastIndexOf(File.separator) >= 0 ) {
			file = new File( "."+File.separator+"temp"+File.separator  + fileName.substring( fileName.lastIndexOf(File.separator))) ;
		} else {
			file = new File( "."+File.separator+"temp"+File.separator + fileName.substring(fileName.lastIndexOf(File.separator)+1)) ;
		}
		return file;
	}


	public static File storeFile(String fileName)
	{
		File file;
		if( fileName.lastIndexOf(File.separator) >= 0 ) {
			file = new File( "."+File.separator+"temp"+File.separator  + fileName.substring( fileName.lastIndexOf(File.separator))) ;
		} else {
			file = new File( "."+File.separator+"temp"+File.separator + fileName.substring(fileName.lastIndexOf(File.separator)+1)) ;
		}
		return file;
	}
	public static Properties	readPropFile(StorageType s)
	{
		Properties prop = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(s.getClass().getClassLoader().getResource("").getPath()+File.separator+".."+File.separator+"/config/prop.properties");
			prop.load(fis);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;

		}
		return 	prop;
	}


	public static boolean checkSetup(StorageType t)
	{
		try {
			Properties prop =readPropFile(t);
			System.out.println("request to check validity of setup"+prop.getProperty("storageType"));
			if(FileFactory.S3_STORAGE.trim().toString().trim().equals(prop.getProperty("storageType")  ))
			{
				System.out.println("setup is s3");
				AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
						.withRegion(prop.getProperty("region"))
						.build();
				
				s3Client.listObjects(prop.getProperty("s3Bucket")).getBucketName();
			}
			System.out.println(prop.getProperty("allowedExtensions").isEmpty());
			System.out.println(prop.getProperty("allowedMime").isEmpty());
			System.out.println(prop.getProperty("maxFileSize").isEmpty());
			System.out.println(prop.getProperty("storageType").isEmpty());
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}