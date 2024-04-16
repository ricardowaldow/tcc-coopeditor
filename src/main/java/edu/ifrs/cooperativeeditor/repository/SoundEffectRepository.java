package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import com.arjuna.ats.internal.jdbc.drivers.modifiers.list;

import edu.ifrs.cooperativeeditor.model.SoundEffect;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

public class SoundEffectRepository implements PanacheRepository<SoundEffect> {


    /**
	 * Return a user from data base
	 *
	 * @param long: The user id
	 * @return User: the user object
	 */
	public Long countSoundEffect() {
		return count();
    }

	/**
	 * Return SoundEffect  list from data base
	 *
	 * @param long: The Production id
	 * @return List: SoundEffect object List
	 */

	@SuppressWarnings("unchecked")
	public List<SoundEffect> getSoundEffectNotInProduction(long idProduction) {
		return find("SELECT se.* FROM sound_effect se WHERE se.id NOT IN ( SELECT upc.sound_effect_id "
		+ " FROM user_production_configuration upc WHERE upc.production_id = ?1 )", idProduction).list();
	}

	/**
	 * Return SoundEffect from data base
	 *
	 * @param long: The SoundEffect id
	 * @return SoundEffect: SoundEffect object
	 */
	public SoundEffect getSoundEffect(long idSoundEffect) {
		return findById(idSoundEffect);
	}
}
