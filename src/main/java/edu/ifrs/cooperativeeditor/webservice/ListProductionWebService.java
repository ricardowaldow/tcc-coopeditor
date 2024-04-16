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
package edu.ifrs.cooperativeeditor.webservice;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import edu.ifrs.cooperativeeditor.model.Production;
import edu.ifrs.cooperativeeditor.repository.ProductionRepository;

@Path("/list")
public class ListProductionWebService {

	private static final Logger log = Logger.getLogger(ListProductionWebService.class.getName());

	private ProductionRepository prorep = new ProductionRepository();

	@Context
	private HttpServletRequest request;

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes({ MediaType.TEXT_XML, MediaType.WILDCARD, MediaType.TEXT_PLAIN })
	@Path("/productionList")
	public String getProduction() {

		StringBuilder json = new StringBuilder();

		List<Production> productions = new ArrayList<Production>();
		try {
			productions = prorep.getProductionByUserId((long) request.getSession().getAttribute("userId"));
		} catch (Exception e) {
			//TODO Exception
		}

		StringBuilder strReturn = new StringBuilder();
		strReturn.append("[ ");

		for (Production production : productions) {
			strReturn.append("{");
			strReturn.append("\"id\":");
			strReturn.append("\"" + production.getId() + "\",");
			strReturn.append("\"objective\":");
			strReturn.append("\"" + production.getObjective() + "\",");
			strReturn.append("\"url\":");
			strReturn.append("\"" + production.getUrl() + "\"");
			strReturn.append("}");
			strReturn.append(",");

		}

		json.append(strReturn.substring(0, strReturn.length() - 1));
		json.append("]");

		log.log(Level.INFO,"Web service return of /productionList: " + json.toString());
		return json.toString();
	}
}
