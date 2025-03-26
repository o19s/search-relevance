#!/bin/bash -e

curl -s -X GET "http://localhost:9200/_plugins/search_relevance/search_configurations" | jq
