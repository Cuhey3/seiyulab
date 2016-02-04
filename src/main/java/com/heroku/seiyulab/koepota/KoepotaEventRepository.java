package com.heroku.seiyulab.koepota;
public class KoepotaEventRepository {

   /* private static final Logger logger = LoggerFactory
            .getLogger(KoepotaEventRepository.class);

    private final MongoCollection<Document> collection;

    @Autowired
    public KoepotaEventRepository(MongoDatabase database) {
        this.collection = database.getCollection("koepotaEvent");
    }

        public void insert(List<Document> docs) {
        RxReactiveStreams.toObservable(this.collection.insertMany(docs)).subscribe();
    }
    public void insert(Document doc) {
        RxReactiveStreams.toObservable(this.collection.updateOne(new Document().append("url", doc.get("url")), new Document("$set",doc), new UpdateOptions().upsert(true))).subscribe();
    }

    public Publisher<Long> insert(Publisher<Document> koepotaEventPublisher) {
        return Streams.wrap(koepotaEventPublisher)
                .map(
                        (Document koepotaEvent) -> {
                            logger.info("insert {}", koepotaEvent);
                            return this.collection.updateOne(new Document().append("url", koepotaEvent.get("url")), koepotaEvent, new UpdateOptions().upsert(true));
                        })
                .reduce(0L, (x, y) -> x + 1L);
    }

    public Publisher<KoepotaEvent> findAll() {
        return Streams.wrap(this.collection.find()).map(
                doc -> new KoepotaEvent(doc));
    }*/
}
