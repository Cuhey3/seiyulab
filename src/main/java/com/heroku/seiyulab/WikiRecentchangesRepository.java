package com.heroku.seiyulab;

import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.reactivestreams.client.MongoDatabase;
import rx.RxReactiveStreams;

@Repository
public class WikiRecentchangesRepository {

    private static final Logger logger = LoggerFactory
            .getLogger(WikiRecentchangesRepository.class);

    private final MongoCollection<Document> collection;

    @Autowired
    public WikiRecentchangesRepository(MongoDatabase database) {
        this.collection = database.getCollection("wikirc");
    }

    public void insert(Document doc) {
        RxReactiveStreams.toObservable(this.collection.updateOne(new Document().append("rcid", doc.get("rcid")), new Document("$set",doc), new UpdateOptions().upsert(true))).subscribe();
    }
}
