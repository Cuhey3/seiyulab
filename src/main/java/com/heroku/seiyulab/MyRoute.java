package com.heroku.seiyulab;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MyRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        //from("jetty:http://0.0.0.0:" + System.getenv("PORT")).setBody(constant("Hello world!!"));
        /*        from("timer:foo?repeatCount=1").process((Exchange exchange) -> {
            Document doc = Jsoup.connect("http://www.koepota.jp/eventschedule/").maxBodySize(Integer.MAX_VALUE).get();
            doc.select("#eventschedule tr:eq(0)").remove();
            Elements select = doc.select("#eventschedule tr");
            List<org.bson.Document> collect = select.stream().map((el) -> {
                return new KoepotaEvent(
                        el.select("td.title a").attr("href").replace("http://www.koepota.jp/eventschedule/", ""),
                        el.select("td.day").get(0).text(),
                        el.select("td.title").text(),
                        el.select("td.hall").text(),
                        el.select("td.number").text(),
                        el.select("td.day").get(1).text()
                ).getDocument();
            }).collect(Collectors.toList());
            exchange.getIn().setBody(collect);
        }).split().body(List.class).to("direct:mybean1");*/
 /*Observable<org.bson.Document> rxdocs = rxmessage.map((Message message) -> message.getBody(org.bson.Document.class));
        Publisher<org.bson.Document> toPublisher = RxReactiveStreams.toPublisher(rxdocs);
        repository.insert(toPublisher).subscribe(new Subscriber<Long>() {

            @Override
            public void onSubscribe(Subscription s) {
                System.out.println("a");
            }

            @Override
            public void onNext(Long t) {
                System.out.println("b");
            }

            @Override
            public void onError(Throwable thrwbl) {
                System.out.println("c");
            }

            @Override
            public void onComplete() {
                System.out.println("d");
            }
        });*/
    }
}
