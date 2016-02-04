package com.heroku.seiyulab.routes;

import com.heroku.seiyulab.rx.SeiyuRecentchanges;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.rx.ReactiveCamel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.observables.ConnectableObservable;

@Component
public class SeiyuRecentchangesHasNewTitles extends RouteBuilder {

    public ConnectableObservable<Map<String, Object>> getObservable() {
        return observable;
    }

    private final ConnectableObservable<Map<String, Object>> observable;

    @Autowired
    public SeiyuRecentchangesHasNewTitles(CamelContext context, SeiyuRecentchanges sr) {
        ReactiveCamel rx = new ReactiveCamel(context);
        rx.sendTo(sr.getObservable(), "direct:seiyu_recentchanges_has_new_titles_start");
        observable = new ReactiveCamel(context).toObservable("direct:seiyu_recentchanges_has_new_titles", Map.class)
                .map((m) -> (Map<String, Object>) m)
                .distinct((map) -> map.hashCode())
                .publish();
        observable.connect();
        observable.map((obj) -> obj.get("title"))
                .buffer(1, TimeUnit.HOURS)
                .forEach((count) -> System.out.println("seiyu_rc_new: " + count.size() + " count." + count));
    }

    @Override
    public void configure() throws Exception {
        from("direct:seiyu_recentchanges_has_new_titles_start")
                .to("direct:new_titles")
                .filter((exchange) -> exchange.getIn().getBody(Map.class).containsKey("changes"))
                .to("direct:seiyu_recentchanges_has_new_titles");
    }
}
