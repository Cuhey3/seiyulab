package com.heroku.seiyulab;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

@Component
public class Twitter4jWrapper {

    public Twitter getTwitter() {
        return twitter;
    }

    private final Twitter twitter;

    public Twitter4jWrapper() throws IOException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        String json = Jsoup.connect("http://myknocker.herokuapp.com/content?key=" + System.getenv("MONGOLAB_URI")).timeout(Integer.MAX_VALUE).get().text();
        System.out.println(json);
        ObjectMapper mapper = new ObjectMapper();
        Map<String, Map> readValue = mapper.readValue(json, Map.class);
        Map<String, String> value = readValue.get("value");
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey(value.get("OAuthConsumerKey"))
                .setOAuthConsumerSecret(value.get("OAuthConsumerSecret"))
                .setOAuthAccessToken(value.get("OAuthAccessToken"))
                .setOAuthAccessTokenSecret(value.get("OAuthAccessTokenSecret"));
        TwitterFactory tf = new TwitterFactory(cb.build());
        twitter = tf.getInstance();
    }

}
