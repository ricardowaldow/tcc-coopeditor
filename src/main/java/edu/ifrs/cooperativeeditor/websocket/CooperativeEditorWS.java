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
package edu.ifrs.cooperativeeditor.websocket;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;


import jakarta.servlet.http.HttpSession;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import edu.ifrs.cooperativeeditor.model.Content;
import edu.ifrs.cooperativeeditor.model.Contribution;
import edu.ifrs.cooperativeeditor.model.InputMessage;
import edu.ifrs.cooperativeeditor.model.OutputMessage;
import edu.ifrs.cooperativeeditor.model.Production;
import edu.ifrs.cooperativeeditor.model.Rubric;
import edu.ifrs.cooperativeeditor.model.RubricProductionConfiguration;
import edu.ifrs.cooperativeeditor.model.Situation;
import edu.ifrs.cooperativeeditor.model.SoundEffect;
import edu.ifrs.cooperativeeditor.model.TextMessage;
import edu.ifrs.cooperativeeditor.model.User;
import edu.ifrs.cooperativeeditor.model.UserProductionConfiguration;
import edu.ifrs.cooperativeeditor.model.UserRubricStatus;
import edu.ifrs.cooperativeeditor.repository.ContributionRepository;
import edu.ifrs.cooperativeeditor.repository.InputMessageRepository;
import edu.ifrs.cooperativeeditor.repository.ProductionRepository;
import edu.ifrs.cooperativeeditor.repository.TextMessageRepository;
import edu.ifrs.cooperativeeditor.repository.UserProductionConfigurationRepository;
import edu.ifrs.cooperativeeditor.repository.UserRepository;
import edu.ifrs.cooperativeeditor.repository.UserRubricStatusRepository;
import fraser.neil.plaintext.diff_match_patch;

/**
 * Web Socket server that controls the editor
 *
 * @author Rodrigo Prestes Machado
 */
@ServerEndpoint(value = "/editorws/{hashProduction}", configurator = HttpSessionConfigurator.class)
public class CooperativeEditorWS {

	private static Map<String, List<User>> activeUsers = Collections.synchronizedMap(new HashMap<String, List<User>>());

	private static final Logger log = Logger.getLogger(CooperativeEditorWS.class.getName());

	private static Gson gson = new Gson();

	private InputMessageRepository imsrep = new InputMessageRepository();

	private TextMessageRepository tmsrep = new TextMessageRepository();

	private UserProductionConfigurationRepository upcrep = new UserProductionConfigurationRepository();

	private UserRepository usrrep = new UserRepository();

	private ProductionRepository prorep = new ProductionRepository();

	private UserRubricStatusRepository ursrep = new UserRubricStatusRepository();

	private ContributionRepository conrep = new ContributionRepository();

	@OnMessage
	public void onMessage(String jsonMessage, @PathParam("hashProduction") String hashProduction, Session session) {

		log.log(Level.INFO, "inputMessage: " + jsonMessage + " hashProduction " + hashProduction + " session "
				+ session.getUserProperties().get(hashProduction));

		InputMessage input = parseInputMessage(jsonMessage, session, hashProduction);
		boolean returnMessage = false;
		OutputMessage out = null;

		if (input != null) {
			switch (Type.valueOf(input.getType())) {
			case SEND_MESSAGE:
				out = this.sendMessageHandler(input);
				returnMessage = true;
				break;
			case TYPING:
				out = this.typingHandler(input);
				returnMessage = true;
				break;
			case FINISH_RUBRIC:
				out = this.finishRubricHandler(input, session, hashProduction);
				returnMessage = (out != null);
				break;
			case REQUEST_PARTICIPATION:
				out = this.requestParticipationHandler(input, session, hashProduction);
				returnMessage = (out != null);
				break;
			case FINISH_PARTICIPATION:
					out = this.finishParticipationHandler(input, session, hashProduction);
					returnMessage = true;
				break;
			default:
				break;
			}
			imsrep.persistInputMessage(input);
		}

		if (returnMessage) {
			log.log(Level.INFO, "outputMessage: " + out.toString());
			sendToAll(out.toString(), session, hashProduction);
		}
	}

