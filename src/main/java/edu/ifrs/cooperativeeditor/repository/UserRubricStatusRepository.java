package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.UserRubricStatus;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class UserRubricStatusRepository implements PanacheRepository<UserRubricStatus> {

    public UserRubricStatus getUserRubricStatussByHashProduction(String hashProduction) {
            return find("inner join production p where p.url = ?1", hashProduction)
            .firstResult();
    }

    public UserRubricStatus getUserRubricStatusByUserIdAndProductionId(Long userId,Long productionId) {
        return find("user = ?1 AND production = ?2", userId, productionId)
        .firstResult();
	}

    public List<UserRubricStatus> getUserRubricStatusByRubricIdAndProductionId(Long rubricId,Long productionId) {

        return find("rubric = ?1 AND production = ?2", rubricId, productionId).list();
	}

    public UserRubricStatus mergeUserRubricStatus(
        final UserRubricStatus userRubricStatus
    ) {
        return Panache.getEntityManager().merge(userRubricStatus);
    }

    public void persistUserRubricStatus(
        final UserRubricStatus userRubricStatus
    ) {
        Panache.getEntityManager().persist(userRubricStatus);
    }
}
