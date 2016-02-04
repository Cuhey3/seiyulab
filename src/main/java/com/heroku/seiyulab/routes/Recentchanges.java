package com.heroku.seiyulab.routes;

import com.heroku.seiyulab.util.WikiParse;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.rx.ReactiveCamel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.observables.ConnectableObservable;

@Component
public class Recentchanges extends RouteBuilder {

    private final ConnectableObservable<Map<String, Object>> observable;

    public ConnectableObservable<Map<String, Object>> getObservable() {
        return observable;
    }

    @Autowired
    public Recentchanges(CamelContext context) {
        observable = new ReactiveCamel(context).toObservable("direct:recentchanges", Map.class)
                .map(m -> (Map<String, Object>) m)
                // .distinct(map -> map.hashCode())
                .publish();
        observable.connect();
        observable.map((obj) -> 0)
                .buffer(1, TimeUnit.HOURS)
                .forEach((count) -> System.out.println("recentchanges: " + count.size() + " count."));
    }

    @Override
    public void configure() throws Exception {
        from("timer:recentchanges?period=30s")
                .process((Exchange exchange) -> {
                    List<Map<String, Object>> mapList = new WikiParse()
                            .setParam("action=query&list=recentchanges&rcnamespace=0&rclimit=200&format=xml&rctype=edit")
                            .setList("recentchanges")
                            .setMap("rc")
                            .getMapList();
                    Collections.reverse(mapList);
                    exchange.getIn().setBody(mapList);
                })
                .split().body(List.class)
                .to("direct:recentchanges");
    }
}
