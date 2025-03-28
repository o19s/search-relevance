#!/bin/bash -e

# Make sure to create the UBI events and queries indexes first otherwise you will run into mapping errors later on.

echo "Indexing queries and events..."
curl  -X POST 'http://localhost:9200/index-name/_bulk?pretty' --data-binary @ubi_queries_events.ndjson -H "Content-Type: application/x-ndjson"
