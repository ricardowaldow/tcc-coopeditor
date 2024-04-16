/**
 * @license
 * Copyright 2018, Instituto Federal do Rio Grande do Sul (IFRS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.ifrs.cooperativeeditor.controller;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import edu.ifrs.cooperativeeditor.webservice.FormWebService;

/**
 * Servlet Filter implementation class ControllerFilter
 *
 * @author Lauro Correa Junior
 * @author Alexandre Almeida
 */
@WebFilter(urlPatterns = { "/login", "/new-user", "/activities", "/activities/*", "/webservice/*", "/editorws/*" })
public class ControllerFilter implements Filter {

	private static final Logger log = Logger.getLogger(FormWebService.class.getName());

	public void destroy() {
		log.log(Level.INFO, "WebFilter destroy ");
	}

	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
			throws ServletException, IOException {

		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) res;
		HttpSession session = request.getSession();

		boolean loggedIn = session.getAttribute("userId") != null;
		boolean loginRequired = false;
		boolean forwardToIndex = false;

		//Remove slashes (/) from the end of the path
		String path = request.getRequestURI().substring(request.getContextPath().length()).replaceAll("[/]+$", "");

		if (path.equals("/login") || path.equals("/new-user") ||
		    path.equals("/activities") || path.startsWith("/activities/")) {

			forwardToIndex = true;
		} else if (path.startsWith("/webservice/") || path.startsWith("/editorws/")) {
			loginRequired = true;
		}

		if (forwardToIndex) {
			request.getServletContext().getRequestDispatcher("/").forward(request, response);
		} else if (loginRequired && !loggedIn) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		} else {
			chain.doFilter(req, res);
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
		log.log(Level.INFO, "WebFilter init ");
	}
}
