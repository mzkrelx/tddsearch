package jp.relx.models

import scala.collection.JavaConversions._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.suggest.Suggest.Suggestion
import org.elasticsearch.search.suggest.term.TermSuggestion.Entry
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder
import org.elasticsearch.action.suggest.SuggestResponse





object Restaurant {

  val typeName = "restaurant"

  def search(query: String) = {
    val json = s"""{
      "query": {
        "multi_match": {
          "query": "$query",
          "fields": [
            "name^10",
            "address",
            "description"
          ]
        }
      },
      "facets": {
        "pref_id_facet": {
          "terms": {
            "field": "pref_id",
            "size": 10
          }
        },
        "category_ids_facet": {
          "terms": {
            "field": "category_ids",
            "size": 10
          }
        }
      },
      "suggest": {
        "name_suggest": {
          "text": "$query",
          "term": {
            "field": "name",
            "size": 3
          }
        }
      },
      "fields": ["id"],
      "size": 100
    }"""

    val response = ElasticSearch.client.prepareSearch()
      .setTypes(typeName)
      .setSource(json)
      .execute()
      .actionGet()

    val suggestEntriesList = response.getSuggest().iterator().toList map { s =>
      SuggestEntries(
        s.getName(),
        s.getEntries() map { e =>
          SuggestEntry(
              e.getText().toString,
              e.getLength(),
              e.getOffset(),
              e.getOptions().toList map { o => SuggestOption(o.getText().toString, o.getScore()) }
          )
        }
      )
    }
    val nameSuggestOption: Option[Seq[SuggestEntry]] = suggestEntriesList.find{ _.name == "name_suggest" }.map { _.entries }
    val suggestsOption: Option[Seq[String]] = nameSuggestOption.map { _.map{_.options}.flatten.map(_.text) }
    val suggests = suggestsOption match {
      case None => Nil
      case Some(seq) => seq
    }

    SearchResult(
      response.getHits.getHits.toList.map { _.getFields.get("id").getValue[Int] },
      suggests,
      response
    )
  }
}

case class SuggestEntries(name: String, entries: Seq[SuggestEntry])
case class SuggestEntry(queryText: String, length: Int, offset: Int, options: Seq[SuggestOption])
case class SuggestOption(text: String, score: Float)

case class SearchResult(
  ids: Seq[Int],
  suggests: Seq[String],
  response: SearchResponse
)

