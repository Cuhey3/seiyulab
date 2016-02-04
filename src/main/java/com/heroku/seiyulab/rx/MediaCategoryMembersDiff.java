package com.heroku.seiyulab.rx;

import com.heroku.seiyulab.routes.MediaCategoryMembers;
import com.heroku.seiyulab.WikiDiffRepository;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MediaCategoryMembersDiff {

    @Autowired
    public MediaCategoryMembersDiff(MediaCategoryMembers mcm, WikiDiffRepository repository) {
        mcm.getObservable()
                .buffer(2, 1)
                .filter((list) -> list.size() == 2)
                .forEach((list) -> {
                    List<Map<String, Object>> oldList = list.get(0);
                    List<Map<String, Object>> newList = list.get(1);
                    newList.stream()
                            .filter((m) -> !oldList.contains(m))
                            .forEach((m) -> {
                                org.bson.Document doc = new org.bson.Document();
                                doc.put("type", "add");
                                doc.put("data", m);
                                doc.put("timestamp", System.currentTimeMillis());
                                repository.insert(doc);
                            });
                    oldList.stream()
                            .filter((m) -> !newList.contains(m))
                            .forEach((m) -> {
                                org.bson.Document doc = new org.bson.Document();
                                doc.put("type", "delete");
                                doc.put("data", m);
                                doc.put("timestamp", System.currentTimeMillis());
                                repository.insert(doc);
                            });

                });
    }
}
