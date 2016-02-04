package com.heroku.seiyulab.rx;

import com.heroku.seiyulab.routes.MediaCategoryMembers;
import com.heroku.seiyulab.routes.Recentchanges;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.observables.ConnectableObservable;

@Component
public class MediaRecentchanges {

    private final ConnectableObservable<Map<String, Object>> observable;

    public ConnectableObservable<Map<String, Object>> getObservable() {
        return observable;
    }

    @Autowired
    public MediaRecentchanges(CamelContext context, Recentchanges rc, MediaCategoryMembers media) {
        observable = Observable
                .combineLatest(rc.getObservable(), media.getObservable(), (map, animeMapList) -> {
                    try {
                        String title = map.get("title").toString();
                        List<Map<String, Object>> collect = animeMapList.stream()
                                .filter((m) -> m.get("title").equals(title))
                                .collect(Collectors.toList());
                        if (collect.isEmpty()) {
                            return null;
                        } else {
                            map.put("categories", collect);
                            return map;
                        }
                    } catch (Throwable t) {
                        Logger.getLogger(MediaRecentchanges.class.getName()).log(Level.SEVERE, null, t);
                        return null;
                    }
                })
                .filter((map) -> map != null)
                .distinct((map) -> map.get("rcid"))
                .publish();
        observable.connect();
                observable.map((obj)->0)
                .buffer(1, TimeUnit.HOURS)
                .forEach((count) -> System.out.println("media_rc: " + count.size() + " count."));
    }
}
