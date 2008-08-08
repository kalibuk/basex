package org.basex.test;

import org.xmldb.api.base.*;
import org.xmldb.api.modules.*;
import org.xmldb.api.*;
import org.w3c.dom.Document;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.ContentHandler;
import org.basex.data.SAXSerializer;

/**
 * Test for the XMLDB:API
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Andreas Weiler
 */
public class XMLDBXPathTest {
  /** XMLDB driver. */
  static String driver = "org.basex.api.xmldb.BXDatabaseImpl";
  /** Database/document path. */
  static String url = "xmldb:basex://localhost:8080/input";
  /** Query. */
  static String query = "//li";

  /**
   * Main Method.
   * @param args command line arguments (ignored).
   * @exception Exception Exception.
   */
  public static void main(String[] args) throws Exception {
    Collection col = null;
    try {
      Class<?> c = Class.forName(driver);
      Database database = (Database) c.newInstance();
      DatabaseManager.registerDatabase(database);
      col = DatabaseManager.getCollection(url);

      XPathQueryService service = (XPathQueryService) col.getService(
          "XPathQueryService", "1.0");
      ResourceSet resultSet = service.query(query);
      
      ResourceIterator results = resultSet.getIterator();
                     
      while(results.hasMoreResources()) {
        Resource res = results.nextResource();
        System.out.println(res.getContent());
      }
      
      String id = "input";
      XMLResource resource = 
        (XMLResource) col.getResource(id);
      
      /* Test XML Document Retrieval */
      String cont = (String) resource.getContent();
      System.out.println("------XML Document Retrieval------");
      System.out.println(cont);
      System.out.println("------XML Document Retrieval END------");
      
      /* DOM Document Retrieval*/
      Document doc = (Document) resource.getContentAsDOM();
      System.out.println("------DOC Document Retrieval------");
      TransformerFactory.newInstance().newTransformer().transform(
                      new DOMSource(doc), new StreamResult(System.out));
      System.out.println("------DOC Document Retrieval END------");
      
      /* SAX Document Retrieval*/
      SAXSerializer sax = new SAXSerializer(null);
      // A custom SAX Content Handler is required to handle the SAX events
      ContentHandler handler = sax.getContentHandler();
      resource.getContentAsSAX(handler);
      
      /* Binary Content Retrieval*/
      /*BinaryResource resource2 = 
         (BinaryResource) col.getResource(id);
      // Return value of getContent must be defined in the specific language mapping
      // for the language used. For Java this is a byte array.
      byte[] img = (byte[]) resource2.getContent();
      System.out.println("------Binary Content Retrieval------");
      System.out.println(new String(img));
      System.out.println("------Binary Content Retrieval END------");*/
                     

    } catch(XMLDBException e) {
      System.err.println("XML:DB Exception occured " + e.errorCode);
      e.printStackTrace();
    } finally {
      if(col != null) {
        col.close();
      }
    }
  }
}
