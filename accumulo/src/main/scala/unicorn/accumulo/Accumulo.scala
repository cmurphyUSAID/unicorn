/******************************************************************************
 *                   Confidential Proprietary                                 *
 *          (c) Copyright ADP 2014, All Rights Reserved                       *
 ******************************************************************************/

package unicorn.accumulo

import java.util.Properties
import scala.collection.JavaConversions._
import org.apache.hadoop.io.Text
import org.apache.accumulo.core.client.{Connector, ZooKeeperInstance}
import org.apache.accumulo.core.client.admin.NewTableConfiguration
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import unicorn.bigtable.BigTable

/**
 * Accumulo server adapter.
 * 
 * @author Haifeng Li
 */
class Accumulo(val connector: Connector) extends unicorn.bigtable.Database {
  val tableOperations = connector.tableOperations
  override def close: Unit = () // Connector has no close method

  override def apply(name: String): BigTable = {
    new AccumuloTable(this, name)
  }

  override def createTable(name: String, props: Properties, families: String*): BigTable = {
    if (connector.tableOperations.exists(name))
      throw new IllegalStateException(s"Creates Table $name, which already exists")

    val config = new NewTableConfiguration
    val settings = props.stringPropertyNames.map { p => (p, props.getProperty(p)) }.toMap
    config.setProperties(settings)
    connector.tableOperations.create(name, config)

    val localityGroups = families.map { family =>
      val set = new java.util.TreeSet[Text]()
      set.add(new Text(family))
      (family, set)
    }.toMap

    tableOperations.setLocalityGroups(name, localityGroups)
    apply(name)
  }
  
  override def dropTable(name: String): Unit = {
    if (!connector.tableOperations.exists(name))
      throw new IllegalStateException(s"Drop Table $name, which does not exists")

    tableOperations.delete(name)
  }

  override def truncateTable(name: String): Unit = {
    tableOperations.deleteRows(name, null, null)
  }

  override def compactTable(name: String): Unit = {
    tableOperations.compact(name, null, null, true, false)
  }
}

object Accumulo {
  def apply(instance: String, zookeeper: String, user: String, password: String): Accumulo = {
    val inst = new ZooKeeperInstance(instance, zookeeper)
    val conn = inst.getConnector(user, new PasswordToken(password))
    new Accumulo(conn)
  }
}
