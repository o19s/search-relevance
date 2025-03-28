# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

echo "Deleting existing judgments index..."
curl -s -X DELETE http://localhost:9200/judgments

echo "Creating judgments..."
curl -s -X POST "http://localhost:9200/_plugins/search_relevance/judgments?click_model=coec&max_rank=50"

curl -s "http://localhost:9200/srw_judgments/_search" | jq