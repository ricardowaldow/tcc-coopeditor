package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.Rubric;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class RubricRepository implements PanacheRepository<Rubric> {

    /**
	 * Return a rubric from data base
	 *
	 * @param long: The rubric id
	 * @return Rubric: The rubric object
	 */
	public Rubric getRubric(long id) {
		return findById(id);
	}

    public List<Rubric> getRubricsByPartObjctive(String partObjective) {
		return find("objective like ?1", partObjective).list();
	}

    public Rubric mergeRubric(final Rubric rubric) {
        return Panache.getEntityManager().merge(rubric);
    }

    public void persistRubric(Rubric rubric) {
        Panache.getEntityManager().persist(rubric);
    }

    public void removeRubric(Rubric rubric) {
        Panache.getEntityManager().remove(rubric);
    }
}
