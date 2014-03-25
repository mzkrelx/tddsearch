package jp.relx.models

import scala.collection.JavaConversions._
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.suggest.Suggest.Suggestion
import org.elasticsearch.search.suggest.term.TermSuggestion.Entry
import org.elasticsearch.search.suggest.completion.CompletionSuggestionBuilder
import org.elasticsearch.action.suggest.SuggestResponse





object Restaurant {

  val typeName = "restaurant"

  val seachRequestJson = s"""
    {
      "query": {
        "multi_match": {
          "query": "%s",
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
          "text": "%s",
          "term": {
            "field": "name",
            "size": 3
          }
        }
      },
      "fields": ["id"],
      "size": 100
    }
  """

// setSource できないっぽいから使えない?
  val completionRequestJson = s"""
    {
      "suggest": {
        "name_completion": {
          "text": "%s",
          "completion": {
            "field": "name"
          }
        }
      },
      "size": 5
    }
  """

  def search(query: String): SearchResult = {

    val response = ElasticSearch.client.prepareSearch()
      .setTypes(typeName)
      .setSource(seachRequestJson.format(query, query))
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
            e.getOptions().toList map { o =>
              SuggestOption(o.getText().toString, o.getScore())
            }
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

  def completion(query: String): Seq[String] = {
//    val response = ElasticSearch.client.prepareSearch()
//      .setTypes(typeName)
//      .setSource(completionRequestJson.format(query))
//      .execute()
//      .actionGet()
    val response = ElasticSearch.client.prepareSuggest("livedoor-gourmet")
        .addSuggestion(
          new CompletionSuggestionBuilder("name_completion")
            .field("name.completion")
            .text(query)
            .size(10)
        ).execute().actionGet()
//    println(response)

    val suggestEntriesList = response.getSuggest().iterator().toList map { s =>
      SuggestEntries(
        s.getName(),
        s.getEntries() map { e =>
          SuggestEntry(
            e.getText().toString,
            e.getLength(),
            e.getOffset(),
            e.getOptions().toList map { o =>
              SuggestOption(o.getText().toString, o.getScore())
            }
          )
        }
      )
    }
    val nameCompletionOption: Option[Seq[SuggestEntry]] = suggestEntriesList.find{ _.name == "name_completion" }.map { _.entries }
    val suggestsOption: Option[Seq[String]] = nameCompletionOption.map { _.map{_.options}.flatten.map(_.text) }
    suggestsOption match {
      case None => Nil
      case Some(seq) => seq
    }
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

