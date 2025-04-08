# OpenSearch Search Relevance Workbench

The Search Relevance Workbench is a set of tools that support users improving search relevance. Users can find all features to optimize search result quality in one place: analyze track user behavior with UBI, manage judgements, run search quality experiments, analyze experiment results, explore search quality tuning options, …

This repository covers the backend part which is implemented as an OpenSearch plugin.

## Example Usage

```
./gradlew build
docker compose build && docker compose up
```

After the container is running:

```
./scripts/initialize-ubi-indexes.sh
```

Then index the ESCI data:

```
cd data/esci
./index-ubi-queries-events.sh
./index-ecommerce-products.sh
```

Then to make judgments:

```
cd scripts
./create-coec-judgments.sh
```

Then a query set  still under scripts):

```
./create-query-set-using-random-sampling.sh
```

To see the judgments

```
./get-judgments.sh
```

And to see the query sets:

```
./get-query-sets.sh
```

All of the scripts under `scripts/` can be used similarly.
