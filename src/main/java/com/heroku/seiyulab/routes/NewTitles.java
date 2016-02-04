package com.heroku.seiyulab.routes;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

@Component
public class NewTitles extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("direct:new_titles").process((Exchange exchange) -> {
            Map<String, Object> map = exchange.getIn().getBody(Map.class);
            Document doc = Jsoup.connect("https://ja.wikipedia.org/w/api.php?action=parse&prop=wikitext|links&oldid=" + map.get("revid") + "&format=xml")
                    .ignoreContentType(true).timeout(Integer.MAX_VALUE).get();
            Pattern pattern = Pattern.compile("\\Q{{\\E[Tt][Ww][Ii][Tt][Tt][Ee][Rr]\\Q|\\E(.+?)(\\Q|\\E.*?\\Q" + (doc.select("parse").get(0).attr("title").replaceFirst(" \\(.+?\\)$", "")) + "\\E.*?)?\\Q}}\\E");
            String newWikitext = doc.select("wikitext").text();
            Matcher matcher = pattern.matcher(newWikitext.replace(" ", "").replace("　", ""));
            String twitterId = null;
            if (matcher.find()) {
                twitterId = matcher.group(1).replace("|", " ");
            }
            String newWikitextStrip = newWikitext.replace(" ", "").replace("_", "");
            Set<String> newTitles = doc.select("pl[ns=0][exists]").stream()
                    .map((el) -> el.text())
                    .filter((title) -> newWikitextStrip.contains(title.replace(" ", "").replace("_", "")))
                    .collect(Collectors.toSet());
            try {
                Set<String> otherKeyword = newTitles.stream()
                        .map((title) -> Pattern.compile("\\[\\[" + title.replace(" ", "[ _]").replace("(", "\\(").replace(")", "\\)") + "\\|(.+?)\\]\\]"))
                        .map((p) -> p.matcher(newWikitext))
                        .flatMap((m) -> {
                            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new MatcherIterator(m), Spliterator.ORDERED), false);
                        })
                        .map((str) -> str.replace("_", " "))
                        .collect(Collectors.toSet());
                newTitles.addAll(otherKeyword);
            } catch (Throwable t) {
            }
            Set<String> newTitles2 = newTitles.stream()
                    .collect(Collectors.toSet());
            doc = Jsoup.connect("https://ja.wikipedia.org/w/api.php?action=parse&prop=wikitext|links&oldid=" + map.get("old_revid") + "&format=xml")
                    .ignoreContentType(true).timeout(Integer.MAX_VALUE).get();
            String oldWikitext = doc.select("wikitext").text();
            String oldWikitextStrip = oldWikitext.replace(" ", "").replace("_", "");
            Set<String> oldTitles = doc.select("pl[ns=0][exists]").stream()
                    .map((e) -> e.text())
                    .filter((title) -> oldWikitextStrip.contains(title.replace(" ", "").replace("_", "")))
                    .collect(Collectors.toSet());
            try {
                Set<String> otherKeyword2 = oldTitles.stream()
                        .map((title) -> Pattern.compile("\\[\\[" + title.replace(" ", "[ _]").replace("(", "\\(").replace(")", "\\)") + "\\|(.+?)\\]\\]"))
                        .map((p) -> p.matcher(oldWikitext))
                        .flatMap((m) -> {
                            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new MatcherIterator(m), Spliterator.ORDERED), false);
                        })
                        .map((str) -> str.replace("_", " "))
                        .collect(Collectors.toSet());
                oldTitles.addAll(otherKeyword2);
            } catch (Throwable t) {
            }
            newTitles.removeAll(oldTitles);
            oldTitles.removeAll(newTitles2);
            Map<String, Object> result = new LinkedHashMap<>();
            Set<String> dance = new HashSet<>();
            if (!oldTitles.isEmpty()) {
                for (String oldTitle : oldTitles) {
                    Set<String> d = newTitles.stream()
                            .filter((newTitle) -> {
                                String combine1 = ("[[" + newTitle + "|" + oldTitle + "]]").replace(" ", "").replace("_", "");
                                String combine2 = ("[[" + oldTitle + "|" + newTitle + "]]").replace(" ", "").replace("_", "");
                                return newWikitextStrip.contains(combine1) || oldWikitextStrip.contains(combine2);
                            }).collect(Collectors.toSet());
                    if (!d.isEmpty()) {
                        dance.add(oldTitle);
                        dance.addAll(d);
                    }
                }
            }
            if (!dance.isEmpty()) {
                newTitles.removeAll(dance);
                oldTitles.removeAll(dance);
            }
            if (!newTitles.isEmpty() || !oldTitles.isEmpty()) {
                result.put("add", newTitles);
                result.put("delete", oldTitles);
            }
            if (!result.isEmpty()) {
                map.put("changes", result);
                map.put("twitter", twitterId);
            }
        });
    }

}

class MatcherIterator implements Iterator<String> {

    private final Matcher matcher;

    public MatcherIterator(Matcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean hasNext() {
        return matcher.find();
    }

    @Override
    public String next() {
        return matcher.group(1);
    }
}
