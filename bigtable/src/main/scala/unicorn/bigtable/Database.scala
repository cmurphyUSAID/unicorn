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

package unicorn.bigtable

import java.util.Properties

/** A BigTable database.
  *
  * @author Haifeng Li
  */
trait Database[+T <: BigTable] extends AutoCloseable {
  /** Returns a table.
    * @param name the name of table.
    */
  def apply(name: String): T

  /** Returns the list of tables. */
  def tables: Set[String]

  /** Creates a table.
    * @param name the name of table.
    * @param families the column families in the table. A column family name
    *   must be printable -- digit or letter -- and may not contain a :.
    *   In analogy with relational databases, a column family is as a "table".
    *   In some NoSQL solutions (e.g. HBase), column families are static
    *   and should be created when creating the table.
    */
  def createTable(name: String, families: String*): T = {
    createTable(name, new Properties(), families: _*)
  }

  /** Creates a table.
    * @param name the name of table.
    * @param props table configurations.
    * @param families the column families in the table. A column family name
    *   must be printable -- digit or letter -- and may not contain a :.
    *   In analogy with relational databases, a column family is as a "table".
    *   In some NoSQL solutions (e.g. HBase), column families are static
    *   and should be created when creating the table.
    */
  def createTable(name: String, props: Properties, families: String*): T

  /** Truncates a table
    * @param name the name of table.
    */
  def truncateTable(name: String): Unit

  /** Drops a table.
    * @param name the name of table.
    */
  def dropTable(name: String): Unit

  /** Tests if a table exists.
    * @param name the name of table.
    */
  def tableExists(name: String): Boolean

  /** Major compacts a table. Asynchronous operation.
    * @param name the name of table.
    */
  def compactTable(name: String): Unit
}
