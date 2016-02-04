package com.heroku.seiyulab.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

public class WikiParse {

    private final String url = "https://ja.wikipedia.org/w/api.php";
    private String param;
    private String list;
    private String map;
    private String continueElement;

    public List<Map<String, Object>> getMapList() throws IOException {
        String requestUrl = url + "?" + param;
        Document get = Jsoup.connect(requestUrl).timeout(Integer.MAX_VALUE).get();
        ArrayList<Map<String, Object>> resultList = new ArrayList<>();
        addElementsAsMap(resultList, get.select(list).select(map));
        if (continueElement != null) {

            while (true) {
                Elements els = get.select("continue[" + continueElement + "]");
                if (els.isEmpty()) {
                    break;
                } else {
                    String value = els.first().attr(continueElement);
                    get = Jsoup.connect(requestUrl + "&" + continueElement + "=" + value).timeout(Integer.MAX_VALUE).get();
                    addElementsAsMap(resultList, get.select(list).select(map));
                }
            }
        }
        return resultList;
    }

    public Set<String> getSet(String attr) throws IOException {
        String requestUrl = url + "?" + param;
        Document get = Jsoup.connect(requestUrl).timeout(Integer.MAX_VALUE).get();
        HashSet<String> resultSet = new HashSet<>();
        get.select(list).select(map).stream()
                .map((el) -> el.attr(attr))
                .forEach(resultSet::add);
        if (continueElement != null) {

            while (true) {
                Elements els = get.select("continue[" + continueElement + "]");
                if (els.isEmpty()) {
                    break;
                } else {
                    String value = els.first().attr(continueElement);
                    get = Jsoup.connect(requestUrl + "&" + continueElement + "=" + value).timeout(Integer.MAX_VALUE).get();
                    get.select(list).select(map).stream()
                            .map((el) -> el.attr(attr))
                            .forEach(resultSet::add);
                }
            }
        }
        return resultSet;
    }

    public void addElementsAsMap(List l, Elements elements) {
        elements.stream()
                .map((element) -> {
                    Map<String, String> m = new HashMap<>();
                    StreamSupport.stream(element.attributes().spliterator(), false)
                            .forEach((entry) -> {
                                m.put(entry.getKey(), entry.getValue());
                            });
                    return m;
                })
                .forEach(l::add);
    }

    public WikiParse setParam(String param) {
        this.param = param;
        return this;
    }

    public WikiParse setList(String list) {
        this.list = list;
        return this;
    }

    public WikiParse setMap(String map) {
        this.map = map;
        return this;
    }

    public WikiParse setContinueElement(String continueElement) {
        this.continueElement = continueElement;
        return this;
    }
}
