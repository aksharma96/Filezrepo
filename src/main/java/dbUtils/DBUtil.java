package dbUtils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil
{
  public static Connection createConnection(String dbname) throws ClassNotFoundException
  {
    // load the sqlite-JDBC driver using the current class loader
    Class.forName("org.sqlite.JDBC");

    Connection connection = null;
    try
    {
      // create a database connection
      connection = DriverManager.getConnection("jdbc:sqlite:"+dbname+".db");
      
      return connection;

    }
    catch(SQLException e)
    {
      // if the error message is "out of memory", 
      // it probably means no database file is found
      System.err.println(e.getMessage());
    }
	return connection;
  }
  
  
  public static Connection createConnectionFileDB(String type) throws ClassNotFoundException
  {
    Class.forName("org.sqlite.JDBC");

    Connection connection = null;
    try
    {
      connection = DriverManager.getConnection("jdbc:sqlite:filestore_"+type+".db");
     // connection.get
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      
      statement.executeUpdate("create table IF NOT EXISTS file (uid string PRIMARY KEY , name string,created datetime,location string,size long ,updated datetime)");
      return connection;
      
    }
    catch(SQLException e)
    {
      // if the error message is "out of memory", 
      // it probably means no database file is found
      System.err.println(e.getMessage());
    }
	return connection;
  }
  
  public static Connection createConnectionDB() throws ClassNotFoundException
  {
    Class.forName("org.sqlite.JDBC");

    Connection connection = null;
    try
    {
      connection = DriverManager.getConnection("jdbc:sqlite:filestore_local_db.db");
     // connection.get
      Statement statement = connection.createStatement();
      statement.setQueryTimeout(30);
      
      statement.executeUpdate("create table IF NOT EXISTS file (uid string PRIMARY KEY , name string,created datetime,location string,size long ,updated datetime,file blob)");
      return connection;
      
    }
    catch(SQLException e)
    {
      // if the error message is "out of memory", 
      // it probably means no database file is found
      System.err.println(e.getMessage());
    }
	return connection;
  }
}