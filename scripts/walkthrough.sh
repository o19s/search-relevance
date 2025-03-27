# SPDX-License-Identifier: Apache-2.0
#
# The OpenSearch Contributors require contributions made to
# this file be licensed under the Apache-2.0 license or a
# compatible open source license.

#!/bin/bash -e

# Example walkthrough end-to-end for the plugin.

# Delete existing UBI indexes and create new ones.
./initialize-ubi-indexes.sh

# Index the ESCI data.
(cd ../data/esci/; ./index.sh)

# Create judgments.
curl -s -X POST "http://localhost:9200/_plugins/search_relevance/judgments?click_model=coec&max_rank=20"

# Create a query set.
QUERY_SET_ID=`curl -s -X POST "http://localhost:9200/_plugins/search_relevance/query_sets?name=test&description=fake&sampling=pptss&query_set_size=100" | jq -r .query_set`

# Run the query set.
./run-query-set.sh ${QUERY_SET_ID}
