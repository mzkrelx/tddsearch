package jp.relx
import org.elasticsearch.indices.IndexMissingException
import org.specs2.mutable.Specification
import org.specs2.specification.BeforeExample
import jp.relx.models.ElasticSearch
import jp.relx.models.Restaurant
import org.elasticsearch.search.facet.terms.TermsFacet
import scala.collection.JavaConversions._
import org.elasticsearch.search.suggest.Suggest

class RestaurantSpec extends Specification with BeforeExample {

  args.execute(threadsNb = 1)

  def before = setupES

  def setupES = {
    try {
      ElasticSearch.deleteIndex
    } catch {
      case e: IndexMissingException => // ignore
      case e: Exception => throw e
    }

    ElasticSearch.createIndex

    ElasticSearch.indexDoc(Restaurant.typeName, """{"id": 1001, "name": "らーめん田中", "address": "東京都千代田区", "pref_id": 13}""")
    ElasticSearch.indexDoc(Restaurant.typeName, """{"id": 1002, "name": "新宿ラーメン 来々軒", "address": "神奈川県横浜", "pref_id": 14}""")
    ElasticSearch.indexDoc(Restaurant.typeName, """{"id": 1003, "name": "カフェ Jack", "address": "東京都港区", "pref_id": 13}""")
    ElasticSearch.indexDoc(Restaurant.typeName, """{"id": 1004, "name": "ラーメン東京一番", "address": "東京都新宿区", "pref_id": 13}""")
    ElasticSearch.indexDoc(Restaurant.typeName, """{"id": 1005, "name": "カフェ Taro", "address": "京都府左京区", "pref_id": 26}""")
    ElasticSearch.indexDoc(Restaurant.typeName, """{"id": 1006, "name": "ラーメン 三郎", "address": "東京都大田区", "pref_id": 13}""")

    ElasticSearch.refreshIndex
  }

  "query 'ラーメン'" should {
    Restaurant.search("ラーメン").ids must contain (1002)
    Restaurant.search("ラーメン").ids must contain (1004)
    Restaurant.search("ラーメン").ids must contain (1006)
  }

  "query 'ラーメン東京一番'" should {
    Restaurant.search("ラーメン東京一番").ids must contain (1004)
  }

  "query '新宿ラーメン'" should {
    Restaurant.search("新宿ラーメン").ids must contain (1002)
  }

  "query 'jack' - 大文字小文字" should {
    Restaurant.search("jack").ids must contain (1003)
  }

  "query - tokenizeテスト" should {
    "京都 > 東京都を含まない" in {
      Restaurant.search("京都").ids must not contain (1004)
    }
    "京都 > 京都を含む" in {
      Restaurant.search("京都").ids must contain (1005)
    }
  }

  "boost" should {
    "新宿 > nameに含まれる" in {
      Restaurant.search("新宿").ids must contain (1002)
    }

    "新宿 > addressに含まれる" in {
      Restaurant.search("新宿").ids must contain (1004)
    }

    "新宿 > nameを優先" in {
      val ids = Restaurant.search("新宿").ids
      ids.indexOf(1002) must lessThan (ids.indexOf(1004))
    }
  }

  "facets" should {
    "ラーメン > 東京*2, 神奈川*1" in {
      val facetRes = Restaurant.search("ラーメン").response.getFacets.facetsAsMap.get("pref_id_facet") match {
        case f: TermsFacet => f.getEntries
      }
      facetRes.get(0).getTermAsNumber.intValue must equalTo (13)
      facetRes.get(0).getCount must equalTo (2)
      facetRes.get(1).getTermAsNumber.intValue must equalTo (14)
      facetRes.get(1).getCount must equalTo (1)
    }
  }

  "suggest" should {
    "ラメーン > ラーメン" in {
       Restaurant.search("ラメーン").suggests.head must equalTo ("ラーメン")
    }
  }

  "completion" should {
    "ラー > ラーメン" in {
      Restaurant.completion("ラー") must contain ("ラーメン")
    }
  }

}