	/**
	 * Open web socket method
	 *
	 * @param session
	 */
	@OnOpen
	public void onOpen(Session session, @PathParam("hashProduction") String hashProduction, EndpointConfig config)
			throws IOException {

		log.log(Level.INFO, "onOpen");

		// saves the production hash in session, to filter the exchange of msg between
		// users
		session.getUserProperties().put(hashProduction, true);

		Production production = findProductionFromDataBase(hashProduction);

		// initializes the map where UserProductionConfiguration are stored
		if (!activeUsers.containsKey(hashProduction)) {
			activeUsers.put(hashProduction, new ArrayList<User>());
		}

		// retrieve http session
		HttpSession httpSession = (HttpSession) config.getUserProperties().get("sessionHttp");

		// retrieves the id of the logged-in user in the http session
		String userId = httpSession.getAttribute("userId").toString();

		User user = findUserOnList(Long.parseLong(userId), hashProduction);

		if(user == null) {

			List<String> strUPC = new ArrayList<String>();
			for (User u : activeUsers.get(hashProduction)) {
				if (u.getUserProductionConfiguration() != null)
						strUPC.add(u.getUserProductionConfiguration().toString());
				}

			OutputMessage outLast = registerUser(userId, session, hashProduction);

			OutputMessage out = new OutputMessage();
			out.setType(Type.LOAD_INFORMATION.name());



			User discoveredUser = findUserOnList(session, hashProduction);
			String jsonUser = "{\"id\":\"" + discoveredUser.getId().toString() + "\",\"name\":\""+ discoveredUser.getName()+"\"}";
			out.addData("user", jsonUser);
			out.addData("production", production.toString().replaceAll("\n", "\\\\n"));
			out.addData("uPCsConnected", strUPC.toString());
			out.addData("messages", imsrep.getMessages(hashProduction));

			session.getBasicRemote().sendText(out.toString());
			log.log(Level.INFO, "outputMessage: " + out.toString());

			// sends a text to the client
			sendToAll(outLast.toString(), session, hashProduction);
			log.log(Level.INFO, "outputMessage: " + outLast.toString());

		} else {
			session.getBasicRemote().sendText("{\"isLoggedIn\":true}");
		}
	}

	/**
	 * Close web socket method
	 *
	 * @param session
	 */
	@OnClose
	public void onClose(Session session, @PathParam("hashProduction") String hashProduction) {

		log.log(Level.INFO, "onClose");

		User user = findUserOnList(session, hashProduction);

		activeUsers.get(hashProduction).remove(user);

		if (user != null && user.getUserProductionConfiguration() != null) {
			Boolean releaseProduction = user.getUserProductionConfiguration().getSituation().equals(Situation.CONTRIBUTING);

			List<String> strUPC = new ArrayList<String>();
			for (User u : activeUsers.get(hashProduction)) {
				if (u.getUserProductionConfiguration() != null) {
					if(releaseProduction)
						u.getUserProductionConfiguration().setSituation(Situation.FREE);
					strUPC.add(u.getUserProductionConfiguration().toString());
				}
			}

			OutputMessage out = new OutputMessage();
			out.setType(Type.DISCONNECTION.name());
			out.addData("size", String.valueOf(activeUsers.get(hashProduction).size()));
			out.addData("userProductionConfigurations", strUPC.toString());
			if(user.getUserProductionConfiguration() != null)
				out.addData("disconnected", user.getId().toString());

			log.log(Level.INFO, "outputMessage: " + out.toString());
			sendToAll(out.toString(), session, hashProduction);
		}
	}

