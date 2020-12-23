package utils;

import java.sql.SQLException;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;


public interface StorageType {

	FileData store(FileItem fi,Boolean overwrite) throws SQLException, ClassNotFoundException;
	FileData getDetails(String uid);
	FileData update(String uid, FileData fileData) throws ClassNotFoundException, SQLException;
	FileData[] getAllDetails();
	boolean showFile(String location, HttpServletResponse response);
	FileData delete(String uid);
	boolean delete(String fileName, boolean b);
	FileData store(String uid, FileItem fi, boolean b);
	
}
