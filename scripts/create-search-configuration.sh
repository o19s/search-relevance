# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

#curl -s -X DELETE "http://localhost:9200/srw_query_sets"

curl -s -X POST "http://localhost:9200/_plugins/search_relevance/search_configurations" -H "Content-type: application/json" -d'
{
  "search_configuration_name": "hybrid_search_default",
  "query_body":  "{\"query\": {\"multi_match\": {\"query\": \"%SearchText%\", \"fields\": [\"id\", \"title\", \"category\", \"bullets\", \"description\", \"attrs.Brand\", \"attrs.Color\"] }}}"
}' | jq
