package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.Production;
import edu.ifrs.cooperativeeditor.model.RubricProductionConfiguration;
import edu.ifrs.cooperativeeditor.model.UserRubricStatus;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class ProductionRepository implements PanacheRepository<Production> {

    RubricProductionConfigurationRepository rubrep = new RubricProductionConfigurationRepository();

    UserRubricStatusRepository ursrep = new UserRubricStatusRepository();

    /**
	 * Return a production from data base
	 *
	 * @param long: The production id
	 * @return Production: The production object
	 */
	public Production getProduction(long id) {
		return findById(id);
	}

    /**
	 * Return Production list by User from data base
	 *
	 * @param long: The user id into Production
	 * @return List: Production object List
	 */
	public List<Production> getProductionByUserId(long userId) {
		return find("SELECT DISTINCT p.* FROM production p "
        + "LEFT JOIN user_production_configuration upc ON p.id = upc.production_id "
        + "WHERE p.user_id = ?1 OR upc.user_id = ?2 ORDER BY p.id DESC ", userId, userId)
        .firstResult();
	}

    /**
	 * Return a production from data base
	 *
	 * @param String: The production hash
	 * @return Production: The production object
	 */
	public Production getProductionByUrl(String url) {
        Production production = null;
        production = find("url", url).firstResult();

		List<RubricProductionConfiguration> rPC = rubrep.getRubricProductionConfigurationByProductionId(production.getId());

		for(RubricProductionConfiguration uPC : rPC) {
			List<UserRubricStatus> uRSs = ursrep.getUserRubricStatusByRubricIdAndProductionId(uPC.getRubric().getId(),production.getId());
			uPC.getRubric().setUserRubricStatus(uRSs);
			production.setUserRubricStatus(uRSs);
		}

		return production;
	}

    public Production mergeProduction(final Production production) {
        return Panache.getEntityManager().merge(production);
    }

    public void persistProduction(final Production production) {
        Panache.getEntityManager().persist(production);
    }
}
