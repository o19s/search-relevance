# OpenSearch Search Relevance Workbench

The Search Relevance Workbench is a set of tools that support users improving search relevance. Users can find all features to optimize search result quality in one place: analyze track user behavior with UBI, manage judgements, run search quality experiments, analyze experiment results, explore search quality tuning options, …

This repository covers the backend part which is implemented as an OpenSearch plugin.

## Example Usage

```
./gradlew build
```

Either start OpenSearch backend with:

```
docker compose build && docker compose up
```

or

```
./gradlew run
```

After OpenSearch backend is running (`curl http://localhost:9200`), load the sample ecommerce data:

```
cd data-esci
./index-ecommerce-products.sh
```

Now load the sample UBI data.

```
cd ../scripts
./initialize-ubi-indexes.sh
cd ../data-esci
./index-ubi-queries-events.sh
```


Then make implicit judgements using the sample UBI click data:

```
cd ../scripts
./create-coec-judgments.sh
```

Now create a query set using random sampling:

```
./create-query-set-using-random-sampling.sh
```

To see the judgments:

```
./get-judgments.sh
```

And to see the query sets:

```
./get-query-sets.sh
```

All of the scripts under `scripts/` can be used similarly.


To start back over with a fresh setup do either:

```
docker compose down -v
```

or

```
./gradlew clean
```
