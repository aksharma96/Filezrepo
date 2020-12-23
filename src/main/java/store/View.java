package store;

import java.io.IOException;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utils.FactoryProvider;
import utils.FileData;
import utils.StorageType;

/**
 * Servlet implementation class upload
 */
@WebServlet({ "/api/v1/view", "/api/v1/View" })
public class View extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final Logger log = LogManager.getLogger(View.class);
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public View() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		try {
			Properties prop = new Properties();
			prop.load(getServletContext().getResourceAsStream("/WEB-INF/config/prop.properties"));
			String uid=request.getParameter("uid");
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
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data with uid passed in parameter. Please check if id is correct.</p>");
				response.setStatus(403);
				return;
			}
			
				//Show document
				log.info(" Trying to display the file in response");
				System.out.println(data);
					System.out.println(data.name);
				t.showFile(data.name,response);
				response.setHeader("Content-Disposition", "filename=\""+data.name+"\"");
				
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
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}


	}