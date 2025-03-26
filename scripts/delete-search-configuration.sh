#!/bin/bash -e

curl -s -X DELETE "http://localhost:9200/_plugins/search_relevance/search_configurations/$1"
