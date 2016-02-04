package com.heroku.seiyulab.routes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.camel.rx.ReactiveCamel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.Observable;
import rx.observables.ConnectableObservable;

@Component
public class SeiyuCategoryMembersAndIncludeTemplate extends RouteBuilder {

    private final ConnectableObservable<List<Map<String, Object>>> observable;
    private static int oldHash = 0;

    public ConnectableObservable<List<Map<String, Object>>> getObservable() {
        return observable;
    }

    @Autowired
    public SeiyuCategoryMembersAndIncludeTemplate(CamelContext context, Matomete matomete) {
        observable = Observable
                .combineLatest(matomete.getObservableByMapList("female_seiyu_category_members"),
                        matomete.getObservableByMapList("male_seiyu_category_members"),
                        matomete.getObservableByMapList("seiyu_template_include_pages"),
                        (list1, list2, list3) -> {
                            Set<String> set = list3.stream()
                            .map((m) -> m.get("title").toString())
                            .collect(Collectors.toSet());
                            list1.addAll(list2);
                            List<Map<String, Object>> list = list1.stream()
                            .filter((m) -> set.contains(m.get("title").toString()))
                            .collect(Collectors.toList());
                            System.out.println("scm is ready.");
                            return list;
                        })
                .filter((list) -> {
                    int newHash = list.hashCode();
                    if (oldHash != newHash) {
                        oldHash = newHash;
                        return true;
                    } else {
                        return false;
                    }
                })
                .publish();
        observable.connect();
        new ReactiveCamel(context).sendTo(observable, "direct:seiyu_category_members_and_include_template_start");
    }

    @Override
    public void configure() throws Exception {
        from("direct:seiyu_category_members_and_include_template_start").marshal().json(JsonLibrary.Jackson).setHeader(Exchange.FILE_NAME, constant("seiyu.json")).to("file:src/main/webapp/api");
    }
}
