package store;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase.SizeLimitExceededException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import utils.CommonUtils;
import utils.FactoryProvider;
import utils.FileData;
import utils.StorageType;

/**
 * Servlet implementation class upload
 */
@WebServlet({ "/api/v1/operate", "/api/v1/upload" })
public class Upload extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static ServletContext context;
	private static final Logger log = LogManager.getLogger(Upload.class);
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Upload() {
		super();
		// TODO Auto-generated constructor stub
	}
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			 context=getServletContext();
			 
			Properties prop = new Properties();
			prop.load(getServletContext().getResourceAsStream("/WEB-INF/config/prop.properties"));
			String uid=request.getParameter("uid");
			String view=request.getParameter("view");
			if(uid==null||uid.isEmpty())
			{
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>Invalid Data. Please pass the right parameter value as mentioned in API doc</p></body></html>");
				response.setStatus(403);
				return;
			}

			StorageType t = FactoryProvider.getFactory().create(prop.getProperty("storageType"));
			FileData data=t.getDetails(uid);

			if(data==null)
			{
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data with uid passed in parameter. Please check if id is correct.</p></body></html>");
				response.setStatus(403);
				return;
			}

			if(view!=null&&Boolean.parseBoolean(view))
			{
				//Show document
				log.info("Since view parameter is set, Trying to display the file in response");
				System.out.println(data);
				System.out.println(data.name);
				t.showFile(data.name,response);
				response.setHeader("Content-Disposition", "filename=\""+data.name+"\"");
			}
			else
			{
				response.setContentType("application/json");
				response.setCharacterEncoding("UTF-8");
				response.getWriter().print(data.toString());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data, Please try again</p></body></html>");
			response.setStatus(403);
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("rawtypes")
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//load the config from property file
		Properties prop = new Properties();
		prop.load(getServletContext().getResourceAsStream("/WEB-INF/config/prop.properties"));
		//set properties
		boolean filecreated=false;
		String fileName="";
		
		if(!CommonUtils.checkSetup(FactoryProvider.getFactory().create(prop.getProperty("storageType"))))
		{
			response.getWriter().println("<html><body><p>App configuration issue.The system is not able to configure itself in expected manner, Please contact your admin</p></body></html>"); 
			
			return;
		}
		try 
		{ 
			log.info(" Recieved a new request to upload file");
			long maxFileSize = Integer.parseInt((String) prop.get("maxFileSize"));
			boolean isMultipart = ServletFileUpload.isMultipartContent(request);
			response.setContentType("text/html");
			java.io.PrintWriter out = response.getWriter();
			if( !isMultipart ) 
			{
				out.println("<html><body><p>No file uploaded</p></body></html>"); 
				return;
			}
			DiskFileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			// maximum file size to be uploaded.
			upload.setSizeMax( maxFileSize );
			// Parse the request to get file items.
			List<FileItem> fileItems = upload.parseRequest(request);
			// Process the uploaded file items
			Iterator i = fileItems.iterator();
			boolean overwrite=false;
			while ( i.hasNext () )
			{
				FileItem fi = (FileItem)i.next();
				if ( fi.isFormField ()&&fi.getFieldName().equals("overwrite") && Boolean.parseBoolean(fi.getString())) 
				{
					overwrite=true;
				}
			}
			log.info(" Number of files to be uploaded:"+fileItems.size());
			for(int j=0;j<fileItems.size();j++)
			{
				FileItem fi = fileItems.get(j);
				if ( !fi.isFormField () ) {
					// Get the uploaded file parameters
					log.info("Size of file:"+FileUtils.byteCountToDisplaySize(fi.getSize()));
					 fileName = fi.getName();
					 if(fileName==null)
					 {
						 response.getWriter().write("<html><body><h1>Invalid File!!</h1><p> Please upload a Valid file.</p></body></html>");
							response.setStatus(403);
							return;
					 }
					log.info("Uploaded file name:"+fileName);
					try {
					if(!isValidFile(fileName,prop,true))
					{
						
						response.getWriter().write("<html><body><h1>Invalid File!!</h1><p>The Uploaded file is not yet supported by the server, Please upload a supported file.</p></body></html>");
						response.setStatus(403);
						return;
					}
					
						failIfDirectoryTraversal(fi.getName());
						
				}
				catch(NullPointerException e)
				{
					response.getWriter().write("<html><body><h1>Invalid file!!</h1><p>Please upload a vaild file.</p></html>");
					response.setStatus(400);
					return;
				}
						catch(Exception e)
						{
							response.getWriter().write("<html><body><h1>Operation Blocked!!</h1><p>This is a protection measure.</p></body></html>");
							response.setStatus(403);
							return;
						}
					log.info("Its a valid file, Proceed with the upload");
					StorageType t = FactoryProvider.getFactory().create(prop.getProperty("storageType"));
					if(t==null)
					{
						out.print("<html><body><h1>OOPS!!</h1><p>Invalid Configuration, Please contact the application Admin.</p></body></html>");
						log.info("Invalid Configuration, Please contact the app admin");
						response.setStatus(403);
						return;
					}
					FileData data = t.store(fi, overwrite);
					
					filecreated=true;
					if(!data.isValid)
					{
						if(data.alreadyUploaded)
						{
							
							out.print("<html><body><h1>OOPS!!</h1><p>File is uploaded already, Please check the documentation for overwriting.</html>");
							log.info("Overwrite disabled");
							response.setStatus(400);
							return;
						}
						else {
							out.print("<html><body><h1>OOPS!!</h1><p>Overwrite is set but file was not found, Please check the documentation for overwriting.</html>");
							log.info("Overwrite disabled");
							response.setStatus(403);
							return;
						}
					}
					
					log.info("Uploaded Filename: " + fileName + "");
					data.notes="File Stored Succesfully at:"+data.location+",Uploaded at:"+data.createdAt+" Size:"+FileUtils.byteCountToDisplaySize(data.size)+"  Name:"+data.name+" FileID:"+data.uid;
					response.setContentType("application/json");
					response.setCharacterEncoding("UTF-8");
					response.getWriter().print(data.toString());
					//response.getWriter().print("File Stored Succesfully at:"+data.location+"\n"+"Uploaded on:"+data.createdAt+"\n Size:"+FileUtils.byteCountToDisplaySize(data.size)+" \n Name:"+data.name+"\n FileID:"+data.uid);
				}
				else
				{
					//ignore non file field
					System.out.println(fi.getName());
				}
			}

		}catch(SizeLimitExceededException e)
		{
			response.getWriter().print("<html><body><h1>Hold On!!</h1><p>The uploaded file size is more than what application is configured to handle(Max:"+FileUtils.byteCountToDisplaySize(Integer.parseInt(prop.getProperty("maxFileSize")))+"), Please contact the application Admin.</p></body></html>");
			//e.printStackTrace();
			response.setStatus(403);
		}
		catch(Exception ex) {
			
			response.getWriter().print("<html><body><h1>OOPS!!</h1><p>There is some error in processing this request, Please contact the application Admin.</p></body></html>");
			log.info("Got exception:",ex);
			
			if(filecreated)
			{
				//corner case;in scenarios where file was uploaded but not stored in DB
				StorageType t = FactoryProvider.getFactory().create(prop.getProperty("storageType"));
				
				t.delete(fileName,true);
			}
			ex.printStackTrace();
			response.setStatus(403);
		}
	}


	

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	@SuppressWarnings("rawtypes")
	protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

		try {
			log.info("Request to update the file");
			Properties prop = new Properties();
			prop.load(getServletContext().getResourceAsStream("/WEB-INF/config/prop.properties"));
			long maxFileSize = Integer.parseInt((String) prop.get("maxFileSize"));
			DiskFileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);
			// maximum file size to be uploaded.
			upload.setSizeMax( maxFileSize );
			// Parse the request to get file items.
			List<FileItem> fileItems = upload.parseRequest(request);
			// Process the uploaded file items
			Iterator i = fileItems.iterator();
			String uid =null;
			log.info("param:"+fileItems.size());
			
			while ( i.hasNext () )
			{
				FileItem fi = (FileItem)i.next();
				System.out.println(fi.getFieldName());
				if ( fi.isFormField ()&&fi.getFieldName().equals("uid")) 
				{
					 uid = fi.getString();
				}
			}
		//	ServletFileUpload upload1 = new ServletFileUpload(factory);
			// List<FileItem> fileItems1 = upload1.parseRequest(request);
			log.info("param:"+fileItems.size());
			StorageType t = FactoryProvider.getFactory().create(prop.getProperty("storageType"));
			for(int j=0;j<fileItems.size();j++)
			{
				FileItem fi = fileItems.get(j);
				if ( !fi.isFormField () ) {
					
					//to update file
					String mimeType = getServletContext().getMimeType( fi.getName());
					System.out.println(mimeType);
					log.info("Size of file:"+FileUtils.byteCountToDisplaySize(fi.getSize()));
					String fileName = fi.getName();
				//	long sizeInBytes = fi.getSize();
					if(fileName==null)
					 {
						 response.getWriter().write("<html><body><h1>Invalid File!!</h1><p> Please upload a Valid file.</p></body></html>");
							response.setStatus(403);
							return;
					 }
					try {
					if(!isValidFile(fileName,prop,true))
					{
						
						response.getWriter().write("<html><body><h1>Invalid File!!</h1><p>The Uploaded file is not yet supported by the server, Please upload a supported file.</p></html>");
						response.setStatus(403);
						return;
					}
					
					
					failIfDirectoryTraversal(fileName);
					}
					catch(NullPointerException e)
					{
						response.getWriter().write("<html><body><h1>Invalid file!!</h1><p>Please upload a vaild file.</p></html>");
						response.setStatus(400);
						return;
					}
					catch(Exception e)
					{
						response.getWriter().write("<html><body><h1>Operation Blocked!!</h1><p>This is a protection measure.</p></html>");
						response.setStatus(403);
						return;
					}
					log.info("Uploaded file name:"+fileName);
					if(t==null)
					{
						response.getWriter().print("<html><body><h1>OOPS!!</h1><p>Invalid Configuration, Please contact the application Admin.</html>");
						log.info("Invalid Configuration, Please contact the app admin");
						response.setStatus(403);
						return;
					}
					FileData data = t.store(uid,fi, true);
					
					log.info("Uploaded Filename: " + fileName + "");
					data.notes="File Stored Succesfully at:"+data.location+"\n"+"Upload Date:"+data.createdAt+" Size:"+FileUtils.byteCountToDisplaySize(data.size)+" Name:"+data.name+" FileID:"+data.uid;
					response.setContentType("application/json");
					response.setCharacterEncoding("UTF-8");
					response.getWriter().print(data.toString());
					//response.getWriter().print("File Stored Succesfully at:"+data.location+"\n"+"Uploaded on:"+data.createdAt+"\n Size:"+FileUtils.byteCountToDisplaySize(data.size)+" \n Name:"+data.name+"\n FileID:"+data.uid);
				}
				else
				{
					log.info("updating details1");
					
					//update file attributes
					if(fi.getFieldName().equalsIgnoreCase("rename"))
					{
						if(!isValidFile(fi.getString(),prop,false))
						{
							
							response.getWriter().write("<html><body><h1>Invalid File Name!!</h1><p>Please specify the name with right extension.</p></html>");
							response.setStatus(403);
							return;
						}
						try {
							failIfDirectoryTraversal(fi.getString());
							}
							catch(Exception e)
							{
								response.getWriter().write("<html><body><h1>Operation Blocked!!</h1><p>This is a protection measure.</p></html>");
								response.setStatus(403);
								return;
							}
						log.info("Operation Rename:"+uid+" to "+fi.getString());
						FileData data = t.update(uid,new FileData(fi.getString(), uid, null, new Date(), -1));
						
						if(!data.isValid||data.alreadyUploaded)
						{
							response.setContentType("application/text");
							response.setCharacterEncoding("UTF-8");
							response.getWriter().print("<html><body><h1>OOPS!!</h1><p>File with same name already exists.Please rename with some other value</p></body></html>");
						return;
						}
						
						data=t.getDetails(uid);
						
						
						response.setContentType("application/json");
						response.setCharacterEncoding("UTF-8");
						response.getWriter().print(data.toString());
					}
					else if(fi.getFieldName().equalsIgnoreCase("uid"))
					{
						
					}
					else {
						response.getWriter().print("No Field specified to update!");
					}
				}
			}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data, Please try again</p>");
				response.setStatus(403);
			}
		}
	
	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		
		try {
			System.out.println(1234);
			Properties prop = new Properties();
			prop.load(getServletContext().getResourceAsStream("/WEB-INF/config/prop.properties"));
			System.setProperty("propfile", getServletContext().getContextPath()+"/WEB-INF/config/prop.properties");
			String uid=request.getParameter("uid");
			if(uid==null||uid.isEmpty())
			{
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>Invalid Data. Please pass the right parameter value as mentioned in API doc</p></body></html>");
				response.setStatus(403);
				return;
			}

			StorageType t = FactoryProvider.getFactory().create(prop.getProperty("storageType"));
			FileData data=t.delete(uid);
			if(data==null)
			{
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data with uid passed in parameter. Please check if id is correct.</p>");
				response.setStatus(400);
				return;
			}
			
				//Show document
				log.info(" Trying to display the file in response");
				data.notes="File Deleted succesfully. file path:"+data.location;
				System.out.println(data);
					System.out.println(data.name);
				//t.showFile(data.name,response);
					response.setContentType("application/json");
					response.setCharacterEncoding("UTF-8");
				response.getWriter().write(data.toString());
				
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data, Please try again</p>");
			response.setStatus(403);
		}
	}

	
	boolean isValidFile(String name, Properties prop,boolean checkMime)
	{
		String whitelist=prop.getProperty("allowedExtensions");
		String whitelistMime=prop.getProperty("allowedMime");
		boolean isValidExt=false;
		boolean isValidMime=false;
		String[] whitelistArr = whitelist.split(",");
		String[] whitelistArrMime = whitelistMime.split(",");
		for(String ext:whitelistArr)
		{
			if(name.contains(ext))
			{
				isValidExt=true;
				log.info("Valid Extension:"+ext);
			}
		}
		if(checkMime)
		{
		String mimeType = getServletContext().getMimeType(name);
		System.out.println(mimeType);
		for(String ext:whitelistArrMime)
		{
			if(mimeType.equals(ext))
			{
				isValidMime=true;
				log.info("Valid Mime:"+ext);
			}
		}
		
		}
		else
			isValidMime=true;
		
		return isValidExt&&isValidMime;
	}
	
	public void failIfDirectoryTraversal(String relativePath)
	{
		System.out.println(relativePath);
	    File file = new File(relativePath);
	    System.out.println(file.isAbsolute());
	    if (file.isAbsolute())
	    {
	    //	log.error("Directory traversal attempt - absolute path not allowed");
	        throw new RuntimeException("Directory traversal attempt - absolute path not allowed");
	    }

	    String pathUsingCanonical = null;
	    String pathUsingAbsolute = null;
	    try
	    {
	    	 System.out.println(file.getCanonicalPath());
	        pathUsingCanonical = file.getCanonicalPath();
	        System.out.println(file.getAbsolutePath());
	        pathUsingAbsolute = file.getAbsolutePath();
	    }
	    catch (IOException e)
	    {
	    	//log.error("Directory traversal attempt?",e);
	    	e.printStackTrace();
	      //  throw new RuntimeException("Directory traversal attempt?", e);
	    }


	    // Require the absolute path and canonicalized path match.
	    // This is done to avoid directory traversal 
	    // attacks, e.g. "1/../2/" 
	    if (! pathUsingCanonical.equals(pathUsingAbsolute))
	    {
	    	//log.error("Directory traversal attempt?");
	        
	    	//throw new RuntimeException("Directory traversal attempt?");
	    }
	}
	}
