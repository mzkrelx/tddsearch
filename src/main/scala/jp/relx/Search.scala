package jp.relx

import org.scalatra._
import scalate.ScalateSupport

class Search extends TddsearchStack {

  get("/") {
    html.helloTwirl.render(new java.util.Date)
  }

}
