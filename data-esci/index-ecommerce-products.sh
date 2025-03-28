#dev shortcut file to reindex data.

OPENSEARCH_CHORUS_HOME="../../chorus-opensearch-edition"

echo -e "Creating ecommerce index, defining its mapping & settings\n"
curl -s -X PUT "localhost:9200/ecommerce/" -H 'Content-Type: application/json' --data-binary @${OPENSEARCH_CHORUS_HOME}/opensearch/schema.json
curl -s -X POST "localhost:9200/ecommerce/_bulk?pretty" -H 'Content-Type: application/json' --data-binary @${OPENSEARCH_CHORUS_HOME}/transformed_esci_10.json
