package com.heroku.seiyulab;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;

@Configuration
public class MongoConfig {

    @Bean
    MongoDatabase mongoDatabase() {
        String env = System.getenv("MONGOLAB_URI");
        String databaseName = env.replaceFirst("^.+/", "");
        return MongoClients.create(env)
                .getDatabase(databaseName);
    }
}
