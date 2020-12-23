package utils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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


public class DBStore implements StorageType {
	final String store="dbstore";
	private static final Logger log = LogManager.getLogger(DBStore.class);
	@Override
	public FileData store( FileItem fi ,Boolean overwrite) {
		// TODO Auto-generated method stub
		FileData f = new  FileData();
		File file;
		try {
			String fileName=fi.getName();
			 file=CommonUtils.tempFile(fileName);
			log.info("trying to store file in DB");
			if(!new File(file.getParent()).exists())
			{
				new File(file.getParent()).mkdirs();
			}
			fileName=file.getName();
			log.info("trying to store file in DB.NAme:"+fileName);
			FileData datafile = getDetailsByName(fileName);
			if(datafile!=null)
			{
				f.alreadyUploaded=true;
			}
			if(Boolean.TRUE.equals(overwrite))
			{
				log.info("Overwriting");
				if(f.alreadyUploaded)
				{
					log.info("Already Uploaded a valid file");
					f.alreadyUploaded=true;
					f.isValid=true;
				}
				else {
					log.info("Overwrite is set,But file is not found");
					log.info("Overwrite flag was set and file was not found to be replaced. Returning back");
					FileData data = new FileData();
					data.isValid=false;
					data.alreadyUploaded=false;
					return data;
				}
			}
			
			fi.write(file);
			byte[] fData = Files.readAllBytes(file.toPath());
			f.isValid=true;
			f.name=fileName;
			f.size=fi.getSize();
			f.location="Local_DB";
			if(!Boolean.TRUE.equals(overwrite))
				f.createdAt=new Date();

			if(Boolean.TRUE.equals(overwrite))
			{
				log.info("Trying to Overwrite File");
				if(f.alreadyUploaded)
				{
					log.info("Trying to Overwrite File");
					Connection con = DBUtil.createConnectionDB();
					Statement statement = con.createStatement();
					statement.setQueryTimeout(30);
					FileData data = getDetailsByName(fileName);
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
					else {
						log.info("Trying to add entry in DB2");
						Connection con1 = DBUtil.createConnectionDB();
						Statement statement1 = con1.createStatement();
						statement1.setQueryTimeout(30);
						UUID uuid = UUID.randomUUID();
						f.uid=uuid;
						f.updatedAt=new Date();
						f.isValid=true;
						f.createdAt=new Date();
						System.out.println("insert into file values("+uuid+",'"+fileName+"',"+f.createdAt+",'"+f.location+"',"+f.size+")");
						statement1.executeUpdate("insert into file values('"+uuid+"','"+f.name+"','"+f.createdAt+"','"+f.location+"',"+f.size+",'"+f.updatedAt+"',)");

						PreparedStatement st = con1.prepareStatement("insert into file values(?,?,?,?,?,?,?)");
						st.setString(1, uuid.toString());
						st.setString(2, f.name);
						st.setString(3, f.createdAt.toString());
						st.setString(4, f.location);
						st.setLong(5, f.size);
						st.setString(6, f.updatedAt.toString());
						st.setBytes(7, fData);
					//	InputStream inputStream = new FileInputStream(file);
					//	st.setBlob(7, inputStream);
						st.execute();
						con1.close();
					}
				}

			}
			else {
				log.info("Trying to upload a new file");
				if(! f.alreadyUploaded)
				{

					log.info("Trying to add entry in DB");
					Connection con = DBUtil.createConnectionDB();
					Statement statement = con.createStatement();
					statement.setQueryTimeout(30);
					UUID uuid = UUID.randomUUID();
					f.uid=uuid;
					f.updatedAt=new Date();
					f.isValid=true;
					PreparedStatement st = con.prepareStatement("insert into file values(?,?,?,?,?,?,?)");
					st.setString(1, uuid.toString());
					st.setString(2, f.name);
					st.setString(3, f.createdAt.toString());
					st.setString(4, f.location);
					st.setLong(5, f.size);
					st.setString(6, f.updatedAt.toString());
					st.setBytes(7, fData);
					st.execute();
					con.close();
				}
				else
				{
					//already uploaded
					f.alreadyUploaded=true;
					f.isValid=false;
					//delete the entry
				}
			}
			file.delete();
		}catch(FileExistsException ex)
		{
			ex.printStackTrace();
			f.alreadyUploaded=true;
			return f;

		}
		catch (Exception e) {
			e.printStackTrace();
			return f;
		}
		file.delete();
		return f;
	}

