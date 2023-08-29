package org.emile.cirilo;

import org.apache.http.client.ClientProtocolException;
import org.apache.log4j.Logger;
import org.emile.cirilo.utils.Claim;
import org.jdom.input.SAXBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.*;
import org.junit.Test;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class MigrationTest {
  
     private static Logger  log = Logger.getLogger("org.emile.cirilo.MigrationTest");
            
     private final static String FAILED = " failed";
         
     private static String hostname;
     private static String rootdir;
     private static SAXBuilder builder;
     private static Claim claim;
 
     private static ArrayList<File> tests = new ArrayList<File>();;
    
     
     @BeforeClass
     public static void setUp() throws Exception {
                  
         Properties props = new Properties();
         builder = new SAXBuilder();
         
         InputStream is = MigrationTest.class.getResourceAsStream("/test.properties");
         if (is == null) is = MigrationTest.class.getResourceAsStream("/default.properties");      
         
         props.load(is);
 
         hostname = props.getProperty("hostname");         
         rootdir = props.getProperty("rootdir");         
         claim = new Claim(hostname);

     }    
 
     @Test
     public void TestScenarios() throws ClientProtocolException, IOException, Exception {
    	     	 
   	     dirTree(new File(getClass().getClassLoader().getResource(rootdir).getFile()));   	     
   	     for (File f: tests) TestScenario(f);
   	     
      }
            
     @AfterClass
     public static void tearDown() throws Exception { 
         log.info("Terminated normally");
     }  
      

     @SuppressWarnings("unchecked")
     private static void TestScenario(File file) throws org.jdom.JDOMException, IOException, Exception {
   	 
    	 try {
    		 
    		 int failed = 0, successful = 0;
    		 
        	 org.jdom.Document xml = builder.build(file);

        	 log.info("Runing test scenario of project "+xml.getRootElement().getAttributeValue("name")+" on "+hostname);

        	 org.jdom.xpath.XPath xn = org.jdom.xpath.XPath.newInstance("//namespace");
        	 for (Iterator<org.jdom.Element> it = xn.selectNodes(xml).iterator(); it.hasNext();) {
        		 org.jdom.Element el = (org.jdom.Element) it.next();
        		 claim.addNamespace(el.getChildText("prefix"), el.getChildText("uri"));
        	 }

        	 org.jdom.xpath.XPath xo = org.jdom.xpath.XPath.newInstance("//test");
        	 
			 for (Iterator<org.jdom.Element> it = xo.selectNodes(xml).iterator(); it.hasNext();) {

				org.jdom.Element el = (org.jdom.Element) it.next();
				
				String url = el.getChildText("url");
				if (!url.startsWith("http")) url = ("https://HOSTNAME" + el.getChildText("url")).replaceAll("HOSTNAME", hostname);
				
				String m = new String();

				if (el.getChild("xpath") != null) {
					m = "AssertXML: " + el.getChild("xpath").getText() + " against " + url;
					try {
						claim.assertXML(url, el.getChild("xpath").getText());
						successful++;
						continue;
					} catch (AssertionError e) {
				    } catch (Exception e) {
					}
				} else if (el.getChild("status") != null) {
					m = "AssertStatus " + url;
					try {
						claim.assertStatus(url);
						successful++;
						continue;
					} catch (AssertionError e) {
				    } catch (Exception e) {
					}
				} else if (el.getChild("matcher") != null) {
					m = "AssertHTML " + el.getChild("matcher").getText() + " against " + url;
					try {
						claim.assertHTML(url, el.getChild("matcher").getText());
						successful++;
						continue;
					} catch (AssertionError e) {
				    } catch (Exception e) {
					}
				} else if (el.getChild("json") != null) {
					m = "AssertJSON " + el.getChild("json").getText() + " against " + url;
					try {
						if (el.getChild("json").getText().contains("[")) 
							claim.assertJSONArray(url, el.getChild("json").getText());
						else
							claim.assertJSON(url, el.getChild("json").getText());
						successful++;
						continue;
					} catch (AssertionError e) {
				    } catch (Exception e) {
					}
				}
				failed++;
				log.info(m + FAILED);

			}
			 
			log.info(String.format("%d/%d tests were successfully completed", successful, successful+failed));
			
		} catch (Exception q) {
			q.printStackTrace();
		} 
     	 
     }	
 
     private static void dirTree(File dir) {
         File[] entries = dir.listFiles();
         
         for (File file: entries) {
            if (file.isDirectory()) dirTree(file); else tests.add(file);	
         }
      }

     
}    
