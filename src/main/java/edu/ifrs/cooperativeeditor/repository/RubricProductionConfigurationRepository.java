package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.RubricProductionConfiguration;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class RubricProductionConfigurationRepository
implements PanacheRepository<RubricProductionConfiguration> {

    /**
	 * Return a RubricProductionConfiguration from data base
	 *
	 * @param long: The RubricProductionConfiguration id
	 * @return RubricProductionConfiguration: The RubricProductionConfiguration object
	 */
	public RubricProductionConfiguration getRubricProductionConfiguration(long id) {
		return findById(id);
	}

    /**
	 * Return rubricProductionConfiguration list from data base
	 *
	 * @param long: The Rubric id into rubricProductionConfiguration
	 * @return List: rubricProductionConfiguration object List
	 */
	public List<RubricProductionConfiguration> getRubricProductionConfigurationByRubricId(long rubricId) {
		return find("rubric", rubricId).list();
	}

    /**
	 * Return RubricProductionConfigurations list from data base
	 *
	 * @param String: The Production id
	 * @return List : The RubricProductionConfiguration list
	 */
	public List<RubricProductionConfiguration> getRubricProductionConfigurationByProductionId(long productionId) {
		return find("production", productionId).list();
	}

    public RubricProductionConfiguration mergeRubricProductionConfiguration(
        final RubricProductionConfiguration configuration
    ) {
        return Panache.getEntityManager().merge(configuration);
    }

    public void persistRubricProductionConfiguration(
        final RubricProductionConfiguration configuration
    ) {
        Panache.getEntityManager().persist(configuration);
    }

    public void removeRubricProductionConfiguration(
        final RubricProductionConfiguration configuration
    ) {
        Panache.getEntityManager().remove(configuration);
    }
}
