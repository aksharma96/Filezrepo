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

import com.google.gson.GsonBuilder;

import utils.FactoryProvider;
import utils.FileData;
import utils.StorageType;

/**
 * Servlet implementation class upload
 */
@WebServlet({ "/api/v1/getstore" })
public class ReadStore extends HttpServlet {
	private static final long serialVersionUID = 1L;
	 private static final Logger log = LogManager.getLogger(ReadStore.class);
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ReadStore() {
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
			
			StorageType t = FactoryProvider.getFactory().create(prop.getProperty("storageType"));
			FileData[] data=t.getAllDetails();
			if(data==null)
			{
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>I'm Unable to pull the requested data, Please try again</p></body></html>");
				response.setStatus(403);
				log.info("Some issue, Please check logs");
				return;
			}
			if(data.length==0)
			{
				response.getWriter().write("<html><body><h1>OOPS!!!</h1><p>No Data in the Store.Please Upload something to proceed</p></body></html>");
				response.setStatus(403);
				log.info("Some issue, Please check logs");
				return;
			}
			String json = new GsonBuilder()
					.excludeFieldsWithoutExposeAnnotation()
					.create().toJson(data);
			response.getWriter().print(json);
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

	/**
	 * 
	 * @param part
	 * @return
	 * @author akshits
	 */
}
