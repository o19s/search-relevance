#!/bin/bash -e

curl -s -X DELETE "http://localhost:9200/srw_judgments" | jq
curl -s -X DELETE "http://localhost:9200/srw_query_results" | jq
curl -s -X DELETE "http://localhost:9200/ubi_queries,ubi_events" | jq
