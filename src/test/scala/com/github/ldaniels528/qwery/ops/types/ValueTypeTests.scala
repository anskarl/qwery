package com.github.ldaniels528.qwery.ops
package types

import com.github.ldaniels528.qwery.ops.Implicits._
import org.scalatest.FunSpec

/**
  * Value Type Tests
  * @author lawrence.daniels@gmail.com
  */
class ValueTypeTests extends FunSpec {
  private val scope = RootScope()

  describe("BooleanValue") {
    it("should support determine when Boolean values are equivalent") {
      val value0 = BooleanValue(true)
      val value1 = BooleanValue(true)
      assert(value0.compare(value1, scope) == 0)
    }

    it("should support determine when Boolean values are not equivalent") {
      val value0 = BooleanValue(true)
      val value1 = BooleanValue(false)
      assert(value0.compare(value1, scope) != 0)
    }
  }

  describe("DateValue") {
    it("should support determine when Date values are equivalent") {
      val time = System.currentTimeMillis()
      val value0 = DateValue(time)
      val value1 = DateValue(time)
      assert(value0.compare(value1, scope) == 0)
    }

    it("should support determine when Date values are not equivalent") {
      val time = System.currentTimeMillis()
      val value0 = DateValue(time)
      val value1 = DateValue(time + 1000)
      assert(value0.compare(value1, scope) < 0)
      assert(value1.compare(value0, scope) > 0)
    }
  }

  describe("Expression") {
    it("should support comparisons") {
      val value0: Expression = (1: NumericValue) + 2
      val value1: Expression = (8: NumericValue) / 2
      assert(value0.compare(value1, scope) < 0)
      assert(value1.compare(value0, scope) > 0)
    }
  }

  describe("NumericValue") {
    it("should support determine when Numeric values are equivalent") {
      val value0 = NumericValue(100)
      val value1 = NumericValue(100)
      assert(value0.compare(value1, scope) == 0)
    }

    it("should support determine when Numeric values are not equivalent") {
      val value0 = NumericValue(100)
      val value1 = NumericValue(1000)
      assert(value0.compare(value1, scope) < 0)
      assert(value1.compare(value0, scope) > 0)
    }
  }

  describe("StringValue") {
    it("should support determine when String values are equivalent") {
      val string = "Hello World"
      val value0 = StringValue(string)
      val value1 = StringValue(string)
      assert(value0.compare(value1, scope) == 0)
    }

    it("should support determine when String values are not equivalent") {
      val value0 = StringValue("Hello")
      val value1 = StringValue("World")
      assert(value0.compare(value1, scope) < 0)
      assert(value1.compare(value0, scope) > 0)
    }
  }

}
