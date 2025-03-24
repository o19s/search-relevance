#!/bin/bash -e

echo "Deleting existing judgments index..."
curl -s -X DELETE http://localhost:9200/judgments

echo "Creating judgments..."
curl -s -X POST "http://localhost:9200/_plugins/search_relevance/judgments?click_model=coec&max_rank=50"
