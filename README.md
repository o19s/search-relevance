# OpenSearch Search Relevance Workbench

The Search Relevance Workbench is a set of tools that support users improving search relevance. Users can find all features to optimize search result quality in one place: analyze track user behavior with UBI, manage judgements, run search quality experiments, analyze experiment results, explore search quality tuning options, …

This repository covers the backend part which is implemented as an OpenSearch plugin.

## Example Usage

The following steps assume you have Chorus (OpenSearch edition) cloned and at least once exectued the quickstart.sh script. We are relying on the data used in that repository.
If you already have Chorus you can skip the next steps until "Build and run OpenSearch with the plugin"

Clone Chorus OpenSearch:

```
cd ..
```
```
git clone https://github.com/o19s/chorus-opensearch-edition.git
```

Run the `quickstart.sh` script:
```
cd chorus-opensearch-edition
```
```
./quickstart.sh
```

Stop Chorus and change into the plugin directory:
```
./quickstart.sh --stop
```
```
cd ../search-relevance
```

Build and run OpenSearch with the plugin:

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
cd data-esci
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
