package com.heroku.seiyulab.rx;

import com.heroku.seiyulab.routes.MediaCategoryMembers;
import com.heroku.seiyulab.MediaCode;
import com.heroku.seiyulab.MessageMaker;
import com.heroku.seiyulab.Twitter4jWrapper;
import com.heroku.seiyulab.WikiRecentchangesRepository;
import com.heroku.seiyulab.routes.SeiyuRecentchangesHasNewTitles;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import twitter4j.TwitterException;

@Component
public class SeiyuRecentchangesHasNewMedia {

    @Autowired
    public SeiyuRecentchangesHasNewMedia(SeiyuRecentchangesHasNewTitles srhnt, MediaCategoryMembers acm, WikiRecentchangesRepository repository, Twitter4jWrapper wrapper) {
        Observable.combineLatest(srhnt.getObservable(), acm.getObservable(), (map, mediaTitles) -> {
            Map<String, Set<String>> changes = (Map<String, Set<String>>) map.get("changes");
            Set<String> addTitles = changes.get("add");
            Map<String, Set<String>> addExpression = new LinkedHashMap<>();
            Set<String> deleteTitles = changes.get("delete");
            Map<String, Set<String>> deleteExpression = new LinkedHashMap<>();
            List<Map<String, Object>> changed = mediaTitles.stream()
                    .map((m) -> {
                        String title = m.get("title").toString();
                        Map<String, Object> result = new LinkedHashMap<>();
                        if (addTitles.contains(title)) {
                            result.put("type", "add");
                            result.put("data", m);
                            Set<String> categorySet = addExpression.get(title);
                            if (categorySet == null) {
                                categorySet = new LinkedHashSet<>();
                            }
                            categorySet.add(MediaCode.valueOf(m.get("c").toString()).twitterExpression);
                            addExpression.put(title, categorySet);
                        } else if (deleteTitles.contains(title)) {
                            result.put("type", "delete");
                            result.put("data", m);
                            Set<String> categorySet = deleteExpression.get(title);
                            if (categorySet == null) {
                                categorySet = new LinkedHashSet<>();
                            }
                            categorySet.add(MediaCode.valueOf(m.get("c").toString()).twitterExpression);
                            deleteExpression.put(title, categorySet);
                        }
                        return result;
                    })
                    .filter((m) -> !m.isEmpty())
                    .collect(Collectors.toList());
            if (!changed.isEmpty()) {
                map.put("type", "seiyu");
                map.put("changed", changed);
                System.out.println("changed: " + changed + " in " + map.get("title"));
                System.out.println(map);
                org.bson.Document doc = new org.bson.Document(map);
                repository.insert(doc);
                String tweet = null;
                try {
                    if (addExpression.isEmpty()) {
                        String deleteString = String.join(",", deleteExpression.entrySet().stream()
                                .map((entry) -> String.format("%s%s", entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList()));
                        MessageMaker maker = new MessageMaker("[[simple:title]]さんの記事から、以下のキーワードが削除されました。\n%s\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                        tweet = maker.make(map, deleteString);
                    } else if (deleteExpression.isEmpty()) {
                        String addString = String.join(",", addExpression.entrySet().stream()
                                .map((entry) -> String.format("%s%s", entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList()));
                        MessageMaker maker = new MessageMaker("[[simple:title]][[twitter]]さんの記事に、以下のキーワードが追加されました。\n%s\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                        tweet = maker.make(map, addString);
                    } else {
                        String addString = String.join(",", addExpression.entrySet().stream()
                                .map((entry) -> String.format("%s%s", entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList()));
                        String deleteString = String.join(",", deleteExpression.entrySet().stream()
                                .map((entry) -> String.format("%s%s", entry.getKey(), entry.getValue()))
                                .collect(Collectors.toList()));
                        MessageMaker maker = new MessageMaker("[[simple:title]][[twitter]]さんの記事に、%sが追加され、\n%sが削除されました。\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                        tweet = maker.make(map, addString, deleteString);
                    }
                    //wrapper.getTwitter().updateStatus(tweet);
                } catch (UnsupportedEncodingException/* | TwitterException*/ ex) {
                    System.out.println("tweet error: " + tweet);
                    Logger.getLogger(MediaRecentchangesHasNewSeiyu.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return 0;
        }).subscribe();
    }
}