	/**
	 * Handles the SEND_MESSAGE message
	 *
	 * @param InputMessage input : Object input message
	 * @return OutputMessage
	 */
	private OutputMessage sendMessageHandler(InputMessage input) {
		OutputMessage out = new OutputMessage();
		out.setType(Type.SEND_MESSAGE.name());
		out.addData("message", input.getMessage().getTextMessage());
		out.addData("user", input.getUser().getName());
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
		out.addData("time", sdf.format(input.getDate()));
		UserProductionConfiguration upc = input.getUser().getUserProductionConfiguration();
		SoundEffect se = upc.getSoundEffect();
		out.addData("effect", se.getEffect());
		out.addData("position", se.getPosition());

		return out;
	}

	/**
	 * Handles the TYPING message
	 *
	 * @param InputMessage input : Object input message
	 * @return OutputMessage
	 */
	private OutputMessage typingHandler(InputMessage input) {
		OutputMessage out = new OutputMessage();

		out.setType(Type.TYPING.name());
		out.addData("user", input.getUser().getName());
		UserProductionConfiguration upc = input.getUser().getUserProductionConfiguration();
		SoundEffect se = upc.getSoundEffect();
		out.addData("effect", se.getEffect());
		out.addData("position", se.getPosition());
		return out;
	}

	/**
	 * Handles the FINISH_RUBRIC message
	 *
	 * @param InputMessage input : Object input message
	 * @return OutputMessage
	 */
	private OutputMessage finishRubricHandler(InputMessage input, Session session,  String hashProduction) {
		OutputMessage out = null;
		UserRubricStatus userRubricStatus = input.getUserRubricStatus();
		if(userRubricStatus != null) {
			out = new OutputMessage();
			out.setType(Type.FINISH_RUBRIC.name());
			Production production = findProductionFromDataBase(hashProduction);
			List<RubricProductionConfiguration> rpc = production.getRubricProductionConfigurations();
			out.addData("rubricProductionConfiguration",rpc.toString());

			User user = findUserOnList(session, hashProduction);
			if(user.getUserProductionConfiguration() != null)
				out.addData("upcUser", user.getUserProductionConfiguration().toString());

		}
		return out;
	}

	/**
	 * Difference between two contributions
	 *
	 * @param Contribution contribution Object
	 * @param Contribution oldContribution Object
	 * @return String in HTML
	 */
	private String diffFormatHtml(Contribution contribution, Contribution oldContribution) {
		diff_match_patch dmp = new diff_match_patch();
	    LinkedList<diff_match_patch.Diff> diff = dmp.diff_main(oldContribution.getContent().toString(), contribution.getContent().toString());
	    dmp.diff_cleanupSemantic(diff);
		return dmp.diff_prettyHtml(diff);
	}

	/**
	 * Handles the REQUEST_PARTICIPATION message
	 *
	 * @param InputMessage input : Object input message
	 * @param Session session : Web Socket session
	 * @param String hashProduction : The hash that identify the production
	 * @return OutputMessage
	 */
	private OutputMessage requestParticipationHandler(InputMessage input,Session session,String hashProduction) {
		OutputMessage out = null;
		if (!this.hasAnyoneContributed(hashProduction)) {
			out = new OutputMessage();
			out.setType(Type.REQUEST_PARTICIPATION.name());
			User user = findUserOnList(session, hashProduction);
			List<String> strUPC = new ArrayList<String>();
			for (User u : activeUsers.get(hashProduction)) {
				if (u.getUserProductionConfiguration() != null) {
					if (u.getId() == user.getId())
						u.getUserProductionConfiguration().setSituation(Situation.CONTRIBUTING);
					else
						u.getUserProductionConfiguration().setSituation(Situation.BLOCKED);
					strUPC.add(u.getUserProductionConfiguration().toString());
				}
			}
			out.addData("userProductionConfigurations", strUPC.toString());
			out.addData("author", user.toString());
		}
		return out;
	}

