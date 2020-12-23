package utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import dbUtils.DBUtil;


public class FileStorage implements StorageType {

	private static final Logger log = LogManager.getLogger(FileStorage.class);
	final String TYPE_NAME="local";
	@Override
	public FileData store( FileItem fi ,Boolean overwrite) throws SQLException, ClassNotFoundException {
		// TODO Auto-generated method stub
		FileData f = new  FileData();
		Connection con = DBUtil.createConnectionFileDB(TYPE_NAME);
		//f.isValid=false;
		
		String fileName=fi.getName();
		
		File file=CommonUtils.storeFile(fileName);
		if(!new File(file.getParent()).exists())
		{
			new File(file.getParent()).mkdirs();
		}
		fileName=file.getName();
		try {
			if(Boolean.TRUE.equals(overwrite))
			{
				if(file.exists())
				{
					file.delete();
					f.alreadyUploaded=true;
					f.isValid=true;
				}
				else {
					log.info("Overwrite flag was set and file was not found to be replaced. Returning back");
					FileData data = new FileData();
					data.isValid=false;
					data.alreadyUploaded=false;
					return data;
				}
			}

			fi.write( file ) ;

			f.isValid=true;
			f.name=fileName;
			f.size=fi.getSize();
			f.location="LOCAL";
			if(!Boolean.TRUE.equals(overwrite))
				f.createdAt=new Date();
			f.updatedAt=new Date();
			if(!Boolean.TRUE.equals(overwrite))
				f.alreadyUploaded=false;


			if(!Boolean.TRUE.equals(overwrite))
			{
				UUID uuid = UUID.randomUUID();
				f.uid=uuid;
				f.updatedAt=new Date();
				f.isValid=true;
				Statement statement = con.createStatement();
				statement.setQueryTimeout(30);
				PreparedStatement st = con.prepareStatement("insert into file values(?,?,?,?,?,?)");
				st.setString(1, uuid.toString());
				st.setString(2, f.name);
				st.setString(3, f.createdAt.toString());
				st.setString(4, f.location);
				st.setLong(5, f.size);
				st.setString(6, f.updatedAt.toString());
				st.execute();
				con.close();
			}
			else {

				if(file.exists())
				{

					Statement statement = con.createStatement();
					statement.setQueryTimeout(30);
					FileData data = getDetailsByName(file.getName());
					if(data==null)
					{
						data=new FileData();
						data.isValid=false;
						data.alreadyUploaded=false;
						//return data;
					}
					if(data.isValid)
					{
					log.info("Request to update info related to:"+data.uid);
					PreparedStatement st = con.prepareStatement("update file set name=?,updated=? where uid=?");
					st.setString(1, fileName);
					st.setString(2, new Date().toString());
					st.setString(3, data.uid.toString());
					st.execute();
					con.close();
					return data;
					}
					else
					{
					log.info("Request to update info related to:"+data.uid);
					PreparedStatement st = con.prepareStatement("update file set name=?,updated=? where uid=?");
					st.setString(1, fileName);
					st.setString(2, new Date().toString());
					st.setString(3, data.uid.toString());

					con.close();
					return data;
					}
				}
			}
		}catch(FileExistsException ex)
		{
			f.alreadyUploaded=true;
			f.isValid=false;
			ex.printStackTrace();
			return f;
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		con.close();
		return f;
	}

	@SuppressWarnings("deprecation")
	public FileData getDetailsByName(String name) {
		// TODO Auto-generated method stub
		Connection con;
		try {
			con = DBUtil.createConnectionFileDB(TYPE_NAME);

			Statement statement = con.createStatement();
			statement.setQueryTimeout(30);
			log.info("Request to get info related to:"+name);
			PreparedStatement st = con.prepareStatement("select * from file where name=?");
			st.setString(1, name);
			ResultSet rs = st.executeQuery();
			FileData ob = null;
			while(rs.next())
			{
				log.info("Got match from DB");
				ob=new FileData(rs.getString("name"),rs.getString("uid"),rs.getString("location"),rs.getString("created"),rs.getInt("size"));

			}
			File file=CommonUtils.storeFile(ob.name);
			Path pFile = Paths.get(file.getAbsolutePath());
			  BasicFileAttributes attr = Files.readAttributes(pFile, BasicFileAttributes.class);
			  System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
			ob.updatedAt=new Date(attr.lastModifiedTime().toString());
			if(ob!=null)
			{
				log.info("Request to get info related to:"+name+" success.");
				return ob;
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public FileData getDetails(String uid) {
		// TODO Auto-generated method stub
		Connection con;
		try {
			con = DBUtil.createConnectionFileDB(TYPE_NAME);
			Statement statement = con.createStatement();
			statement.setQueryTimeout(30);
			log.info("Request to get info related to:"+uid);
			PreparedStatement st = con.prepareStatement("select * from file where uid=?");
			st.setString(1, uid);
			ResultSet rs = st.executeQuery();
			FileData ob = null;
			while(rs.next())
			{
				ob=new FileData(rs.getString("name"),rs.getString("uid"),rs.getString("location"),rs.getString("created"),rs.getInt("size"));

			}
			log.info("Request to get info related to:"+uid+" success.");
			return ob;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return null;

	}

	@Override
	public FileData[] getAllDetails() {
		Connection con;
		try {
			con = DBUtil.createConnectionFileDB(TYPE_NAME);
			Statement statement = con.createStatement();
			statement.setQueryTimeout(30);
			ResultSet rs1 = statement.executeQuery("SELECT COUNT(*) FROM file");
			FileData[] arr=new FileData[rs1.getInt(1)];
			ResultSet rs = statement.executeQuery("select * from file");
			while(rs.next())
			{
				FileData ob=new FileData(rs.getString("name"),rs.getString("uid"),rs.getString("location"),rs.getString("created"),rs.getInt("size"));
				arr[rs.getRow()-1]=ob;

			}
			return arr;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		

	}

	@Override
	public boolean showFile(String fileName, HttpServletResponse response) {
		response.setContentType("application/octet-stream");
		try {
			File file;
			if( fileName.lastIndexOf(File.separator) >= 0 ) {
				file = new File( "."+File.separator+"store"+File.separator  + fileName.substring( fileName.lastIndexOf(File.separator))) ;
			} else {
				file = new File( "."+File.separator+"store"+File.separator + fileName.substring(fileName.lastIndexOf(File.separator)+1)) ;
			}
			response.setHeader("Content-Disposition", "filename=\""+fileName+"\"");

			FileUtils.copyFile(file, response.getOutputStream());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	public FileData update(String uid, FileData fileData) throws ClassNotFoundException, SQLException {
		Connection con = DBUtil.createConnectionFileDB(TYPE_NAME);
		try {
			if(uid==null||uid.isEmpty())
			{
				return null;
			}
			String fileName=fileData.name;
			FileData data=getDetails(uid);
			
			if(data==null)
			{
				log.info("got uid null");
				return null;
			}
			log.info("got uid dta"+data.name);
			if(fileData.name!=null&&!fileData.name.isEmpty())
			{
				File newFile=CommonUtils.storeFile(fileName);
				fileName=newFile.getName();
				Statement statement = con.createStatement();
				statement.setQueryTimeout(30);
				log.info("Request to update info related to:"+uid+". Param to update:"+fileName);
				PreparedStatement st = con.prepareStatement("update file set name=?,updated=? where uid=?");
				st.setString(1, fileName);
				st.setString(2, new Date().toString());
				st.setString(3, uid);
				st.executeUpdate();
				try {
					File oldFile=CommonUtils.storeFile(data.name);
					log.info("renaming to "+newFile.getAbsolutePath());
					if(newFile.exists())
					{
						log.info("File already present");
						fileData.isValid=false;
						fileData.alreadyUploaded=true;
						return fileData;
					}
					else {
					oldFile.renameTo(newFile);

					log.info("File updated sucessfully");
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			con.close();
		} catch (Exception e) {
			e.printStackTrace();
			con.close();
		}

		return fileData;
	}

	@Override
	public FileData delete(String uid) {
		FileData data = getDetails(uid);
		try {
		File file= new File( "."+File.separator+"store"+File.separator +  data.name) ;
			
			log.info("Deleting File:"+file.getAbsolutePath()+" With UID:"+uid);
		
			if(file.exists())
			{
				file.delete();
				
			}
			else {
				log.error("Unable to find file:"+file.getAbsolutePath()+" With UID:"+uid);
			}
			Connection con;
			con = DBUtil.createConnectionFileDB(TYPE_NAME);
			PreparedStatement st = con.prepareStatement("delete from file where uid=?");
			
			st.setString(1, uid);
			st.execute();
			con.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return data;
	}

	@Override
	public boolean delete(String fileName, boolean b) {

		File file=CommonUtils.storeFile(fileName);
		
		if(file.exists())
		{
			log.info("Deleting filke at:"+file.getAbsolutePath());
			file.delete();
		}
		return true;
	}

	@Override
	public FileData store(String uid, FileItem fi, boolean b) {

		try {
			FileData data = getDetails(uid);
			String fileName;
			if( fi.getName().lastIndexOf(File.separator) >= 0 ) {
				fileName = fi.getName().substring( fi.getName().lastIndexOf(File.separator)) ;
			} else {
				fileName =fi.getName().substring(fi.getName().lastIndexOf(File.separator)+1) ;
			}
			data.name=fileName;
			data=update(uid, data);
			
			store(fi, true);
			return data;
		}catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
		
	}

}
