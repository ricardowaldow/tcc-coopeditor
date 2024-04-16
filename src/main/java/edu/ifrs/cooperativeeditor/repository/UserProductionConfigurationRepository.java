package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.UserProductionConfiguration;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class UserProductionConfigurationRepository
implements PanacheRepository<UserProductionConfiguration> {

    /**
	 * Return a production from data base
	 *
	 * @param long: The UserProductionConfiguration id
	 * @return Production: The UserProductionConfiguration object
	 */
	public UserProductionConfiguration getUserProductionConfiguration(long id) {
		return findById(id);
	}

    public List<UserProductionConfiguration> getUserProductionConfigurationByProductionId(long productionId) {
		return find("production", productionId).list();
	}

    public UserProductionConfiguration getUserProductionConfigurationByidUserAndHashProduction(
        Long idUser,
        String hashProduction) {
            return find("inner join production p where user = ?1 AND p.url = ?2", idUser,hashProduction)
            .firstResult();
    }

    public UserProductionConfiguration mergeUserProductionConfiguration(
        final UserProductionConfiguration configuration
    ) {
        return Panache.getEntityManager().merge(configuration);
    }

    public void persistUserProductionConfiguration(
        final UserProductionConfiguration configuration
    ) {
        Panache.getEntityManager().persist(configuration);
    }

    public void removeUserProductionConfiguration(
        final UserProductionConfiguration configuration
    ) {
        Panache.getEntityManager().remove(configuration);
    }
}
