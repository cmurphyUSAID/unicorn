/*******************************************************************************
 * (C) Copyright 2015 ADP, LLC.
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package unicorn.bigtable.accumulo

import org.specs2.mutable._
import org.specs2.specification.BeforeAfterAll
import unicorn.bigtable._
import unicorn.util._

/**
 * @author Haifeng Li
 */
class AccumuloSpec extends Specification with BeforeAfterAll {
  // Make sure running examples one by one.
  // Otherwise, test cases on same columns will fail due to concurrency
  sequential
  val accumulo = Accumulo()
  val tableName = "unicorn_test"
  var table: AccumuloTable = null

  override def beforeAll = {
    accumulo.createTable(tableName, "cf1", "cf2")
    table = accumulo(tableName)
  }

  override def afterAll = {
    if (table != null) table.close
    accumulo.dropTable(tableName)
  }

  "Accumulo" should {
    "get the put" in {
      table.put("row1", "cf1", "c1", "v1", 0L)
      new String(table("row1", "cf1", "c1").get, utf8) === "v1"
      table.delete("row1", "cf1", "c1")
      table("row1", "cf1", "c1".getBytes(utf8)) === None
    }

    "get the family" in {
      table.put("row1", "cf1", Column("c1", "v1"), Column("c2", "v2"))
      val columns = table.get("row1", "cf1")
      columns.size === 2
      new String(columns(0).value, utf8) === "v1"
      new String(columns(1).value, utf8) === "v2"

      table.delete("row1", "cf1")
      val empty = table.get("row1", "cf1")
      empty.size === 0
    }

    "get empty family" in {
      val columns = table.get("row1", "cf1")
      columns.size === 0
    }

    "get nonexistent family" in {
      val columns = table.get("row1", "cf5")
      columns.size === 0
    }

    "get the row" in {
      table.put("row1", Seq(
        ColumnFamily("cf1", Seq(Column("c1", "v1"), Column("c2", "v2"))),
        ColumnFamily("cf2", Seq(Column("c3", "v3"))))
      )
      val families = table.get("row1")
      families.size === 2
      families(0).columns.size === 2
      families(1).columns.size === 1
      families(0).family === "cf1"
      families(1).family === "cf2"

      new String(families(0).columns(0).value, utf8) === "v1"
      new String(families(0).columns(1).value, utf8) === "v2"
      new String(families(1).columns(0).value, utf8) === "v3"

      table.delete("row1", "cf1")
      val cf1 = table.get("row1", "cf1")
      println(cf1)
      cf1.size === 0

      table.get("row1").size === 1
      val cf2 = table.get("row1", "cf2")
      cf2.size === 1

      table.delete("row1")
      table.get("row1").size === 0
    }

    "get nonexistent row" in {
      val families = table.get("row5")
      families.size === 0
    }

    "get multiple rows" in {
      val row1 = Row("row1",
        Seq(ColumnFamily("cf1", Seq(Column("c1", "v1"), Column("c2", "v2"))),
          ColumnFamily("cf2", Seq(Column("c3", "v3")))))

      val row2 = Row("row2".getBytes(utf8),
        Seq(ColumnFamily("cf1", Seq(Column("c1", "v1"), Column("c2", "v2")))))

      table.putBatch(row1, row2)

      val keys = Seq("row1", "row2")
      val rows = table.getBatch(keys)
      rows.size === 2
      rows(0).families.size === 2
      rows(1).families.size === 1

      table.deleteBatch(keys)
      table.getBatch(keys).size === 0
    }

    "scan" in {
      val row1 = Row("row1",
        Seq(ColumnFamily("cf1", Seq(Column("c1", "v1"), Column("c2", "v2"))),
          ColumnFamily("cf2", Seq(Column("c3", "v3")))))

      val row2 = Row("row2".getBytes(utf8),
        Seq(ColumnFamily("cf1", Seq(Column("c1", "v1"), Column("c2", "v2")))))

      val row3 = Row("row3".getBytes(utf8),
        Seq(ColumnFamily("cf1", Seq(Column("c1", "v1"), Column("c2", "v2")))))

      table.putBatch(row1, row2, row3)

      val scanner = table.scan("row1", "row3")
      val r1 = scanner.next
      new String(r1.row, utf8) === "row1"
      r1.families(0).family === "cf1"
      r1.families(1).family === "cf2"
      val r2 = scanner.next
      new String(r2.row, utf8) === "row2"
      scanner.hasNext === false
      scanner.close

      val keys = Seq("row1", "row2", "row3")
      table.deleteBatch(keys)
      table.getBatch(keys).size === 0
    }
  }
}
