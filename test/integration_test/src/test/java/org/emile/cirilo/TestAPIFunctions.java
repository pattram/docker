
package org.emile.cirilo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.emile.cirilo.exceptions.FedoraException;
import org.emile.cirilo.fedora.FedoraConnector;
import org.emile.cirilo.utils.Claim;
import org.emile.cirilo.utils.LegacySystem;
import org.emile.cirilo.utils.XMLUtils;
import org.emile.cirilo.utils.Unzipper;
import org.emile.cm4f.models.DatastreamListEntry;
import org.emile.cm4f.models.ObjectListEntry;
import org.emile.cm4f.models.PrototypeListEntry;
import org.emile.cm4f.models.UploadOptions;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.*;

import java.util.Vector;
import java.util.Date;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.io.*; 
import org.apache.commons.io.IOUtils.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.BitSet;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class TestAPIFunctions {
  
     private static Logger  log = Logger.getLogger("org.emile.cirilo.TestAPIFunctions");
     
     private static FedoraConnector connector = new FedoraConnector();
  
     private static String dir = "src/test/resources/org/emile/cirilo/";

     private static SAXBuilder builder;
        
     private static String hostname;
     private static String username;
     private static String group;
     private static String passwd;
     
     private static boolean superuser;
     private static Claim claim;
        
     @BeforeClass
     public static void setUp() {
        
    	 try {	 
    		 Properties props = new Properties();
         
    		 InputStream is = IntegrationTest.class.getResourceAsStream("/test.properties");
    		 if (is == null) is = IntegrationTest.class.getResourceAsStream("/default.properties");      
         
    		 props.load(is);
    		 is.close();
         
     		 hostname = props.getProperty("fcrepo.host");         
    		 username = props.getProperty("username");         
    		 group = props.getProperty("group");         
    		 passwd = props.getProperty("passwd"); 
 
       		 log.info("Running IntegrationTest against https://"+hostname+" with user "+username);
       		 claim = new Claim(hostname);
       		 
    		 builder = new SAXBuilder();
     		 
 			 while (connector.stubOpenConnection("https", hostname, username, passwd ) != 200) {
    			 Thread.sleep(3000);
    		 }	 	 
    		 
 			 superuser = new String(connector.stubGetUserInfoAsRDF()).contains("superuser");
 			 
    	 } catch (Exception e) {
    		 log.error(e);
    	 }
     }
     
     
     // @Test 
     public void TestFedoraAPIFunctions() throws ClientProtocolException, IOException, Exception { 
     }
   
     @AfterClass
     public static void tearDown() throws Exception { 
         log.info("Terminated normally");
     }  
     
     private static byte[] getStream(String file) { 	 
    	 byte[] stream = null;
    	 try {              
            FileInputStream fp = new FileInputStream(new File(dir+file));    	     
    	   	stream = IOUtils.toByteArray(fp);
    	   	fp.close();
    	 } catch (Exception e) {}  	
    	 return stream;  	
      }
      
}