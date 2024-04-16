package edu.ifrs.cooperativeeditor.repository;

import edu.ifrs.cooperativeeditor.model.Contribution;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class ContributionRepository implements PanacheRepository<Contribution> {

    /**
	 *  Return a user from data base
	 *
	 * @param Object indexer : The indexer can be the user's id or e-mail
	 * @return User: the user object
	 */
	public Contribution getLastContribution(String hashProduction) {
		return find("url = ?1 ORDER BY id desc", hashProduction).firstResult();
	}

	public void persistContribution(Contribution contribution) {
		Panache.getEntityManager().persist(contribution);
	}

}
