package com.remkohde.dev.liberty.servlets;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Servlet implementation class NewsSearchServlet
 */
@WebServlet(name = "com.remkohde.dev.liberty.NewsSearchServlet",
			loadOnStartup=1,
        	urlPatterns = "/news")
public class NewsSearchServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public NewsSearchServlet() {
        super();
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		String hosturl = request.getScheme()+"://"+request.getServerName()+":"+request.getServerPort()+""+request.getContextPath();
		
		String startdate = (String) request.getParameter("startdate");
		String enddate = (String) request.getParameter("enddate");
		String searchterm = (String) request.getParameter("searchterm");
		String count = (String) request.getParameter("count");
	
		// Search AlchemyData News
		//String alchemyResults = "[{\"alchemyResults\":\"Hello AlchemyNews\"}]";
		String alchemyResults = getAlchemyNewsApi(hosturl, startdate, enddate, searchterm, count);
		request.getSession().setAttribute("alchemyResults", alchemyResults);
		
		// Save to CloudantDB
		String cloudantResults = postCloudantDbApi(hosturl, alchemyResults);
		request.getSession().setAttribute("cloudantResults", cloudantResults);
		
		//request.getRequestDispatcher("pages/response.jsp").include(request, response);		
		request.getRequestDispatcher("pages/newsanalysis_result.jsp").forward(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		doGet(request, response);
	}
	
	
	private String getAlchemyNewsApi(String hosturl, String startdate, String enddate, String searchterm, String count){		
		String params = "startdate="+startdate+"&enddate="+enddate+"&searchterm="+searchterm+"&count="+count;
		String urlString = hosturl+"/api/watson/news";
		
		String response = callApi("get", urlString, params);
		return response;
	}
	
	private String postCloudantDbApi(String hosturl, String jsonobj){		
		String params = "jsonobj="+jsonobj;
		String urlString = hosturl+"/api/cloudant/data";
		
		String response = callApi("post", urlString, params);
		return response;
	}
	
	private String callApi(String method, String urlString, String params){
		System.out.println("UrlString: "+urlString);
		
		String response = "";
		HttpURLConnection conn = null;
		try {
			if(method.toLowerCase()=="get"){
				
				URL url = new URL(urlString+"?"+params);
				conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("Accept", "application/json");
				
			}else if(method.toLowerCase()=="post"){
				
				byte[] postData       = params.getBytes(StandardCharsets.UTF_8);
				int    postDataLength = postData.length;
				URL    url            = new URL(urlString);
				conn= (HttpURLConnection) url.openConnection();           
				conn.setDoOutput(true);
				//conn.setInstanceFollowRedirects(false);
				conn.setRequestMethod("POST");
				//conn.setRequestProperty( "Content-Type", "application/x-www-form-urlencoded"); 
				//conn.setRequestProperty( "charset", "utf-8");
				conn.setRequestProperty( "Content-Length", Integer.toString( postDataLength ));
				//conn.setUseCaches( false );
				try( DataOutputStream wr = new DataOutputStream( conn.getOutputStream())) {
				   wr.write( postData );
				}
				
			}else{
				// 
				return "Illegal HTTP Method";
			}
			
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));	
			String output;			
			while ((output = br.readLine()) != null) {
				System.out.println(output);
				response += output;
			}
			conn.disconnect();
			
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return response;
	}


}
