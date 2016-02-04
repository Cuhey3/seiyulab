package com.heroku.seiyulab.rx;

import com.heroku.seiyulab.routes.SeiyuCategoryMembersAndIncludeTemplate;
import com.heroku.seiyulab.routes.Recentchanges;
import java.util.Map;
import java.util.Set;
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
public class SeiyuRecentchanges {

    private final ConnectableObservable<Map<String, Object>> observable;

    public ConnectableObservable<Map<String, Object>> getObservable() {
        return observable;
    }

    @Autowired
    public SeiyuRecentchanges(CamelContext context, Recentchanges rc, SeiyuCategoryMembersAndIncludeTemplate scm) {
        observable = Observable
                .combineLatest(rc.getObservable(), scm.getObservable(), (map, seiyu) -> {
                    try {
                        Set<String> seiyuNames = seiyu.stream()
                                .map((m) -> m.get("title").toString())
                                .collect(Collectors.toSet());
                        if (seiyuNames.contains(map.get("title").toString())) {
                            return map;
                        } else {
                            return null;
                        }
                    } catch (Throwable t) {
                        Logger.getLogger(SeiyuRecentchanges.class.getName()).log(Level.SEVERE, null, t);
                        return null;
                    }
                })
                .filter((map) -> map != null)
                .distinct((map) -> map.get("rcid"))
                .publish();
        observable.connect();
        observable.map((obj) -> 0)
                .buffer(1, TimeUnit.HOURS)
                .forEach((count) -> System.out.println("seiyu_rc: " + count.size() + " count."));
    }
}
