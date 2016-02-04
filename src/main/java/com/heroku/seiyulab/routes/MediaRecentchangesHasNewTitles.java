package com.heroku.seiyulab.routes;

import com.heroku.seiyulab.rx.MediaRecentchanges;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.rx.ReactiveCamel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.observables.ConnectableObservable;

@Component
public class MediaRecentchangesHasNewTitles extends RouteBuilder {

    public ConnectableObservable<Map<String, Object>> getObservable() {
        return observable;
    }

    private final ConnectableObservable<Map<String, Object>> observable;

    @Autowired
    public MediaRecentchangesHasNewTitles(CamelContext context, MediaRecentchanges ar) {
        ReactiveCamel rx = new ReactiveCamel(context);
        rx.sendTo(ar.getObservable(), "direct:media_recentchanges_has_new_titles_start");
        observable = new ReactiveCamel(context).toObservable("direct:media_recentchanges_has_new_titles", Map.class)
                .map((m) -> (Map<String, Object>) m)
                .distinct((map) -> map.hashCode())
                .publish();
        observable.connect();
        observable.map((map) -> map.get("title"))
                .buffer(1, TimeUnit.HOURS)
                .forEach((count) -> System.out.println("media_rc_new: " + count.size() + " count." + count));
    }

    @Override
    public void configure() throws Exception {
        from("direct:media_recentchanges_has_new_titles_start")
                .to("direct:new_titles")
                .filter((exchange) -> exchange.getIn().getBody(Map.class).containsKey("changes"))
                .to("direct:media_recentchanges_has_new_titles");
    }
}