	/**
	 * Handles the FINISH_PARTICIPATION message
	 *
	 * @param InputMessage input : Object input message
	 * @param String hashProduction : The hash that identify the production
	 * @return OutputMessage
	 */
	private OutputMessage finishParticipationHandler(InputMessage input,Session session, String hashProduction) {
		// Return user with less tickets used
		User use = Collections.max(activeUsers.get(hashProduction), new Comparator<User>(){
			@Override
			public int compare(User o1, User o2) {
				int a = -1;
				if(o1.getUserProductionConfiguration() != null && o2.getUserProductionConfiguration() != null)
					if(o1.getUserProductionConfiguration().getTicketsUsed() < o2.getUserProductionConfiguration().getTicketsUsed())
						a = 1;
				return a;
			}});

		int lessTicketUsed = use.getUserProductionConfiguration().getTicketsUsed();

		int limint = use.getUserProductionConfiguration().getProduction().getMinimumTickets();

		OutputMessage out = new OutputMessage();
		out.setType(Type.FINISH_PARTICIPATION.name());
		List<String> strUPC = new ArrayList<String>();

		for (User u : activeUsers.get(hashProduction)) {
			if (u.getUserProductionConfiguration() != null) {
				if(lessTicketUsed < u.getUserProductionConfiguration().getTicketsUsed() || limint == u.getUserProductionConfiguration().getTicketsUsed().intValue())
					u.getUserProductionConfiguration().setSituation(Situation.BLOCKED);
				else
					u.getUserProductionConfiguration().setSituation(Situation.FREE);
				upcrep.mergeUserProductionConfiguration(u.getUserProductionConfiguration());
				strUPC.add(u.getUserProductionConfiguration().toString());
			}
		}
		out.addData("userProductionConfigurations", strUPC.toString());
		out.addData("contribution", input.getContribution().toString());

		User user = findUserOnList(session, hashProduction);
		out.addData("author", user.toString());

		return out;
	}

