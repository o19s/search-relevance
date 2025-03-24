#!/bin/bash -e

curl -s -X POST "http://localhost:9200/_plugins/search_relevance/schedule?id=1&click_model=coec&max_rank=20&job_name=test2&interval=10" | jq

echo "Scheduled jobs:"
curl -s "http://localhost:9200/search_relevance_scheduler/_search" | jq
