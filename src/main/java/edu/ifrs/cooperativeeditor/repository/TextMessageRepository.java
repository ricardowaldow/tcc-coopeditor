package edu.ifrs.cooperativeeditor.repository;

import java.util.List;

import edu.ifrs.cooperativeeditor.model.InputMessage;
import edu.ifrs.cooperativeeditor.model.Production;
import edu.ifrs.cooperativeeditor.model.TextMessage;
import io.quarkus.hibernate.orm.panache.Panache;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

public class TextMessageRepository implements PanacheRepository<TextMessage> {

    public void persistMessage(TextMessage message) {
        Panache.getEntityManager().persist(message);
    }
}
