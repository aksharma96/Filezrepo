package utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class FileData {

	
	public FileData(String name, String uid, String location, String created, int size) throws ParseException {
		// TODO Auto-generated constructor stub
		
		this.isValid=true;
		DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
		Date date = (Date)formatter.parse(created);
		this.createdAt=date;
		this.size=(size);
		this.location=location;
		this.uid=UUID.fromString(uid);
		this.name=name;
		this.updatedAt=new Date();
	}
	
	public FileData(String name, String uid, String location, Date created, int size) throws ParseException {
		// TODO Auto-generated constructor stub
		
		this.isValid=true;
		Date date = (created);
		this.createdAt=date;
		this.size=(size);
		this.location=location;
		this.uid=UUID.fromString(uid);
		this.name=name;
		this.updatedAt=new Date();
	}
	public FileData(String name, String uid, String location, String created, int size,String updated) throws ParseException {
		// TODO Auto-generated constructor stub
		
		this.isValid=true;
		DateFormat formatter = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
		//DateFormat formatter1 = new SimpleDateFormat("E MMM dd HH:mm:ss Z yyyy");
		Date date = (Date)formatter.parse(created);
		this.createdAt=date;
		this.size=(size);
		this.location=location;
		this.uid=UUID.fromString(uid);
		this.name=name;
		this.updatedAt=(Date)formatter.parse(updated);
		
	}
	
	public String toString()
	{
		
		//this.createdAt=new Date(this.createdAt.toString());
		//this.updatedAt=new Date(this.updatedAt.toString());
		this.notes+="Size="+ FileUtils.byteCountToDisplaySize(this.size);
		String json = new GsonBuilder()
				.excludeFieldsWithoutExposeAnnotation()
				.create().toJson(this);
		
		return json;
		
	}
	public FileData() {
		// TODO Auto-generated constructor stub
		
	}
	public boolean isValid=false;
	@Expose
	public String name;
	@Expose
	public long size;
	
	@Expose
	public Date createdAt;
	
	@Expose
	public Date updatedAt;
	@Expose
	public String location="";
	
	public boolean alreadyUploaded=false;
	@Expose
	public UUID uid;
	
	@Expose
	public String notes;
}
