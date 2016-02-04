package com.heroku.seiyulab.rx;

import com.heroku.seiyulab.routes.SeiyuCategoryMembersAndIncludeTemplate;
import com.heroku.seiyulab.MessageMaker;
import com.heroku.seiyulab.Twitter4jWrapper;
import com.heroku.seiyulab.WikiDiffRepository;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import twitter4j.TwitterException;

@Component
public class SeiyuCategoryMembersAndIncludeTemplateDiff {

    @Autowired
    public SeiyuCategoryMembersAndIncludeTemplateDiff(SeiyuCategoryMembersAndIncludeTemplate scmait, WikiDiffRepository repository, Twitter4jWrapper wrapper) {
        scmait.getObservable()
                .buffer(2, 1)
                .filter((list) -> list.size() == 2)
                .forEach((list) -> {
                    List<Map<String, Object>> oldList = list.get(0);
                    Set<String> oldListNames = oldList.stream().map((map) -> (String)map.get("title")).collect(Collectors.toSet());
                    List<Map<String, Object>> newList = list.get(1);
                    Set<String> newListNames = newList.stream().map((map) -> (String)map.get("title")).collect(Collectors.toSet());
                    oldList.stream()
                            .filter((m) -> !newListNames.contains((String)m.get("title")))
                            .forEach((m) -> {
                                org.bson.Document doc = new org.bson.Document();
                                doc.put("type", "delete");
                                doc.put("data", m);
                                doc.put("timestamp", System.currentTimeMillis());
                                repository.insert(doc);
                                String tweet = null;
                                //try {
                                    MessageMaker maker = new MessageMaker("当botが使用する声優のリストから、[[simple:title]]さんが除外されました。"
                                            + "\n・記事が削除された\n・記事が声優のカテゴリに属さなくなった\n・声優のテンプレートを含まなくなった\n・改名した\nなどが考えられます。"
                                            + "\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                                    //wrapper.getTwitter().updateStatus(maker.make(m));
                                /*} catch (UnsupportedEncodingException | TwitterException ex) {
                                    System.out.println("tweet error: " + tweet);
                                    Logger.getLogger(SeiyuCategoryMembersAndIncludeTemplateDiff.class.getName()).log(Level.SEVERE, null, ex);
                                }*/
                            });
                    newList.stream()
                            .filter((m) -> !oldListNames.contains((String)m.get("title")))
                            .forEach((m) -> {
                                org.bson.Document doc = new org.bson.Document();
                                doc.put("type", "add");
                                doc.put("data", m);
                                doc.put("timestamp", System.currentTimeMillis());
                                repository.insert(doc);
                                String tweet = null;
                                MessageMaker maker = new MessageMaker("当botが使用する声優のリストに、[[choice/m=男性の,f=女性の:gender]][[simple:title]]さんが追加されました。\nhttps://ja.wikipedia.org/wiki/[[encodespace:title]]");
                                try {
                                    tweet = maker.make(m);
                                    //wrapper.getTwitter().updateStatus(tweet);
                                } catch (UnsupportedEncodingException/* | TwitterException*/ ex) {
                                    System.out.println("tweet error: " + tweet);
                                    Logger.getLogger(SeiyuCategoryMembersAndIncludeTemplateDiff.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            });
                });
    }
}
