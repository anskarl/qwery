package com.github.ldaniels528.qwery

import com.github.ldaniels528.qwery.ops.{Row, Scope}
import org.scalatest.FunSpec

import scala.util.Properties

/**
  * Qwery Integration Test Suite
  * @author lawrence.daniels@gmail.com
  */
class QweryTest extends FunSpec {
  private val scope = Scope.root()

  describe("Qwery") {

    it("should support creating named aliases") {
      val query = QweryCompiler("SELECT 1234 AS number")
      val results = query.execute(scope).toSeq
      assert(results == List(Row("number" -> 1234.0)))
    }

    it("should CAST values from one type to another") {
      val query = QweryCompiler("SELECT CAST('1234' AS Double) AS number")
      val results = query.execute(scope).toSeq
      assert(results == List(Row("number" -> 1234.0)))
    }

    it("should support CASE statement") {
      val query = QweryCompiler(
        """
          |SELECT
          |   CASE 'Hello World'
          |     WHEN 'HelloWorld' THEN '1'
          |     WHEN 'Hello' || ' ' || 'World' THEN '2'
          |     ELSE '3'
          |   END AS ItemNo
        """.stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == List(Row("ItemNo" -> "2")))
    }

    it("should support stored procedures") {
      val query = QweryCompiler(
        """
          |CREATE PROCEDURE doIt() AS
          |BEGIN
          |   declare @n int;
          |   set @n = 1;
          |   set @n = @n + 1;
          |   set @n = @n + 1;
          |   select @n
          |END;
          |
          |CALL doIt()
        """.stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == List(Row("n" -> 3)))
    }

    it("should support extracting filtered results from a file") {
      val query = QweryCompiler(
        """
          |SELECT Symbol, Name, Sector, Industry, LastSale, MarketCap
          |FROM 'companylist.csv'
          |WHERE Industry = 'Consumer Specialties'""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == Stream(
        List("Symbol" -> "BGI", "Name" -> "Birks Group Inc.", "Sector" -> "Consumer Services",
          "Industry" -> "Consumer Specialties", "LastSale" -> "1.4401", "MarketCap" -> "25865464.7281"): Row,
        List("Symbol" -> "DGSE", "Name" -> "DGSE Companies, Inc.", "Sector" -> "Consumer Services",
          "Industry" -> "Consumer Specialties", "LastSale" -> "1.64", "MarketCap" -> "44125234.84"): Row
      ))
    }

    it("should support extracting filtered results from a file for all (*) fields") {
      val query = QweryCompiler(
        """
          |SELECT * FROM 'companylist.csv'
          |WHERE Industry = 'Oil/Gas Transmission'""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results ==
        Stream(List("Symbol" -> "CQH", "Name" -> "Cheniere Energy Partners LP Holdings, LLC", "LastSale" -> "25.68",
          "MarketCap" -> "5950056000", "ADR TSO" -> "n/a", "IPOyear" -> "n/a", "Sector" -> "Public Utilities",
          "Industry" -> "Oil/Gas Transmission", "Summary Quote" -> "http://www.nasdaq.com/symbol/cqh"): Row,
          List("Symbol" -> "CQP", "Name" -> "Cheniere Energy Partners, LP", "LastSale" -> "31.75", "MarketCap" -> "10725987819",
            "ADR TSO" -> "n/a", "IPOyear" -> "n/a", "Sector" -> "Public Utilities", "Industry" -> "Oil/Gas Transmission",
            "Summary Quote" -> "http://www.nasdaq.com/symbol/cqp"): Row,
          List("Symbol" -> "LNG", "Name" -> "Cheniere Energy, Inc.", "LastSale" -> "45.35", "MarketCap" -> "10786934946.1",
            "ADR TSO" -> "n/a", "IPOyear" -> "n/a", "Sector" -> "Public Utilities", "Industry" -> "Oil/Gas Transmission",
            "Summary Quote" -> "http://www.nasdaq.com/symbol/lng"): Row,
          List("Symbol" -> "EGAS", "Name" -> "Gas Natural Inc.", "LastSale" -> "12.5", "MarketCap" -> "131496600", "ADR TSO" -> "n/a",
            "IPOyear" -> "n/a", "Sector" -> "Public Utilities", "Industry" -> "Oil/Gas Transmission",
            "Summary Quote" -> "http://www.nasdaq.com/symbol/egas"): Row))
    }

    it("should support limiting results via 'TOP n'") {
      val query = QweryCompiler(
        """
          |SELECT TOP 1 Symbol, Name, Sector, Industry, LastSale, MarketCap
          |FROM 'companylist.csv'
          |WHERE Industry = 'Consumer Specialties'""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == Stream(
        List("Symbol" -> "BGI", "Name" -> "Birks Group Inc.", "Sector" -> "Consumer Services",
          "Industry" -> "Consumer Specialties", "LastSale" -> "1.4401", "MarketCap" -> "25865464.7281"): Row
      ))
    }

    it("should support limiting results via 'LIMIT n'") {
      val query = QweryCompiler(
        """
          |SELECT Symbol, Name, Sector, Industry, LastSale, MarketCap
          |FROM 'companylist.csv'
          |WHERE Industry = 'Consumer Specialties'
          |LIMIT 1""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == Stream(
        List("Symbol" -> "BGI", "Name" -> "Birks Group Inc.", "Sector" -> "Consumer Services",
          "Industry" -> "Consumer Specialties", "LastSale" -> "1.4401", "MarketCap" -> "25865464.7281"): Row
      ))
    }

    it("should support simple data aggregation") {
      val query = QweryCompiler(
        """
          |SELECT Sector, COUNT(*) AS Securities
          |FROM 'companylist.csv'
          |GROUP BY Sector""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == List(
        List("Sector" -> "Consumer Durables", "Securities" -> 4L): Row,
        List("Sector" -> "Consumer Non-Durables", "Securities" -> 13L): Row,
        List("Sector" -> "Energy", "Securities" -> 30L): Row,
        List("Sector" -> "Consumer Services", "Securities" -> 27L): Row,
        List("Sector" -> "Transportation", "Securities" -> 1L): Row,
        List("Sector" -> "n/a", "Securities" -> 120L): Row,
        List("Sector" -> "Health Care", "Securities" -> 48L): Row,
        List("Sector" -> "Basic Industries", "Securities" -> 44L): Row,
        List("Sector" -> "Public Utilities", "Securities" -> 11L): Row,
        List("Sector" -> "Capital Goods", "Securities" -> 24L): Row,
        List("Sector" -> "Finance", "Securities" -> 12L): Row,
        List("Sector" -> "Technology", "Securities" -> 20L): Row,
        List("Sector" -> "Miscellaneous", "Securities" -> 5L): Row
      ))
    }

    it("should support aggregation functions like AVG, MIN, MAX and SUM") {
      val query = QweryCompiler(
        """
          |SELECT MIN(LastSale) AS min, MAX(LastSale) AS max, AVG(LastSale) AS avg, SUM(LastSale) AS total, COUNT(*) AS records
          |FROM 'companylist.csv'""".stripMargin('|'))
      val results = query.execute(scope).toSeq
      assert(results == List(
        List("min" -> 0.1213, "max" -> 4234.01, "avg" -> 23.68907242339833, "total" -> 8504.377, "records" -> 359): Row
      ))
    }

    it("should support aggregation functions like AVG, MIN, MAX and SUM via GROUP BY") {
      val query = QweryCompiler(
        """
          |SELECT MIN(LastSale) AS min, MAX(LastSale) AS max, AVG(LastSale) AS avg, SUM(LastSale) AS total, COUNT(*) AS records
          |FROM 'companylist.csv'
          |GROUP BY Sector""".stripMargin('|'))
      val results = query.execute(scope).toSeq
      assert(results == List(
        List(("min", 0.45), ("max", 102.5), ("avg", 26.246975000000003), ("total", 104.98790000000001), ("records", 4)): Row,
        List(("min", 0.45), ("max", 95.5), ("avg", 16.67923846153846), ("total", 216.8301), ("records", 13)): Row,
        List(("min", 0.3189), ("max", 40.62), ("avg", 7.20831), ("total", 216.2493), ("records", 30)): Row,
        List(("min", 0.4985), ("max", 67.2943), ("avg", 13.454151851851854), ("total", 363.2621000000001), ("records", 27)): Row,
        List(("min", 6.0), ("max", 6.0), ("avg", 6.0), ("total", 6.0), ("records", 1)): Row,
        List(("min", 1.26), ("max", 153.82), ("avg", 16.451099166666673), ("total", 1974.1319000000005), ("records", 120)): Row,
        List(("min", 0.1213), ("max", 74.42), ("avg", 3.5293604166666674), ("total", 169.40930000000003), ("records", 48)): Row,
        List(("min", 0.1215), ("max", 24.35), ("avg", 2.729804545454545), ("total", 120.11139999999999), ("records", 44)): Row,
        List(("min", 0.2998), ("max", 128.86), ("avg", 23.643109090909093), ("total", 260.0742), ("records", 11)): Row,
        List(("min", 0.4399), ("max", 29.0), ("avg", 9.704875), ("total", 232.91699999999997), ("records", 24)): Row,
        List(("min", 1.18), ("max", 4234.01), ("avg", 390.67642500000005), ("total", 4688.1171), ("records", 12)): Row,
        List(("min", 0.372), ("max", 41.45), ("avg", 6.22684), ("total", 124.5368), ("records", 20)): Row,
        List(("min", 0.9999), ("max", 11.65), ("avg", 5.549980000000001), ("total", 27.749900000000004), ("records", 5)): Row
      ))
    }

    it("should support describing the layout of a file") {
      val query = QweryCompiler("DESCRIBE 'companylist.csv'")
      val results = query.execute(scope).toSeq
      assert(results == Vector(
        List("Column" -> "Symbol", "Type" -> "String", "Sample" -> "XXII"): Row,
        List("Column" -> "Name", "Type" -> "String", "Sample" -> "22nd Century Group, Inc"): Row,
        List("Column" -> "LastSale", "Type" -> "String", "Sample" -> "1.4"): Row,
        List("Column" -> "MarketCap", "Type" -> "String", "Sample" -> "126977358.2"): Row,
        List("Column" -> "ADR TSO", "Type" -> "String", "Sample" -> "n/a"): Row,
        List("Column" -> "IPOyear", "Type" -> "String", "Sample" -> "n/a"): Row,
        List("Column" -> "Sector", "Type" -> "String", "Sample" -> "Consumer Non-Durables"): Row,
        List("Column" -> "Industry", "Type" -> "String", "Sample" -> "Farming/Seeds/Milling"): Row,
        List("Column" -> "Summary Quote", "Type" -> "String", "Sample" -> "http://www.nasdaq.com/symbol/xxii"): Row
      ))
    }

    it("should write direct data to a file") {
      val query = QweryCompiler(
        """
          |INSERT OVERWRITE 'test1.csv' (Symbol, Name, LastSale, MarketCap)
          |VALUES ("XXII", "22nd Century Group, Inc", 1.4, 126977358.2)
          |VALUES ("FAX", "Aberdeen Asia-Pacific Income Fund Inc", 5, 1266332595)
          |VALUES ("ACU", "Acme United Corporation.", 29, 96496195)""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == Stream(Row(Seq("ROWS_INSERTED" -> 4, "ROWS_REJECTED" -> 0))))
    }

    it("should write filtered results from one source (CSV) to another (CSV)") {
      val query = QweryCompiler(
        """
          |INSERT OVERWRITE 'test2.csv' (Symbol, Name, Sector, Industry, LastSale, MarketCap)
          |SELECT Symbol, Name, Sector, Industry, CAST(LastSale AS DOUBLE) AS LastSale, CAST(MarketCap AS DOUBLE) AS MarketCap
          |FROM 'companylist.csv'
          |WHERE Industry = 'Precious Metals'""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == Stream(Row(Seq("ROWS_INSERTED" -> 35, "ROWS_REJECTED" -> 0))))
    }

    it("should overwrite/append filtered results from one source (CSV) to another (CSV)") {
      val queries = Seq(
        QweryCompiler(
          """
            |INSERT OVERWRITE 'test3.csv' (Symbol, Name, Sector, Industry, LastSale, MarketCap)
            |SELECT Symbol, Name, Sector, Industry, LastSale, MarketCap
            |FROM 'companylist.csv'
            |WHERE Industry = 'Precious Metals'""".stripMargin),
        QweryCompiler(
          """
            |INSERT INTO 'test3.csv' (Symbol, Name, Sector, Industry, LastSale, MarketCap)
            |SELECT Symbol, Name, Sector, Industry, LastSale, MarketCap
            |FROM 'companylist.csv'
            |WHERE Industry = 'Mining & Quarrying of Nonmetallic Minerals (No Fuels)'""".stripMargin)
      )
      val results = queries.map(_.execute(scope).toSeq)
      assert(results == Seq(
        Seq(Row(Seq("ROWS_INSERTED" -> 35, "ROWS_REJECTED" -> 0))),
        Seq(Row(Seq("ROWS_INSERTED" -> 6, "ROWS_REJECTED" -> 0)))
      ))
    }

    it("should write filtered results from one source (CSV) to another (JSON)") {
      val query = QweryCompiler(
        """
          |INSERT OVERWRITE 'test4.json' (Symbol, Name, Sector, Industry, LastSale, MarketCap)
          |SELECT Symbol, Name, Sector, Industry, LastSale, MarketCap
          |FROM 'companylist.csv'
          |WHERE Sector = 'Basic Industries'""".stripMargin)
      val results = query.execute(scope).toSeq
      assert(results == Stream(Row(Seq("ROWS_INSERTED" -> 44, "ROWS_REJECTED" -> 0))))
    }

    it("should extract filtered results from a URL") {
      val connected = Properties.envOrNone("QWERY_WEB").map(_.toLowerCase()).contains("true")
      if (connected) {
        val query = QweryCompiler(
          """
            |SELECT TOP 4 * FROM 'http://www.nasdaq.com/screening/companies-by-industry.aspx?exchange=AMEX&render=download'
            |WHERE Sector = 'Oil/Gas Transmission'""".stripMargin)
        val results = query.execute(scope).toSeq
        assert(results == Stream(
          List("Symbol" -> "CQH", "Name" -> "Cheniere Energy Partners LP Holdings, LLC", "Sector" -> "Public Utilities",
            "Industry" -> "Oil/Gas Transmission", "LastSale" -> "25.68", "MarketCap" -> "5950056000"): Row,
          List("Symbol" -> "CQP", "Name" -> "Cheniere Energy Partners, LP", "Sector" -> "Public Utilities",
            "Industry" -> "Oil/Gas Transmission", "LastSale" -> "31.75", "MarketCap" -> "10725987819"): Row,
          List("Symbol" -> "LNG", "Name" -> "Cheniere Energy, Inc.", "Sector" -> "Public Utilities",
            "Industry" -> "Oil/Gas Transmission", "LastSale" -> "45.35", "MarketCap" -> "10786934946.1"): Row,
          List("Symbol" -> "EGAS", "Name" -> "Gas Natural Inc.", "Sector" -> "Public Utilities",
            "Industry" -> "Oil/Gas Transmission", "LastSale" -> "12.5", "MarketCap" -> "131496600"): Row
        ))
      }
    }

  }

}
