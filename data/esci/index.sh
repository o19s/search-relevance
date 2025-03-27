#!/bin/bash -e

echo "Indexing queries and events..."
curl  -X POST 'http://localhost:9200/index-name/_bulk?pretty' --data-binary @ubi_queries_events.ndjson -H "Content-Type: application/x-ndjson"
