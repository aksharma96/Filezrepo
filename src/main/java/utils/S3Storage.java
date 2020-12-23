package utils;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import dbUtils.DBUtil;


public class S3Storage implements StorageType {
	final String s3="S3";
	private static final Logger log = LogManager.getLogger(S3Storage.class);
	@Override
	public FileData store( FileItem fi ,Boolean overwrite) {
		FileData f = new  FileData();

		try {
			Properties prop =CommonUtils.readPropFile(this);
			String fileName=fi.getName();
			File file=CommonUtils.tempFile(fileName);
			String bucketName = prop.getProperty("s3Bucket");
			String fileObjKeyName = fileName;
			fileName=file.getName();
			File tempFile = new File( "."+File.separator+"temp"+File.separator  + UUID.randomUUID()) ;
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withRegion(prop.getProperty("region"))
					.build();
			ObjectListing objects = s3Client.listObjects(new ListObjectsRequest().withBucketName(bucketName).withPrefix(fileName));
			for (S3ObjectSummary objectSummary: objects.getObjectSummaries()) {
				if (objectSummary.getKey().equals(fileName)) {
					f.alreadyUploaded=true;
				}
			}
			if(overwrite)
			{
				if(f.alreadyUploaded)
				{
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

			if(!new File(tempFile.getParent()).exists())
			{
				new File(tempFile.getParent()).mkdirs();
			}

			fi.write(tempFile);

			PutObjectRequest request = new PutObjectRequest(bucketName, fileObjKeyName, (tempFile));
			ObjectMetadata metadata = new ObjectMetadata();
			request.setMetadata(metadata);
			s3Client.putObject(request);
			URL s3Url = s3Client.getUrl(bucketName, fileObjKeyName);
			tempFile.delete();
			log.info("Deleting Temp File");
			f.isValid=true;
			f.name=fileName;
			f.size=fi.getSize();
			f.location=s3Url.toString();
			if(!Boolean.TRUE.equals(overwrite))
				f.createdAt=new Date();
			if(Boolean.TRUE.equals(overwrite))
				f.updatedAt=new Date();

			if(Boolean.TRUE.equals(overwrite))
			{
				log.info("Trying to Overwrite File");
				if(f.alreadyUploaded)
				{
					log.info("Trying to Overwrite File");
					Connection con = DBUtil.createConnectionFileDB(s3);
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
						log.info("updated DB:"+fileName+new Date().toString());
						return data;
					}
					else {
						log.info("Trying to add entry in DB");
						Connection con1 = DBUtil.createConnectionFileDB(s3);
						Statement statement1 = con1.createStatement();
						statement1.setQueryTimeout(30);
						UUID uuid = UUID.randomUUID();
						f.uid=uuid;
						f.updatedAt=new Date();
						f.isValid=true;
						f.createdAt=new Date();
						PreparedStatement st = con1.prepareStatement("insert into file values(?,?,?,?,?,?)");
						st.setString(1, uuid.toString());
						st.setString(2, f.name);
						st.setString(3, f.createdAt.toString());
						st.setString(4, f.location);
						st.setLong(5, f.size);
						st.setString(6, f.updatedAt.toString());
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
					Connection con = DBUtil.createConnectionFileDB(s3);
					Statement statement = con.createStatement();
					statement.setQueryTimeout(30);
					UUID uuid = UUID.randomUUID();
					f.uid=uuid;
					f.updatedAt=new Date();
					f.isValid=true;
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
				else
				{
					//already uploaded
					f.alreadyUploaded=true;
					f.isValid=false;
					//delete the entry
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return f;
		}

		return f;
	}

	private FileData getDetailsByName(String fileName) {
		Connection con;
		try {
			con = DBUtil.createConnectionFileDB(s3);
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
				ob=new FileData(rs.getString("name"),rs.getString("uid"),rs.getString("location"),rs.getString("created"),rs.getInt("size"));

			}
			Properties prop =CommonUtils.readPropFile(this);
			String bucketName = prop.getProperty("s3Bucket");
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withRegion(prop.getProperty("region"))
					.build();
			log.info("Showing "+fileName+" under bucket"+bucketName);
			S3Object fileObject = s3Client.getObject(new GetObjectRequest(bucketName, fileName));         
			ob.updatedAt=( fileObject.getObjectMetadata().getLastModified());
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
			con = DBUtil.createConnectionFileDB(s3);

			Statement statement = con.createStatement();
			statement.setQueryTimeout(30);
			log.info("Request to get info related to:"+uid);
			PreparedStatement st = con.prepareStatement("select * from file where uid=?");
			st.setString(1, uid);
			ResultSet rs = st.executeQuery();
			FileData ob = null;
			while(rs.next())
			{
				ob=new FileData(rs.getString("name"),rs.getString("uid"),rs.getString("location"),rs.getString("created"),rs.getInt("size"),rs.getString("updated"));
				try {
					Properties prop = CommonUtils.readPropFile(this);
					String bucketName = prop.getProperty("s3Bucket");
					AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
							.withRegion(prop.getProperty("region"))
							.build();
					log.info("fetching  modified date fromS bucket"+bucketName+"\\"+ob.name);
					S3Object fileObject = s3Client.getObject(new GetObjectRequest(bucketName, ob.name)); 
					log.info(fileObject.getObjectMetadata().getLastModified());
				}catch( Exception e)
				{
					e.printStackTrace();
				}
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
			con = DBUtil.createConnectionFileDB("s3");
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
		}
		return null;

	}

	@Override
	public boolean showFile(String location, HttpServletResponse response) {
		File file;
		try {
			Properties prop = CommonUtils.readPropFile(this);
			String bucketName = prop.getProperty("s3Bucket");
			String fileName=location;
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withRegion(prop.getProperty("region"))
					.build();
			log.info("Showing "+fileName+" under bucket"+bucketName);
			S3Object fileObject = s3Client.getObject(new GetObjectRequest(bucketName, fileName));         
			InputStream in = new BufferedInputStream(fileObject.getObjectContent());

			ByteArrayOutputStream out = new ByteArrayOutputStream();

			byte[] buf = new byte[1024];

			int n = 0;

			while (-1 != (n = in.read(buf))) {
				out.write(buf, 0, n);
			}

			out.close();
			in.close();
			byte[] d = out.toByteArray();
			file = new File("./" + fileName);
			FileOutputStream fos = new FileOutputStream("./" + fileName);
			fos.write(d);
			fos.close();
			response.setHeader("Content-Disposition", "filename=\""+fileName+"\"");
			FileUtils.copyFile(file, response.getOutputStream());
			file.delete();
			return true;
		}catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}


	@Override
	public FileData update(String uid, FileData fileData) {
		Properties prop = new Properties();
		try {
			prop=CommonUtils.readPropFile(this);
			String bucketName = prop.getProperty("s3Bucket");
			Connection con = DBUtil.createConnectionFileDB(s3);
			if(uid==null||uid.isEmpty())
			{
				return null;
			}
			String newName=fileData.name;
			FileData data=getDetails(uid);
			if(data==null)
			{
				log.info("got uid null");
				return null;
			}
			if(fileData.name!=null&&!fileData.name.isEmpty())
			{
				File file=CommonUtils.storeFile(newName);
				newName=file.getName();
				try {
					File file1=CommonUtils.storeFile(data.name);
					AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
							.withRegion(prop.getProperty("region"))
							.build();
					ObjectMetadata meta;
					try {
						meta=s3Client.getObject(new GetObjectRequest(bucketName, newName)).getObjectMetadata();
						if(!meta.getLastModified().toString().isEmpty())
						{
							log.info("File uploaded already");
							fileData.isValid=false;
							fileData.alreadyUploaded=true;
							return fileData;
						}
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}


					CopyObjectRequest copyObjRequest = new CopyObjectRequest(bucketName, 
							file1.getName(), bucketName, newName);
					s3Client.copyObject(copyObjRequest);
					s3Client.deleteObject(new DeleteObjectRequest(bucketName, file1.getName()));
					Statement statement = con.createStatement();
					statement.setQueryTimeout(30);
					log.info("Request to update info related to:"+uid+". Param to update:"+newName);
					PreparedStatement st = con.prepareStatement("update file set name=?,updated=?,location=? where uid=?");
					st.setString(1, newName);
					st.setString(2,new Date().toString());
					URL s3Url = s3Client.getUrl(bucketName, newName);
					st.setString(3, s3Url.toString());
					st.setString(4, uid);
					st.executeUpdate();
					fileData.updatedAt=new Date();
					log.info("File updated sucessfully");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			con.close();
		} catch (Exception e) {
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
		FileData data = getDetails(uid);
		//f.isValid=false;
		try {
			Properties prop =CommonUtils.readPropFile(this);
			String bucketName = prop.getProperty("s3Bucket");
			String fileName=data.name;
			log.info("Request to remove file from s3");
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withRegion(prop.getProperty("region"))
					.build();
			try {
				S3Object fileObject = s3Client.getObject(new GetObjectRequest(bucketName, fileName));   

				data.updatedAt=fileObject.getObjectMetadata().getLastModified();
				s3Client.deleteObject(new DeleteObjectRequest(bucketName, fileName));
				log.info("deleted "+fileName+" under bucket"+bucketName);

			}catch(AmazonS3Exception s1)
			{
				s1.printStackTrace();
				if(!s1.getErrorMessage().contains("The specified key does not exist"))
					return data;
			}
			Connection con;
			con = DBUtil.createConnectionFileDB(s3);
			PreparedStatement st = con.prepareStatement("delete from file where uid=?");

			st.setString(1, uid);
			st.execute();
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
		// TODO Auto-generated method stub
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
