package com.heroku.seiyulab.routes;

import com.heroku.seiyulab.MediaCode;
import com.heroku.seiyulab.util.WikiParse;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.rx.ReactiveCamel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rx.observables.ConnectableObservable;

@Component
public class Matomete {

    private final Map<String, ObservableSourceBuilder> sources;

    @Autowired
    public Matomete(CamelContext context) throws Exception {
        sources = new HashMap<>();

        for (MediaCode code : MediaCode.values()) {
            sources.put(code.name(), new ObservableSourceBuilder(context, code.name(), code.period, (Exchange exchange) -> {
                List<Map<String, Object>> mapList = new WikiParse()
                        .setParam("action=query&list=categorymembers&cmtitle=Category:" + URLEncoder.encode(code.categoryName, "UTF-8") + "&cmlimit=500&cmnamespace=0&format=xml&continue=&cmprop=title")
                        .setList("categorymembers").setMap("cm").setContinueElement("cmcontinue")
                        .getMapList();
                mapList.forEach((m) -> m.put("c", code.name()));
                exchange.getIn().setBody(mapList);
            }));
        }
        sources.put("female_seiyu_category_members", new ObservableSourceBuilder(context, "female_seiyu_category_members", "27m", (Exchange exchange) -> {
            List<Map<String, Object>> mapList = new WikiParse()
                    .setParam("action=query&list=categorymembers&cmtitle=Category:%E6%97%A5%E6%9C%AC%E3%81%AE%E5%A5%B3%E6%80%A7%E5%A3%B0%E5%84%AA&cmlimit=500&cmnamespace=0&format=xml&continue=&cmprop=title|ids|sortkeyprefix")
                    .setList("categorymembers").setMap("cm").setContinueElement("cmcontinue")
                    .getMapList();
            mapList.forEach((m) -> m.put("gender", "f"));
            exchange.getIn().setBody(mapList);
        }));
        sources.put("male_seiyu_category_members", new ObservableSourceBuilder(context, "male_seiyu_category_members", "28m", (Exchange exchange) -> {
            List<Map<String, Object>> mapList = new WikiParse()
                    .setParam("action=query&list=categorymembers&cmtitle=Category:%E6%97%A5%E6%9C%AC%E3%81%AE%E7%94%B7%E6%80%A7%E5%A3%B0%E5%84%AA&&cmlimit=500&cmnamespace=0&format=xml&continue=&cmprop=title|ids|sortkeyprefix")
                    .setList("categorymembers").setMap("cm").setContinueElement("cmcontinue")
                    .getMapList();
            mapList.forEach((m) -> m.put("gender", "m"));
            exchange.getIn().setBody(mapList);
        }));
        sources.put("seiyu_template_include_pages", new ObservableSourceBuilder(context, "seiyu_template_include_pages", "29m", (Exchange exchange) -> {
            exchange.getIn().setBody(new WikiParse().setParam("action=query&list=backlinks&bltitle=Template:%E5%A3%B0%E5%84%AA&format=xml&bllimit=500&blnamespace=0&continue=")
                    .setList("backlinks")
                    .setMap("bl")
                    .setContinueElement("blcontinue")
                    .getMapList());
        }));
    }

    public ConnectableObservable<List<Map<String, Object>>> getObservableByMapList(String sourceName) {
        return sources.get(sourceName).getObservableByMapList();
    }

    public ConnectableObservable<Set<String>> getObservableBySet(String sourceName) {
        return sources.get(sourceName).getObservableBySet();
    }
}

class ObservableSourceBuilder extends RouteBuilder {

    private final String sourceName, period;
    private final Processor processor;
    private final ConnectableObservable observable;

    public ObservableSourceBuilder(CamelContext context, String sourceName, String period, Processor processor) throws Exception {
        this.sourceName = sourceName;
        this.period = period;
        this.processor = processor;
        observable = new ReactiveCamel(context).toObservable("direct:" + sourceName, Object.class)
                .distinct((obj) -> obj.hashCode())
                .publish();
        observable.connect();
        context.addRoutes(this);
    }

    @Override
    public void configure() throws Exception {
        fromF("timer:%s?period=%s", sourceName, period)
                .process(processor)
                .toF("direct:%s", sourceName);
    }

    public ConnectableObservable<List<Map<String, Object>>> getObservableByMapList() {
        return (ConnectableObservable<List<Map<String, Object>>>) observable;
    }

    public ConnectableObservable<Set<String>> getObservableBySet() {
        return (ConnectableObservable<Set<String>>) observable;
    }
}