	private FileData getDetailsByName(String fileName) {
		Connection con;
		try {
			con = DBUtil.createConnectionDB();

			Statement statement = con.createStatement();
			statement.setQueryTimeout(30);
			log.info("Request to get info related to:"+fileName);
			PreparedStatement st = con.prepareStatement("select * from file where name=?");
			st.setString(1, fileName);
			ResultSet rs = st.executeQuery();
			FileData ob = null;
			while(rs.next())
			{
				log.info("Got match from DB");
				System.out.println(rs.getString("created"));
				ob=new FileData(rs.getString("name"),rs.getString("uid"),rs.getString("location"),rs.getString("created"),rs.getInt("size"));

			}
			// System.out.println(Upload.context.getContextPath());
			log.info("Showing "+fileName);
			if(ob!=null)
			{
				log.info("Request to get info related to:"+fileName+" success.");
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


		Connection con;
		try {
			con = DBUtil.createConnectionDB();

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
		// TODO Auto-generated method stub
		Connection con;
		try {
			con = DBUtil.createConnectionDB();

			Statement statement = con.createStatement();
			statement.setQueryTimeout(30);

			ResultSet rs1 = statement.executeQuery("SELECT COUNT(*) FROM file");
			FileData[] arr=new FileData[rs1.getInt(1)];
			ResultSet rs = statement.executeQuery("select * from file");

			while(rs.next())
			{
				// read the result set

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
	public boolean showFile(String name, HttpServletResponse response) {
		try {
			FileData data = getDetailsByName(name);
			
			Connection conn = DBUtil.createConnectionDB();
			String sql = "SELECT * from file where uid=?"; 
	        PreparedStatement stmt = conn.prepareStatement(sql); 
	        stmt.setString(1, data.uid.toString());
	        ResultSet resultSet = stmt.executeQuery();
	        byte[] is = null;
	        File shl = null;
	        while (resultSet.next()) { 
	           shl = new File("./temp/"+data.uid.toString()); 
	           is = resultSet.getBytes("file");
	           FileOutputStream out = new FileOutputStream( shl );
	           out.write( is );
	           out.close();
	        } 
	        response.setHeader("Content-Disposition", "filename=\""+data.name+"\"");
			FileUtils.copyFile(shl, response.getOutputStream());
			shl.delete();
			conn.close(); 
			return true;
		}catch (Exception e) {
		
			e.printStackTrace();

		}

		return false;
	}


	@Override
	public FileData update(String uid, FileData fileData) {
		try {
			Connection con = DBUtil.createConnectionDB();
			if(uid==null||uid.isEmpty())
			{
				return null;
			}
			String fileName=fileData.name;
			FileData data=getDetails(uid);
			log.info("got uid dta"+data);
			if(data==null)
			{
				log.info("got uid null");
				return null;
			}

			if(fileData.name!=null&&!fileData.name.isEmpty())
			{
				File file=CommonUtils.storeFile(fileName);
				fileName=file.getName();
				try {
					//File file1=CommonUtils.storeFile(data.name);
					Statement statement = con.createStatement();
					statement.setQueryTimeout(30);
					log.info("Request to update info related to:"+uid+". Param to update:"+fileName);
					PreparedStatement st = con.prepareStatement("update file set name=?,updated=? where uid=?");
					st.setString(1, fileName);
					st.setString(2, new Date().toString());
				//	st.setBytesg(3, );
					st.setString(3, uid);
					st.executeUpdate();
					log.info("File updated sucessfully");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			con.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			//con.close();
		}

		return fileData;
	}

	@Override
	public boolean  delete(String uid,boolean b) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public FileData delete(String uid) {
		// TODO Auto-generated method stub
		FileData data = getDetails(uid);
		//f.isValid=false;
		try {
			String fileName=data.name;
			Connection con;
			con = DBUtil.createConnectionDB();
			PreparedStatement st = con.prepareStatement("delete from file where uid=?");

			st.setString(1, uid);
			System.out.println( st.execute());
			con.close();
			log.info("deleted "+fileName+" entry in DB");
		}catch (Exception e) {
			e.printStackTrace();
			return data;

		}
		return data;
	}

	@Override
	public FileData store(String uid, FileItem fi, boolean b) {
		try {
			FileData data = getDetails(uid);
			File f;
			if( fi.getName().lastIndexOf(File.separator) >= 0 ) {
				f = new File(fi.getName().substring( fi.getName().lastIndexOf(File.separator))) ;
			} else {
				f =new File(fi.getName().substring(fi.getName().lastIndexOf(File.separator)+1)) ;
			}
			data.name=f.getPath();
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