	/**
	 * Send to all connected users in web socket server
	 *
	 * @param String message : A message in JSON format
	 */
	private void sendToAll(String message, Session session, String hashProduction) {
		try {
			synchronized (activeUsers.get(hashProduction)) {
				for (User u : activeUsers.get(hashProduction))
					if (Boolean.TRUE.equals(u.getSession().getUserProperties().get(hashProduction)))
						u.getSession().getBasicRemote().sendText(message.replaceAll("\n", "\\\\n"));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Find the user from data base
	 *
	 * @param long: userId : User id
	 * @return User: One user object
	 */
	private User findUserFromDataBase(long userId, String hashProduction) {
		User user = usrrep.getUser(userId, hashProduction);
		return user;
	}

	/**
	 * Find the user from Web Socket session Or User Id
	 *
	 * @param Object indexer : Web socket session Or User Id
	 * @return User object
	 */
	private User findUserOnList(Object indexer, String hashProduction) {
		Boolean findBySession = (indexer instanceof Session);
		User user = null;
		for (User u : activeUsers.get(hashProduction)) {
			if(findBySession) {
				String idSesssion = u.getSession().getId();
				if (idSesssion.equals(((Session) indexer).getId()))
					user = u;
			} else {
				if (u.getId() == indexer)
					user = u;
			}
		}
		return user;
	}

	/**
	 * Find the Production from data base
	 *
	 * @param String hashProduction : Production URL
	 * @return Production: One Production object
	 */

	private Production findProductionFromDataBase(String hashProduction) {
		return prorep.getProductionByUrl(hashProduction);
	}

	/**
	 *
	 * @param String hashProduction
	 * @return boolean
	 */
	private Boolean hasAnyoneContributed(String hashProduction) {
		Boolean a = false;
			for (User u : activeUsers.get(hashProduction))
				if(u.getUserProductionConfiguration() != null)
					if (u.getUserProductionConfiguration().getSituation().equals(Situation.CONTRIBUTING))
						a = true;
		return a;
	}

	/**
	 * Creates the input message object
	 *
	 * @param String jsonMessage : JSON message from the client
	 * @param Sesstion session : Web Socket session
	 * @return InputMessage object
	 */
	private InputMessage parseInputMessage(String jsonMessage, Session session, String hashProduction) {

		// Retrieve the user from WS list of sessions
		User user = findUserOnList(session, hashProduction);
		InputMessage input = null;

		// if it is null, it means that the user is not participating in the production,
		// just a visitor or owner.
		if (user.getUserProductionConfiguration() != null) {

			Production production = user.getUserProductionConfiguration().getProduction();

			input = gson.fromJson(jsonMessage, InputMessage.class);
			Calendar cal = Calendar.getInstance();
			Timestamp time = new Timestamp(cal.getTimeInMillis());
			input.setDate(time);

			// Assign the user to the input
			input.setUser(user);

			// Check if the json message contains an text message
			if (jsonMessage.toLowerCase().contains("message")) {
				TextMessage message = new TextMessage();
				message = gson.fromJson(jsonMessage, TextMessage.class);
				message.escapeCharacters();
				// To maintain the relationship between the messages exchanged during production
				message.setProduction(production);
				tmsrep.persistMessage(message);
				input.setMessage(message);

			} else if (input.getType().equals(Type.FINISH_RUBRIC.toString())) {

				JsonParser parser = new JsonParser();
				JsonObject objeto = parser.parse(jsonMessage).getAsJsonObject();
				RubricProductionConfiguration rPC = new RubricProductionConfiguration();
				rPC = gson.fromJson(objeto.get("rubricProductionConfiguration").toString(),
						RubricProductionConfiguration.class);

				for (RubricProductionConfiguration ruPC : production.getRubricProductionConfigurations()) {
					if (ruPC.getId() == rPC.getId()) {
						rPC = ruPC;
						break;
					}
				}

				Rubric rubric = rPC.getRubric();
				UserRubricStatus userRubricStatus = new UserRubricStatus();
				userRubricStatus.setConsent(true);
				userRubricStatus.setSituation(Situation.FINALIZED);
				userRubricStatus.setUser(user);
				userRubricStatus.setRubric(rubric);
				userRubricStatus.setProduction(production);
				ursrep.persistUserRubricStatus(userRubricStatus);

				input.setUserRubricStatus(userRubricStatus);

				rPC.getRubric().addUserRubricStatus(userRubricStatus);
				production.addUserRubricStatus(userRubricStatus);

			} else if (input.getType().equals(Type.FINISH_PARTICIPATION.toString())) {

				Content content = new Content();
				JsonParser parser = new JsonParser();
				JsonObject objeto = parser.parse(jsonMessage).getAsJsonObject();
				content = gson.fromJson(objeto.get("content").toString(), Content.class);


				Contribution contribution = new Contribution();
				contribution.setUser(user);
				contribution.setProduction(production);
				contribution.setContent(content);
				contribution.setMoment(new Date());
				contribution.setCard(0);

				try {
					conrep.persistContribution(contribution);
					user.getUserProductionConfiguration().increaseTicketsUsed();
					user.getUserProductionConfiguration().setSituation(Situation.FREE);
					upcrep.mergeUserProductionConfiguration(user.getUserProductionConfiguration());
				} catch (Exception e) {
					// TODO: handle exception
				}

				input.setContribution(contribution);
			}
		}

		return input;
	}

	/**
	 * Register the user by creating a creates the input message object
	 *
	 * @param String userId : user Id
	 * @param Sesstion session : Web Socket session
	 * @return OutputMessage object
	 */
	private OutputMessage registerUser(String userId, Session session, String hashProduction) {
		InputMessage input = new InputMessage();
		input.setType(Type.NEW_CONNECTED.name());
		Calendar cal = Calendar.getInstance();
		Timestamp time = new Timestamp(cal.getTimeInMillis());
		input.setDate(time);

		// Get the user from the data base in the login
		User user = findUserFromDataBase(Long.parseLong(userId), hashProduction);
		user.setSession(session);

		// Assign the user to the input
		input.setUser(user);

		// Add the user in the WS connected users
		activeUsers.get(hashProduction).add(user);

		imsrep.persistInputMessage(input);

		OutputMessage out = new OutputMessage();
		out.setType(Type.NEW_CONNECTED.name());
		if(user.getUserProductionConfiguration() != null)
			out.addData("userProductionConfiguration", user.getUserProductionConfiguration().toString() );


		return out;
	}
}
