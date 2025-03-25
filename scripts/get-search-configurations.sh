#!/bin/bash -e

curl -s "http://localhost:9200/srw_search_configurations/_search" | jq
