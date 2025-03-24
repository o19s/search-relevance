#!/bin/bash -e

#curl -s -X DELETE "http://localhost:9200/srw_query_sets"

curl -s -X POST "http://localhost:9200/_plugins/search_relevance/queryset?name=test&description=fake&sampling=random&query_set_size=20" | jq
