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
public class WikiDiffRepository {

    private static final Logger logger = LoggerFactory
            .getLogger(WikiDiffRepository.class);

    private final MongoCollection<Document> collection;

    @Autowired
    public WikiDiffRepository(MongoDatabase database) {
        this.collection = database.getCollection("wikidiff");
    }

    public void insert(Document doc) {
        RxReactiveStreams.toObservable(this.collection.insertOne(doc)).subscribe();
    }
}
