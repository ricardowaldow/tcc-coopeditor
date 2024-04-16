package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.InputMessage;
import edu.ifrs.cooperativeeditor.model.Production;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class InputMessageRepository implements PanacheRepository<InputMessage> {

    ProductionRepository prorep = new ProductionRepository();

    /**
	 * Retrieve stored messages from the data base
	 *
	 * @return A JSON representing a collection of stored messages
	 */
	public String getMessages(String hashProduction) {

		Production production = prorep.getProductionByUrl(hashProduction);

		StringBuilder json = new StringBuilder();
		json.append("[");

		List<InputMessage> result = find("inner join text_message t type = 'SEND_MESSAGE' AND t.production = ?1", production.getId()).list();

        boolean flag = false;
		for (InputMessage im : result) {
			if (im.getMessage() != null) {
				json.append(im.toString());
				json.append(",");
				flag = true;
			}
		}

		StringBuilder str = new StringBuilder();
		if (flag) {
			str.append(json.substring(0, json.length() - 1));
			str.append("]");
		}
		return str.toString();
	}

    public InputMessage mergeInputMessage(InputMessage input) {
        return Panache.getEntityManager().merge(input);
    }

    public void persistInputMessage(InputMessage input) {
        Panache.getEntityManager().persist(input);
    }
}
