package com.heroku.seiyulab.rx;

import com.heroku.seiyulab.routes.SeiyuCategoryMembersAndIncludeTemplate;
import com.heroku.seiyulab.MediaCode;
import com.heroku.seiyulab.MessageMaker;
import com.heroku.seiyulab.Twitter4jWrapper;
import com.heroku.seiyulab.WikiRecentchangesRepository;
import com.heroku.seiyulab.routes.MediaRecentchangesHasNewTitles;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
public class MediaRecentchangesHasNewSeiyu {

    @Autowired
    public MediaRecentchangesHasNewSeiyu(MediaRecentchangesHasNewTitles arhnt,
            SeiyuCategoryMembersAndIncludeTemplate scm,
            WikiRecentchangesRepository repository,
            Twitter4jWrapper wrapper) {
        Observable.combineLatest(arhnt.getObservable(), scm.getObservable(), (map, seiyuTitles) -> {
            Map<String, Set<String>> changes = (Map<String, Set<String>>) map.get("changes");
            Set<String> addTitles = changes.get("add");
            Set<String> addSeiyu = new HashSet<>();
            Set<String> deleteTitles = changes.get("delete");
            Set<String> deleteSeiyu = new HashSet<>();
            List<Map<String, Object>> changed = seiyuTitles.stream()
                    .map((m) -> {
                        String title = m.get("title").toString();
                        Map<String, Object> result = new LinkedHashMap<>();
                        if (addTitles.contains(title)) {
                            addSeiyu.add(title);
                            result.put("type", "add");
                            result.put("data", m);
                        } else if (deleteTitles.contains(title)) {
                            deleteSeiyu.add(title);
                            result.put("type", "delete");
                            result.put("data", m);
                        }
                        return result;
                    })
                    .filter((m) -> !m.isEmpty())
                    .collect(Collectors.toList());
            if (!changed.isEmpty()) {
                map.put("type", "media");
                map.put("changed", changed);
                System.out.println("changed: " + changed + " in " + map.get("title"));
                System.out.println(map);
                org.bson.Document doc = new org.bson.Document(map);
                repository.insert(doc);
                List<Map<String, Object>> categories = (List<Map<String, Object>>) map.get("categories");
                Set<String> collect = categories.stream().map((m) -> m.get("c").toString())
                        .map((name) -> MediaCode.valueOf(name).twitterExpression)
                        .collect(Collectors.toSet());
                String tweet = null;
                try {
                    if (addSeiyu.isEmpty()) {
                        MessageMaker maker = new MessageMaker("記事「[[title]]」%sから、以下の声優名が削除されました。\n%s\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                        tweet = maker.make(map, collect, deleteSeiyu);
                    } else if (deleteSeiyu.isEmpty()) {
                        MessageMaker maker = new MessageMaker("記事「[[title]]」%sに、以下の声優名が追加されました。\n%s\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                        tweet = maker.make(map, collect, addSeiyu);
                    } else {
                        MessageMaker maker = new MessageMaker("記事「[[title]]」%sに、声優名%sが追加され、\n声優名%sが削除されました。\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                        tweet = maker.make(map, collect, addSeiyu, deleteSeiyu);
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
