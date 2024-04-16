package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.User;
import edu.ifrs.cooperativeeditor.model.UserProductionConfiguration;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class UserRepository implements PanacheRepository<User> {

    UserProductionConfigurationRepository upcrep = new UserProductionConfigurationRepository();

    public User mergerUser(User user) {
        return Panache.getEntityManager().merge(user);
    }

    public User getUser(final Object indexer) {
        String column = (indexer instanceof String) ? "email" : "id";
        return find(column, indexer).firstResult();
    }

    public User getUser(String email, String password) {
        return find("email = ?1 AND password = ?2", email, password).firstResult();
    }

    public User getUser(Long userId, String hashProduction) {
        User user = this.getUser(userId);
		UserProductionConfiguration uPC = upcrep.getUserProductionConfigurationByidUserAndHashProduction(user.getId(), hashProduction);
		if(uPC != null)
			user.setUserProductionConfiguration(uPC);
		return user;
    }

	public List<User> getUsersByPartEmail(final String partEmail) {
		return find("email like ?1", partEmail).list();
	}

	public List<User> getAllUsers() {
		return listAll();
	}


}
