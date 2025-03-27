# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

#curl -s -X DELETE "http://localhost:9200/srw_query_sets"

curl -s -X POST "http://localhost:9200/_plugins/search_relevance/query_sets?name=test&description=fake&sampling=topn&query_set_size=20" | jq
