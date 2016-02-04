package com.heroku.seiyulab.routes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
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
public class MediaCategoryMembers extends RouteBuilder {

    private final ConnectableObservable<List<Map<String, Object>>> observable;
    private static int oldHash = 0;

    public ConnectableObservable<List<Map<String, Object>>> getObservable() {
        return observable;
    }

    @Autowired
    public MediaCategoryMembers(CamelContext context, Matomete matomete) {
        Set<String> ngTitle = new HashSet<>();
        ngTitle.add("アニラジ");
        ngTitle.add("HiBiKi Radio Station");
        ngTitle.add("音泉");
        ngTitle.add("超!A&G+");
        ngTitle.add("アニメイトTV");
        observable = Observable
                .combineLatest(
                        Arrays.asList(matomete.getObservableByMapList("anime_2015"),
                                matomete.getObservableByMapList("anime_2016"),
                                matomete.getObservableByMapList("anime_2017"),
                                matomete.getObservableByMapList("game_2015"),
                                matomete.getObservableByMapList("game_2016"),
                                matomete.getObservableByMapList("game_2017"),
                                matomete.getObservableByMapList("ova_2015"),
                                matomete.getObservableByMapList("ova_2016"),
                                matomete.getObservableByMapList("ova_2017"),
                                matomete.getObservableByMapList("amovie_2015"),
                                matomete.getObservableByMapList("amovie_2016"),
                                matomete.getObservableByMapList("amovie_2017"),
                                matomete.getObservableByMapList("movie_2015"),
                                matomete.getObservableByMapList("movie_2016"),
                                matomete.getObservableByMapList("movie_2017"),
                                matomete.getObservableByMapList("drama_2015"),
                                matomete.getObservableByMapList("drama_2016"),
                                matomete.getObservableByMapList("drama_2017"),
                                matomete.getObservableByMapList("tv_2015"),
                                matomete.getObservableByMapList("tv_2016"),
                                matomete.getObservableByMapList("tv_2017"),
                                matomete.getObservableByMapList("production"),
                                matomete.getObservableByMapList("continuation"),
                                matomete.getObservableByMapList("aradio"),
                                matomete.getObservableByMapList("aginfo"),
                                matomete.getObservableByMapList("seiyuvar"),
                                matomete.getObservableByMapList("asong"),
                                matomete.getObservableByMapList("radio_2015"),
                                matomete.getObservableByMapList("radio_2016"),
                                matomete.getObservableByMapList("radio_2017"),
                                matomete.getObservableByMapList("ar_hibiki"),
                                matomete.getObservableByMapList("ar_onsen"),
                                matomete.getObservableByMapList("ar_aandg"),
                                matomete.getObservableByMapList("ar_animatetv")),
                        (args) -> {
                            List<Map<String, Object>> allMedia = new ArrayList<>();
                            int i = 0;
                            for (Object arg : args) {
                                List<Map<String, Object>> list = (List<Map<String, Object>>) arg;
//                                System.out.println(++i + "th media: " + list.size());
                                allMedia.addAll(list);
                            }

                            allMedia = allMedia.stream()
                            .filter((map) -> !ngTitle.contains((String) map.get("title")))
                            .collect(Collectors.toList());
                            System.out.println("allmedia is recalculated.");
                            return allMedia;
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
        new ReactiveCamel(context).sendTo(observable, "direct:media_category_members_start");
    }

    @Override
    public void configure() throws Exception {
        from("direct:media_category_members_start").marshal().json(JsonLibrary.Jackson).setHeader(Exchange.FILE_NAME, constant("media.json")).to("file:src/main/webapp/api");
    }
}
