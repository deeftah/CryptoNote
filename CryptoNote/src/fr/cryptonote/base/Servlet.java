package fr.cryptonote.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Servlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	public static final Logger log = Logger.getLogger("fr.cryptonote");
	
	@Override public void init(ServletConfig servletConfig) throws ServletException {
		
	}
	
	@Override public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		PrintWriter out = resp.getWriter();
	    out.println("Hello, world!");
	    out.close();
	}

	@Override public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {

	}

}
