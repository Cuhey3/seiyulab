package com.heroku.seiyulab;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MessageMaker {

    String template;
    Pattern p = Pattern.compile("\\[\\[(.+?)\\]\\]");

    public MessageMaker(String template) {
        this.template = template;
    }

    public String make(Map<String, Object> map) throws UnsupportedEncodingException {
        return make(map, new Object[0]);
    }

    public String make(Map<String, Object> map, Object... args) throws UnsupportedEncodingException {
        Matcher m;
        String result = template;
        if (args.length > 0) {
            result = String.format(result, args);
        }
        while ((m = p.matcher(result)).find()) {
            String attr = m.group(1);
            String key = attr.replaceFirst("^(.+?:)?", "");
            String value;
            if (map.containsKey(key)) {
                Object get = map.get(key);
                if (get == null) {
                    value = "";
                } else {
                    value = get.toString();
                }
            } else {
                value = "";
            }
            if (attr.equals("twitter")) {
                value = (String) map.get("twitter");
                if (value == null) {
                    value = "";
                } else {
                    value = "(@" + value + ")";
                }
            } else if (attr.startsWith("encodespace:")) {
                value = URLEncoder.encode(value.replace(" ", "_"), "UTF-8");
            } else if (attr.startsWith("encode:")) {
                value = URLEncoder.encode(value, "UTF-8");
            } else if (attr.startsWith("simple:")) {
                value = value.replaceFirst("[_ ]\\(.+?\\)$", "");
            } else if (attr.startsWith("choice/")) {
                String[] split = attr.replaceFirst("choice/(.+?):.+", "$1").split(",");
                String fValue = value;
                Optional<String> first = Stream.of(split)
                        .filter((token) -> token.startsWith(fValue + "="))
                        .map((token) -> token.split("=")[1])
                        .findFirst();
                value = first.orElse("");
            }
            result = m.replaceFirst(value);
        }
        return result;
    }
}
