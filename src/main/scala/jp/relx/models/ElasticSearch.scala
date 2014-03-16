package jp.relx.models

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest

object ElasticSearch {

  val host = "localhost"

  val port = 9300

  val indexName = "livedoor-gourmet"

  val client = new TransportClient().addTransportAddress(
    new InetSocketTransportAddress(host, port))

  def refreshIndex = client.admin().indices().refresh(
    new RefreshRequest(ElasticSearch.indexName)).actionGet()

  def deleteIndex = client.admin().indices().delete(
    new DeleteIndexRequest(ElasticSearch.indexName)).actionGet()

  def createIndex {
    val body = """{
      "settings": {
        "index": {
          "number_of_shards": 5,
          "number_of_replicas": 1
        },
        "analysis": {
          "tokenizer": {
            "ngram_tokenizer": {
              "type": "nGram",
              "min_gram": 2,
              "max_gram": 3,
              "token_chars": ["letter", "digit"]
            }
          },
          "filter": {
          },
          "analyzer": {
            "ngram_analyzer": {
              "type": "custom",
              "tokenizer": "ngram_tokenizer",
              "filter": ["lowercase", "stop"]
            }
          }
        }
      },
      "mappings": {
        "restaurant": {
          "_id": {"path": "id"},
          "properties": {
            "id":   {"type": "integer", "index": "not_analyzed"},
            "name": {
              "type": "multi_field",
              "fields": {
                "name":       {"type": "string", "analyzer": "ngram_analyzer"},
                "suggest":    {"type": "string", "analyzer": "kuromoji"},
                "completion": {"type": "completion", "analyzer": "ngram_analyzer"}
              }
            },
            "property":       {"type": "string", "analyzer": "ngram_analyzer"},
            "alphabet":       {"type": "string", "analyzer": "ngram_analyzer"},
            "name_kana":      {"type": "string", "analyzer": "ngram_analyzer"},
            "pref_id":        {"type": "integer", "index": "not_analyzed"},
            "category_ids":   {"type": "integer", "index": "not_analyzed"},
            "zip":            {"type": "string", "index": "not_analyzed"},
            "address":        {"type": "string", "analyzer": "kuromoji"},
            "description":    {"type": "string", "analyzer": "kuromoji"}
          }
        }
      }
    }"""

    client.admin().indices().create(new CreateIndexRequest(indexName).settings(body))
  }

  def indexDoc(typeName: String, json: String) {
    client.prepareIndex(indexName, typeName).setSource(json).execute.actionGet
  }

}