/*
 * ZIM: Center for Information Modelling, University Graz
 * 
 * Licensed to ZIM under one or more contributor license agreements.
 * See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership.
 *
 * ZIM licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applcubeicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.emile.cirilo;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.log4j.Logger;
import org.emile.cirilo.exceptions.ForbiddenException;
import org.emile.cirilo.exceptions.KeycloakServerNotFoundException;
import org.emile.cirilo.fedora.FedoraConnector;
import org.emile.cm4f.models.DatastreamListEntry;
import org.emile.cm4f.models.LogList;
import org.emile.cm4f.models.ObjectListEntry;
import org.emile.cm4f.models.PrototypeListEntry;
import org.emile.cm4f.models.UploadOptions;
import org.emile.cirilo.utils.Claim;
import org.emile.cirilo.utils.LegacySystem;
import org.emile.cirilo.utils.SSLConnectionFactory;
import org.emile.cirilo.utils.XMLUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)

public class IntegrationTest {
  
     private static Logger  log = Logger.getLogger("org.emile.cirilo.IntegrationTest");
     
     private final static String UPDATE        = "update"; 
     private final static String DELETE        = "delete"; 
     private final static String WELLFORMED    = "WELLFORMED"; 
     
     private final static int MAXREPEATS       = 250;
      
     private static FedoraConnector connector = new FedoraConnector();
     
     private static String dir = "src/test/resources/org/emile/cirilo/";

     private static String hostname;
     private static String username;
     private static String group;
     private static String passwd;
     
     private static boolean superuser;  
     private static Claim claim;           
     private static String uuid;
     
     private static boolean nokeycloakhost;
     
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
    		 
    		 nokeycloakhost = false;

    		 checkServices();       
    		 
    		 while (connector.stubOpenConnection("https", hostname, username, passwd ) != 200) {
    			 Thread.sleep(3000);
    		 }	 
    		 
			 superuser = new String(connector.stubGetUserInfoAsRDF()).contains("superuser");
 
			 createTestObjects();
		  
       	 } catch (KeycloakServerNotFoundException e) {
         	  log.info( "Warning: No configured authorization server was found for <"+hostname+">. Therefore, the integration tests cannot be run.");
         	  notreadyError();
       	 } catch (Exception e) {
       		  notreadyError();
       	 }
   
     }
     
     private static void checkServices() {
    	 check("Framework",             "https://HOSTNAME/cm4f/health", "UP");    	
     }
     	
     private static void createTestObjects() {       
       
       try {
                   
         log.info("Creating test objects");
         
         connector.stubTxBegin();
         connector.stubCreateObject("o:emile.tei",        group,     getStream("objects/tei/tei.xml"));  
         connector.stubCreateObject("o:emile.mei",        group,     getStream("objects/mei/mei.xml"));  
         connector.stubCreateObject("o:emile.pdf",        group,     getStream("objects/pdf/pdf.xml"));  
         connector.stubCreateObject("o:emile.resource",   group,     getStream("objects/resource/resource.xml"));  
         connector.stubCreateObject("o:emile.story",      group,     getStream("objects/story/story.xml"));  
         connector.stubCreateObject("o:emile.skos",       group,     getStream("objects/skos/skos.xml"));  
         connector.stubCreateObject("o:emile.ontology",   group,     getStream("objects/ontology/ontology.xml"));  
         connector.stubCreateObject("o:emile.latex",      group,     getStream("objects/latex/latex.xml"));  
         connector.stubCreateObject("o:emile.bibtex",     group,     getStream("objects/bibtex/bibtex.xml"));  
         connector.stubCreateObject("o:emile.html",       group,     getStream("objects/html/html.xml"));  
         connector.stubCreateObject("o:emile.lido",       group,     getStream("objects/lido/lido.xml"));  
         connector.stubCreateObject("query:emile",        group,     getStream("objects/query/query.xml"));  
         connector.stubCreateObject("query:construct",    group,     getStream("objects/construct/query.xml"));  
         connector.stubCreateObject("query:geosparql",    group,     getStream("objects/geosparql/query.xml"));  
         connector.stubCreateObject("context:emile",      group,     getStream("objects/context/context.xml"));  
         connector.stubCreateObject("corpus:emile",       group,     getStream("objects/corpus/corpus.xml"));  
         connector.stubCreateObject("o:emile.r",          group,     getStream("objects/r/r.xml"));  
         connector.stubCreateObject("o:emile.gml",        group,     getStream("objects/gml/gml.xml"));  
         connector.stubCreateObject("o:emile.odt",        group,     getStream("objects/odt/tei.xml"));  
         connector.stubCreateObject("o:emile.rdf",        group,     getStream("objects/rdf/rdf.xml"));  
         connector.stubCreateObject("o:emile.rdo",        group,     getStream("objects/rdo/rdo.xml"));  
         connector.stubTxCommit();
            
         connector.stubTxBegin();
         connector.stubAddDatastream("o:emile.tei",       "TEI_SOURCE", getStream("objects/tei/tei_source.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.tei",       "THUMBNAIL", getStream("objects/tei/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.tei",       "DC", getStream("objects/tei/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.tei",       "METS_SOURCE", getStream("objects/tei/mets_source.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.tei",       "FOL_1R", getStream("objects/tei/fol_1r.jpg"), "image/jpeg", WELLFORMED);
         connector.stubAddDatastream("o:emile.tei",       "FOL_1V", getStream("objects/tei/fol_1v.jpg"), "image/jpeg", WELLFORMED);
         connector.stubAddDatastream("o:emile.tei",       "BIBTEX", getStream("objects/tei/bibtex.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.tei",       "RDF_TRIPLES", getStream("objects/tei/rdf_triples.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.mei",       "MEI_SOURCE", getStream("objects/mei/mei_source.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.mei",       "THUMBNAIL", getStream("objects/mei/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.mei",       "DC", getStream("objects/mei/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.mei",       "RDF_TRIPLES", getStream("objects/mei/rdf_triples.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.pdf",       "PDF_STREAM", getStream("objects/pdf/pdf_stream.pdf"), "application/pdf", WELLFORMED);
         connector.stubAddDatastream("o:emile.pdf",       "THUMBNAIL", getStream("objects/pdf/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.pdf",       "DC", getStream("objects/pdf/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.pdf",       "BIBTEX", getStream("objects/tei/bibtex.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.story",     "STORY", getStream("objects/story/storymap.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.story",     "THUMBNAIL", getStream("objects/story/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.story",     "DC", getStream("objects/story/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.skos",      "ONTOLOGY", getStream("objects/skos/ontology.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.skos",      "THUMBNAIL", getStream("objects/skos/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.skos",      "DC", getStream("objects/skos/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.ontology",  "ONTOLOGY", getStream("objects/skos/ontology.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.ontology",  "THUMBNAIL", getStream("objects/ontology/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.ontology",  "DC", getStream("objects/ontology/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.bibtex",    "BIBTEX", getStream("objects/bibtex/bibtex_source.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.bibtex",    "THUMBNAIL", getStream("objects/bibtex/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.bibtex",    "DC", getStream("objects/bibtex/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.latex",     "LATEX_SOURCE", getStream("objects/latex/latex.txt"), "text/plain", WELLFORMED);
         connector.stubAddDatastream("o:emile.latex",     "THUMBNAIL", getStream("objects/latex/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.latex",     "DC", getStream("objects/latex/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.latex",     "BIBTEX", getStream("objects/tei/bibtex.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.html",      "HTML_STREAM", getStream("objects/html/html_stream.xml"), "text/xml", null);                      
         connector.stubAddDatastream("o:emile.html",      "THUMBNAIL", getStream("objects/html/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.html",      "DC", getStream("objects/html/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("o:emile.lido",      "LIDO_SOURCE", getStream("objects/lido/lido_source.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("o:emile.lido",      "THUMBNAIL", getStream("objects/lido/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.lido",      "DC", getStream("objects/lido/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("o:emile.lido",      "RDF_TRIPLES", getStream("objects/lido/rdf_triples.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.resource",  "DC", getStream("objects/resource/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("o:emile.resource",  "THUMBNAIL", getStream("objects/resource/thumbnail.png"), "image/png", null);
         connector.stubTxCommit();

         connector.stubTxBegin();
         connector.stubAddDatastream("query:emile",       "QUERY", getStream("objects/query/query.sparql"), "text/plain", null);                      
         connector.stubAddDatastream("query:emile",       "DC", getStream("objects/query/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("query:construct",   "QUERY", getStream("objects/construct/query.sparql"), "text/plain", null);                      
         connector.stubAddDatastream("query:construct",   "DC", getStream("objects/construct/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("query:geosparql",   "QUERY", getStream("objects/geosparql/query.sparql"), "text/plain", null);                      
         connector.stubAddDatastream("query:geosparql",   "DC", getStream("objects/geosparql/dc.xml"), "text/xml", WELLFORMED);      
         connector.stubAddDatastream("context:emile",     "QUERY", getStream("objects/context/query.sparql"), "text/plain", null);     
         connector.stubAddDatastream("context:emile",     "THUMBNAIL", getStream("objects/context/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("context:emile",     "DC", getStream("objects/context/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("context:emile",     "KML", getStream("objects/context/kml.xml"), "text/xml", WELLFORMED);     
         connector.stubAddDatastream("context:emile",     "CMIF", getStream("objects/context/cmif.xml"), "text/xml", WELLFORMED);     
         connector.stubAddDatastream("context:emile",     "PELAGIOS", getStream("objects/context/pelagios.xml"), "text/xml", WELLFORMED);     
         connector.stubAddDatastream("corpus:emile",      "QUERY", getStream("objects/corpus/query.sparql"), "text/plain", WELLFORMED);     
         connector.stubAddDatastream("corpus:emile",      "THUMBNAIL", getStream("objects/corpus/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("corpus:emile",      "DC", getStream("objects/corpus/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("corpus:emile",      "CMDI", getStream("objects/corpus/cmdi.xml"), "text/xml", WELLFORMED);     
         connector.stubAddDatastream("corpus:emile",      "DSPIN", getStream("objects/corpus/dspin.xml"), "text/xml", WELLFORMED);     
         connector.stubAddDatastream("corpus:emile",      "TCF", getStream("objects/corpus/tcf.xml"), "text/xml", WELLFORMED);     
         connector.stubTxCommit();

         connector.stubTxBegin();
         connector.stubAddDatastream("o:emile.r",         "DC", getStream("objects/r/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.r",         "THUMBNAIL", getStream("objects/r/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.r",         "DATASETS", getStream("objects/r/datasets.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.r",         "RSCRIPT", getStream("objects/r/rscript.txt"), "text/plain", null);
         connector.stubAddDatastream("o:emile.r",         "PDF_STREAM", getStream("objects/r/pdf_stream.pdf"), "application/pdf", WELLFORMED);
         connector.stubAddDatastream("o:emile.r",         "DATAFRAME", getStream("objects/r/data.csv"), "text/plain", WELLFORMED);
         connector.stubAddDatastream("o:emile.gml",       "GML_SOURCE", getStream("objects/gml/gml_source.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("o:emile.gml",       "DC", getStream("objects/gml/dc.xml"), "text/xml", WELLFORMED);                      
         connector.stubAddDatastream("o:emile.gml",       "THUMBNAIL", getStream("objects/gml/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.gml",       "GeoRDF_TRIPLES", getStream("objects/gml/geordf_triples.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.odt",       "TEI_SOURCE", getStream("objects/odt/tei_source.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.odt",       "ODT", getStream("objects/odt/tei.odt"), "application/vnd.oasis.opendocument.text", null);
         connector.stubAddDatastream("o:emile.odt",       "DOCX", getStream("objects/odt/tei.docx"), "application/vnd.openxmlformats-officedocument.wordprocessingml.document", null);
         connector.stubAddDatastream("o:emile.odt",       "DC", getStream("objects/odt/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.odt",       "THUMBNAIL", getStream("objects/tei/thumbnail.png"), "image/png", null);      
         connector.stubAddDatastream("o:emile.rdf",       "ONTOLOGY", getStream("objects/rdf/ontology.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.rdf",       "THUMBNAIL", getStream("objects/rdf/thumbnail.png"), "image/png", null);
         connector.stubAddDatastream("o:emile.rdf",       "DC", getStream("objects/rdf/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.rdo",       "DC", getStream("objects/rdo/dc.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.rdo",       "DESCRIPTION", getStream("objects/rdo/description.xml"), "text/xml", WELLFORMED);
         connector.stubAddDatastream("o:emile.rdo",       "THUMBNAIL", getStream("objects/rdo/thumbnail.png"), "image/png", null);
         connector.stubTxCommit();
         
       } catch (Exception e) {e.printStackTrace();}           
     }

    
     @Test 
     public void Test001CreatePrototypes() throws ClientProtocolException, IOException, Exception {
    	 
    	 log.info("Testing existance of content model prototypes");

         claim.assertStatus("https://HOSTNAME/o:prototype.context/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.corpus/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.cube/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.envelope/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.gml/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.html/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.latex/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.lido/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.mei/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.mets/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.oairecord/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.ontology/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.pdf/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.r/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.rdo/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.resource/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.rti/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.skos/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.spectral/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.story/DC");
         claim.assertStatus("https://HOSTNAME/o:prototype.tei/DC");
              
     }
          
     @Test 
     public void Test002UserManagement() throws ClientProtocolException, ForbiddenException, Exception {
  			   	     	  
    	 if (superuser) {
    		  
    	    log.info("Testing Keycloak environment");

    		connector.stubOpenConnection("https", hostname, "obiwan", "passwd");  
    		//group: padawan, role: projectadmin
    		claim.assertStatus(HttpStatus.SC_NOT_FOUND, connector.stubGetAuthorization("o:emile", UPDATE)); 
    	    claim.assertStatus(HttpStatus.SC_OK, connector.stubGetAuthorization("o:emile.mei", UPDATE)); 
    		claim.assertStatus(HttpStatus.SC_OK, connector.stubGetAuthorization("o:emile.mei", DELETE) ); 	
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubSetOwner("o:emile.mei", "yoda")); 
    	
    		connector.stubOpenConnection("https", hostname, "padme", "passwd");  
    		//group: padawan, role: editor
    		claim.assertStatus(HttpStatus.SC_OK, connector.stubGetAuthorization("o:emile.mei", UPDATE) ); 
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubGetAuthorization("o:emile.mei", DELETE) ); 
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubSetOwner("o:emile.mei", "yoda")); 
  			 
    		connector.stubOpenConnection("https", hostname, "r2d2", "passwd");  
    		//group: aaif, role: projectadmin
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubGetAuthorization("o:emile.mei", UPDATE) ); 
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubGetAuthorization("o:emile.mei", DELETE) ); 
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubSetOwner("o:emile.mei", "yoda")); 

    		connector.stubOpenConnection("https", hostname, "c3po", "passwd");  	
    		//group: aaif, role: editor
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubGetAuthorization("o:emile.mei", UPDATE) ); 
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubGetAuthorization("o:emile.mei", DELETE) ); 	
    		claim.assertStatus(HttpStatus.SC_FORBIDDEN, connector.stubSetOwner("o:emile.mei", "yoda")); 	 
    		  
    	  }
     }
    
     @Test 
     public void Test003ResetUserManagement() throws ClientProtocolException, ForbiddenException, Exception {
    	
  		log.info("Reset Keycloak environment");
    	 
  		claim.assertStatus(HttpStatus.SC_OK, connector.stubOpenConnection("https", hostname, username, passwd ));
   			
     }
   
     @Test 
     public void Test004ClassUploadOptions() throws ClientProtocolException, IOException, Exception {	 
 
       	UploadOptions options = new UploadOptions(); 
    	     	
       	options.setCMDIify();
       	options.setDCify();
       	options.setGEOifyByIds();
       	options.setGEOifyByPlacenames();
       	options.setMETSify();
       	options.setRDFify();
       	options.setSKOSify();
       	options.setCustomize();
    	options.setValidate();
    	  	
    	String s =  options.get();
    	
    	if (options.isValidate()) {
            log.info("Testing Class UploadOptions: "+ new UploadOptions(s).get());                	
    	}
       	
       	options.clear();
       	
      }    

     
     @Test 
     public void Test005CloneObject() throws ClientProtocolException, IOException, Exception {
    	 
         log.info("Testing API Functions: CloneObject");

         // Valid PIDs. The objects are created
         // is also valid:  connector.stubCloneObject("o:prototype.tei", "o:109", group);          	
         
         connector.stubTxBegin();
         connector.stubCloneObject("o:prototype.tei", "o:emile.ingest.tei.1", group);   
         connector.stubTxCommit();
          
         
     }

     @Test 
     public void Test006AddProperty() throws ClientProtocolException, IOException, Exception {
  
       	 log.info("Testing API Functions: AddProperty");
         
         connector.stubTxBegin();
         Element property = new Element("hasXsltToCMDI", Namespaces.xmlns_cm4f_xsl).setAttribute("resource", "http://apache:82/xsl/tei2cmdi.xsl", Namespaces.xmlns_rdf);
         claim.assertStatus(HttpStatus.SC_OK, connector.stubAddProperty("o:emile.ingest.tei.1", property));
         connector.stubTxCommit();

         
     }    
 
     @Test 
     public void Test007AddProperty() throws ClientProtocolException, IOException, Exception {
  
       	 log.info("Testing API Functions: Set optional cm4f properties for TEI objects");
         
         Element property;
         
         connector.stubTxBegin();
         property = new Element("hasXsltToCMDI", Namespaces.xmlns_cm4f_xsl).setAttribute("resource", "http://apache:82/xsl/tei2cmdi.xsl", Namespaces.xmlns_rdf);
         claim.assertStatus(HttpStatus.SC_OK, connector.stubAddProperty("o:emile.ingest.tei.1", property));
         
         property = new Element("hasXsltPipelines", Namespaces.xmlns_cm4f).setAttribute("resource", "http://apache:82/xsl/pipelines.xml", Namespaces.xmlns_rdf);
         claim.assertStatus(HttpStatus.SC_OK, connector.stubAddProperty("o:emile.odt", property));
         connector.stubTxCommit();
         
     }    
 
     @Test 
     public void Test008AddRelationships() throws ClientProtocolException, IOException, Exception {
    	 
      	 log.info("Testing API Functions: AddRelationships");
 
      	 connector.stubTxBegin();
      	 claim.assertStatus(HttpStatus.SC_OK, connector.stubAddRelationships("o:emile.skos", "context:test"));          
      	 connector.stubTxCommit();
      	 
      	 connector.stubTxBegin();
      	 claim.assertStatus(HttpStatus.SC_OK, connector.stubDelRelationships("o:emile.skos", "all"));     
     	 connector.stubTxCommit();
     	         
      	 connector.stubTxBegin();
      	 claim.assertStatus(HttpStatus.SC_OK, connector.stubAddRelationships("o:emile.skos", "context:emile;context:skos;context:sitemap"));                        	 
     	 connector.stubTxCommit();
     	 
      	 connector.stubTxBegin();
      	 claim.assertStatus(HttpStatus.SC_OK, connector.stubDelRelationships("o:emile.skos", "context:skos"));                        	    
     	 connector.stubTxCommit();
     	 
      	 connector.stubTxBegin();
      	 claim.assertStatus(HttpStatus.SC_OK, connector.stubAddRelationships("o:emile.ingest.tei.1", "context:emile")); 
         connector.stubTxCommit();

     }
     
     @Test 
     public void Test009UpdateDCMI() throws ClientProtocolException, IOException, Exception {
 	 
      	 log.info("Testing API Functions: UpdateDCMI");

      	 connector.stubTxBegin();
      	 claim.assertStatus(HttpStatus.SC_OK, connector.stubModifyDatastream("o:emile.ingest.tei.1", "DC", getStream("objects/tei/import/dc.xml"), "text/xml", null)); 
      	 connector.stubTxCommit();
      	 claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.1/DC","//*[. = 'application/tei+xml']" );      
         
     }

     @Test 
     public void Test010GetObjectList() throws ClientProtocolException, IOException, Exception {
          	 
      	 log.info("Testing API Functions: GetObjectList");

    	 String groups = username;
    	    
      	 ArrayList<ObjectListEntry> ls = connector.stubGetObjectList(group, "filter(regex(str(?pid), '^o:'))", 10, null, false);
      	 Assert.assertNotEquals(0, ls.size());

    	 log.info("# Get a list of the first 10 objects as an ObjectListEntry ArrayList");
         for (ObjectListEntry o : connector.stubGetObjectList(group, "filter(regex(str(?pid), '^o:'))", 10, null, false)) {
    		log.info(String.format("  %-35s %s", o.getPid(), o.getModel()));
    	 }       
         
         log.debug("# Get a list of the first 10 objects serialized as a SPARQL result");
         log.debug(new String(connector.stubGetObjectListAsRDF(group, "filter(regex(str(?pid), '^o:'))", 10, null, false)));
         
     }

     @Test 
     public void Test011GetContextList() throws ClientProtocolException, IOException, Exception {
          	 
      	 log.info("Testing API Functions: GetContextList");

      	 ArrayList<ObjectListEntry> ls = connector.stubGetContextList(group);
      	 Assert.assertNotEquals(0, ls.size());
      	 
         log.info("# Get a list of all context objects of group <" +group +"> as an ObjectListEntry ArrayList");
         for (ObjectListEntry o : ls){
    		log.info(String.format("  %-35s %s", o.getPid(), o.getTitle()));
    	 }       
         
         log.debug("# Get a list of all context objects for the current user serialized as a SPARQL result");
         log.debug(new String(connector.stubGetContextListAsRDF((null))));
        
     }

     @Test 
     public void Test012GetDatastreamList() throws ClientProtocolException, IOException, Exception {
 
      	 log.info("Testing API Functions: GetDatastreamList");      
          
      	 ArrayList<DatastreamListEntry> ls = connector.stubGetDatastreamList("o:emile.gml");
      	 Assert.assertNotEquals(0, ls.size());

    	 log.info("# Get a datastream list of object o:emile.tei as a DatastreamListEntry ArrayList");
         for (DatastreamListEntry ds : ls) {
    		log.info(String.format("  %-35s %s", ds.getDsid(), ds.getMimetype()));
    	 } 
         
         log.debug("# Get a datastream list of object o:emile.tei serialized as a SPARQL result");
         log.debug(new String(connector.stubGetDatastreamListAsRDF("o:emile.tei")));
         
    }  
    
    @Test 
    public void Test013GetPrototypeList() throws ClientProtocolException, IOException, Exception {
          	 
      	log.info("Testing API Functions: GetPrototypeList");
 
      	ArrayList<PrototypeListEntry> ls = connector.stubGetPrototypeList(group, true);
       	Assert.assertNotEquals(0, ls.size());

        log.info("# Get a list of all super prototype objects");
        for (PrototypeListEntry o : ls) {
        	log.info(String.format("  %-35s %s", o.getPid(), o.getTitle()));
        }
        
        log.debug("# Get a list of all prototype objects for the current user serialized as a SPARQL result");
        log.debug(new String(connector.stubGetPrototypeListAsRDF(null, false)));
 
        log.debug("# Get a list of all super prototype objects serialized as a RDF stream");
        log.debug(new String(connector.stubGetPrototypeListAsRDF(null, true)));
 
    }

     
    @Test 
    public void Test014TriggerUploadWorkflow() throws ClientProtocolException, IOException, Exception {

      	 UploadOptions options = new UploadOptions(); 
   	  
         log.info("Testing API Functions: TriggerUploadWorkflow for TEI objects with Open Office documents");
          	
         options.setValidate(); 	 

         connector.stubTxBegin();
         connector.stubCloneObject("o:prototype.tei", "o:emile.ingest.tei.2", group);          	
         connector.stubTxCommit();

         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.tei.2", getStream("objects/odt/tei.odt"), options.get(), "o:prototype.context", null));
         claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.2/TEI_SOURCE","//tei:p[contains(.,'Written by OpenOffice')]" );      

    }  
    
    @Test 
    public void Test015TriggerUploadWorkflow() throws ClientProtocolException, IOException, Exception {

     	 UploadOptions options = new UploadOptions(); 
   	  
         log.info("Testing API Functions: TriggerUploadWorkflow for TEI objects with MS Word documents");
       	
         options.setValidate();	 
         
         connector.stubTxBegin();
         connector.stubCloneObject("o:prototype.tei", "o:emile.ingest.tei.3", group);          	
         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.tei.3", getStream("objects/odt/tei.docx"), options.get(), "o:prototype.context", null));
         connector.stubTxCommit();

         claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.3/TEI_SOURCE","//tei:p[contains(.,'Converted from a Word document')]" );     

    }  
    
    @Test 
    public void Test016TriggerUploadWorkflow() throws ClientProtocolException, IOException, Exception {

     	 UploadOptions options = new UploadOptions(); 
   	  
         log.info("Testing API Functions: TriggerUploadWorkflow for TEI objects with source documents");
          	
         options.setValidate(); 	 
         options.setSKOSify();
         options.setGEOifyByIds();
         options.setCMDIify();
         
         connector.stubTxBegin();
         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.tei.1", getStream("objects/tei/ingest/tei_source.xml"), options.get(), "o:prototype.context",  new File(dir + "objects/tei/ingest").getAbsolutePath()));
         connector.stubTxCommit();
 
         claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.1/TEI_SOURCE","//tei:place[@xml:id='GID.1']" ); 
         claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.1/TEI_SOURCE","//tei:p[contains(@ana,'pth.2')]" ); 
 	     claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.1/CMDI","/*" );
       

    }  
    
    @Test 
    public void Test017TriggerUploadWorkflow() throws ClientProtocolException, IOException, Exception {

    	 UploadOptions options = new UploadOptions(); 
     	  
         log.info("Testing API Functions: TriggerUploadWorkflow for LIDO objects");
          	
         options.setValidate(); 	 
         options.setDCify();
         options.setSKOSify();
         options.setGEOifyByIds();
        
         connector.stubTxBegin();
         connector.stubCloneObject("o:prototype.lido", "o:emile.ingest.lido.1", group);          	
         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.lido.1", getStream("objects/lido/ingest/lido_source.xml"), options.get(), "o:prototype.context",  new File(dir + "objects/lido/ingest").getAbsolutePath()));
         connector.stubTxCommit();

         claim.assertXML("https://HOSTNAME/o:emile.ingest.lido.1/LIDO_SOURCE","//tei:term[@id='oth.1']" ); 
         claim.assertXML("https://HOSTNAME/o:emile.ingest.lido.1/LIDO_SOURCE","//lido:gml//gml:metaDataProperty" ); 


    }  
    
    
    @Test 
    public void Test018TriggerUploadWorkflow() throws ClientProtocolException, IOException, Exception {

     	 UploadOptions options = new UploadOptions(); 
   	  
         log.info("Testing API Functions: TriggerUploadWorkflow for MEI objects with source documents");
          	
   //    options.setDCify();
        
         connector.stubTxBegin();
      	 connector.stubCloneObject("o:prototype.mei", "o:emile.ingest.mei.1", group);          	
         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.mei.1", getStream("objects/mei/ingest/abc/abc.txt"), options.get(), "o:prototype.context", null));
         connector.stubTxCommit();

         claim.assertXML("https://HOSTNAME/o:emile.ingest.mei.1/MEI_SOURCE","/mei:mei/mei:music" ); 
         
         connector.stubTxBegin();
    	 connector.stubCloneObject("o:prototype.mei", "o:emile.ingest.mei.2", group);          	
     	 claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.mei.2", getStream("objects/mei/ingest/humdrum/humdrum.krn"), options.get(), "o:prototype.context", null));
         connector.stubTxCommit();
         
         claim.assertXML("https://HOSTNAME/o:emile.ingest.mei.2/MEI_SOURCE","/mei:mei/mei:music" ); 
         
         connector.stubTxBegin();
         connector.stubCloneObject("o:prototype.mei", "o:emile.ingest.mei.3", group);          	
     	 claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.mei.3", getStream("objects/mei/ingest/plaine_easie/plaine_easie.txt"), options.get(), "o:prototype.context", null));
         connector.stubTxCommit();

     	 claim.assertXML("https://HOSTNAME/o:emile.ingest.mei.3/MEI_SOURCE","/mei:mei/mei:music" ); 
         
         options.setValidate(); 	 

         connector.stubTxBegin();
     	 connector.stubCloneObject("o:prototype.mei", "o:emile.ingest.mei.4", group);          	
     	 claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.mei.4", getStream("objects/mei/ingest/mei/mei.xml"), options.get(), "o:prototype.context", null));
         connector.stubTxCommit();
         
         claim.assertXML("https://HOSTNAME/o:emile.ingest.mei.4/MEI_SOURCE","/mei:mei/mei:music" ); 
  
    }  
   
    
    
     @Test 
     public void Test019TriggerXSLTPipelines() throws ClientProtocolException, IOException, Exception {
    	  
      	 log.info("Testing API Functions: TriggerXSLTPipelines for TEI objects");

         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerXSLTPipelines("o:emile.odt"));

         claim.assertXML("https://HOSTNAME/o:emile.odt/TEI_SOURCE","//tei:p[.='Written by OpenOffice']" );
         claim.assertString("https://HOSTNAME/o:emile.odt/POSTAGGED", "ADJD");
         claim.assertXML("https://HOSTNAME/o:emile.odt/VERTICAL", "/doc/head[contains(.,'Blindtext')]");
         claim.assertXML("https://HOSTNAME/o:emile.odt/TOKENIZED","//tei:seg[@type='ws']");
         claim.assertStatus("https://HOSTNAME/o:emile.odt/TCF");          

     }
     
     @Test 
     public void Test020TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
     	  
      	 UploadOptions options = new UploadOptions(); 
         File file;
         
    	 log.info("Testing API Functions: TriggerBatchIngest for TEI objects");
         
         options.setValidate();
         options.setDCify();

         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/tei/ingest").getFile());
   	 
         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.tei", "o:prototype.context", options.get()).startsWith("0:"));
         
         // Save logID of the previous stubTriggerXSLTPipelines process;
         uuid = connector.getLogUUID();
         log.debug("Save LogUUID: "+ uuid);
   
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.tei.1/DC");           
         claim.assertStatus("https://HOSTNAME/context:emile.test/DC");	
         
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/tei/cord").getFile());
       	 
         connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.tei", "o:prototype.context", options.get());
         claim.assertStatus("https://HOSTNAME/o:cord.c3bd5335.df73.3143.9718.147bb2671071");    
         
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/tei/taxonomy").getFile());
         
         connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.tei", "o:prototype.context", options.get());
         claim.assertStatus("https://HOSTNAME/context:gz.taxonomy");           
         claim.assertStatus("https://HOSTNAME/context:gz.italy");           
         claim.assertStatus("https://HOSTNAME/context:gz.latium");      
                          
         options.setUploadImages();
         options.setMETSify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/tei/mets").getFile());

         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.tei", "o:prototype.context", options.get()).startsWith("0:"));
         claim.assertXML("https://HOSTNAME/o:emile.ingest.tei.5/METS_SOURCE", "//mets:FContent/mets:xmlData/x:xmpmeta/exif:PixelXDimension");

     }
    
     @Test 
     public void Test021TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
    	
     }

     @Test 
     public void Test022TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
     	  
    	 UploadOptions options = new UploadOptions(); 
         File file;
        
    	 log.info("Testing API Functions: TriggerBatchIngest for METS objects");
         
         options.setValidate();
         options.setDCify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/mets/ingest").getFile());
       
         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.mets", "o:prototype.context", options.get()).startsWith("0:"));
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.mets.1");           
         claim.assertXML("https://HOSTNAME/o:emile.ingest.mets.1/METS_SOURCE","//mets:div[@ID='folio.page.0']" ); 
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.mets.2");           
         claim.assertXML("https://HOSTNAME/o:emile.ingest.mets.2/METS_SOURCE","//mets:div[@ID='folio.page.0']" ); 
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.mets.3");           
         claim.assertXML("https://HOSTNAME/o:emile.ingest.mets.3/METS_SOURCE","//mets:div[@ID='folio.page.0']" ); 	     
 
     }
     
     
     @Test 
     public void Test023TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
     	  
      	 UploadOptions options = new UploadOptions(); 
         File file;
         
    	 log.info("Testing API Functions: TriggerBatchIngest for RDF based objects: RDF und SKOS");
         
         options.setValidate();
         options.setDCify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/ontology/ingest").getFile());
  
         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml;*.ttl", "o:prototype.rdf", "o:prototype.context", options.get()).startsWith("0:"));
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.ontology.1/DC");           
         claim.assertXML("https://HOSTNAME/o:emile.ingest.ontology.1/ONTOLOGY","//*[contains(@rdf:about,'prodomo/pdrAo.000.000.100000006')]" ); 
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.ontology.2/DC");           
         claim.assertXML("https://HOSTNAME/o:emile.ingest.ontology.2/ONTOLOGY","//*[contains(@rdf:about,'prodomo/pdrAo.000.000.100000008')]" );    
 
         options = new UploadOptions(); 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/rdf/ingest").getFile());
    
         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml;*.ttl", "o:prototype.rdf", "o:prototype.context", options.get()).startsWith("0:"));
          
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rdf.1/ONTOLOGY", "//rdf:Description[@rdf:about = 'http://example.org#Chamois']");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rdf.1/ONTOLOGY", "//rdf:type[@rdf:resource = 'http://example.org#Chamois']");
         
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/skos/ingest").getFile());

         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml;*.ttl", "o:prototype.skos", "o:prototype.context", options.get()).startsWith("0:"));
         
         claim.assertXML("https://HOSTNAME/o:emile.ingest.skos.1/ONTOLOGY", "//rdf:Description[@rdf:about = 'http://example.org#Chamois']");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.skos.1/ONTOLOGY", "//rdf:type[@rdf:resource = 'http://example.org#Chamois']");
       
     }
     
     @Test 
     public void Test024TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
     	  
      	 UploadOptions options = new UploadOptions(); 
         File file;
         
    	 log.info("Testing API Functions: TriggerBatchIngest for GML objects");
         
         options.setValidate();
         options.setDCify();
         options.setRDFify();

         connector.stubCloneObject("o:prototype.gml", "o:emile.ingest.gml.1", group);
           
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/gml/ingest").getFile());
    
         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.gml", "o:prototype.context", options.get()).startsWith("0:"));             
         
         claim.assertXML("https://HOSTNAME/o:emile.ingest.gml.1/GeoRDF_TRIPLES", "//dc:title[.='Centre Georges Pompidou']");
     }
     
     
     @Test 
     public void Test025TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
     	  
      	 UploadOptions options = new UploadOptions(); 
         File file;
         
    	 log.info("Testing API Functions: TriggerBatchIngest for CUBE objects");
         
         options.setValidate();
         options.setDCify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/cube/ingest").getFile());

         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), null, "o:prototype.cube", "o:prototype.context", options.get()).startsWith("0:"));
    
         assertTrue(connector.stubGetDatastream("o:emile.ingest.cube.1", "MULTIRESMODEL").length > 190000);   
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.cube.1");     
         claim.assertStatus("https://HOSTNAME/cubecache/emile.ingest.cube.1-multiresmodel.nxz");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.cube.2");     
         claim.assertStatus("https://HOSTNAME/cubecache/emile.ingest.cube.2-multiresmodel.nxz");
         
     }
         
     @Test 
     public void Test026TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {

     	 UploadOptions options = new UploadOptions(); 
         File file;
         
         log.info("Testing API Functions: TriggerBatchIngest for RTI objects");
         
         options.setValidate();
         options.setDCify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/rti/ingest").getFile());
   
         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), null, "o:prototype.rti", "o:prototype.context", options.get()).startsWith("0:"));
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1");     
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/INFO");     
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/MATERIALS");     
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/PLANE_0");                    		 
 
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2");     

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2/INFO");     
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2/MATERIALS");     
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2/PLANE_0");                    		 
 
     } 
     
     
     @Test
     public void Test027TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {

     	 UploadOptions options = new UploadOptions(); 
         File file;
         
         log.info("Testing API Functions: TriggerBatchIngest for Spectral objects");
         
         options.setDCify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/spectral/ingest").getFile());

         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), null, "o:prototype.spectral", "o:prototype.context", options.get()).startsWith("0:"));
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1/DC");     
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.1/METS_SOURCE","/mets:mets/mets:structMap//mets:div[@TYPE='MP1']//mets:area[@FILEID='FTIR.MP1']" );
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1/sdef:Spectral/get");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.2/DC");     
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.2/METS_SOURCE","/mets:mets/mets:structMap//mets:div[@TYPE='POINTDATA']//mets:fptr[@FILEID='FTIR.MP1']" );
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.2/sdef:Spectral/get");
        
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.3/DC");     
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.3/METS_SOURCE","/mets:mets/mets:structMap//mets:div[@LABEL='Pointdata']//mets:fptr[@FILEID='FTIR.MP1']" );
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.3/sdef:Spectral/get");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4/DC");     
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.4/METS_SOURCE","/mets:mets/mets:structMap//mets:div[@TYPE='POINTDATA']//mets:fptr[@FILEID='FTIR.MP1']" );
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4/sdef:Spectral/get");
 
     }
     
     @Test 
     public void Test028TriggerBatchIngest() throws ClientProtocolException, IOException, Exception {
     	  
      	 UploadOptions options = new UploadOptions(); 
         File file;
         
    	 log.info("Testing API Functions: TriggerBatchIngest for RDO objects");
         
  //       options.setValidate();
 //        options.setDCify();
    	 
         file = new File(getClass().getClassLoader().getResource("org/emile/cirilo/objects/rdo/ingest").getFile());

         Assert.assertTrue(connector.stubTriggerBatchIngest(group, file.getAbsolutePath(), "*.xml", "o:prototype.rdo", "o:prototype.context", options.get()).startsWith("0:"));
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rdo.1");     
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rdo.1/DESCRIPTION");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rdo.1/DESCRIPTION", "//dc:title[. = 'Welcome to Jupyter!']");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rdo.1/DS.1");          
 
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rdo.1/sdef:RDO/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rdo.1/sdef:RDO/get","//xhtml:h4[contains(., 'Welcome to Jupyter!')]" );
              
     }
     
  

     @Test 
     public void Test029GetLogInfo() throws ClientProtocolException, IOException, Exception {
  
       	 log.info("Testing API Functions: GetLogInfo");      
         	
         ArrayList<LogList> ls = connector.stubGetLogInfo("INFO", username, uuid);
         Assert.assertNotEquals(0, ls.size());

       	 log.debug("# Get log entries with logID " + uuid + " as a LogList ArrayList");
    	 for (LogList le : ls) {
    		log.debug(String.format("  %-6s %s", le.getLevel(), le.getMessage()));
    	 }   
    	
       	 log.debug("# Get log entries with logID " + uuid + " serialized as a SPARQL result");
      	 connector.stubGetLogInfoAsRDF("INFO", username, uuid);
      } 	 
	    
     @Test 
     public void Test030TriggerFromSpreadsheetIngest() throws ClientProtocolException, IOException, Exception {
    	 
      	 log.info("Testing API Functions: TriggerFromSpreadsheetIngest for TEI objects");
    	 
    	 UploadOptions options = new UploadOptions(); 
    	 
    	 options.setValidate();
       	
    	 claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerFromSpreadsheetIngest(group, "o:prototype.tei", "o:prototype.context", options.get(), getStream("objects/tei/spreadsheet/tei.xlsx"), getStream("objects/tei/spreadsheet/tei_template.xml")));
     
    	 claim.assertStatus("https://HOSTNAME/o:emile.sheet.1/DC");           
         claim.assertStatus("https://HOSTNAME/o:emile.sheet.2/DC");           
                 	
     }    

     
     @Test 
     public void Test031BuildNexus() throws ClientProtocolException, IOException, Exception {
    	 
      	 log.info("Testing API Functions: BuildNexus for PLY and OBJ format");
             
         byte[] laurana = connector.stubBuildNexus(getClass().getClassLoader().getResource("org/emile/cirilo/objects/cube/ingest/laurana").getFile(), "laurana.ply");
         assertTrue(laurana.length > 190000);    
      
     }    
     
     @Test 
     public void Test032TriggerAggregatorFactory() throws ClientProtocolException, IOException, Exception {
         
       	 UploadOptions options = new UploadOptions(); 
     	 String pid = "context:emile";
 
         log.info("Testing API Functions: TriggerAggregatorFactory for CMIF, KML and PELAGIOS schema based metadata");
        
         options.setSKOSify();
         options.setGEOifyByIds();
         
         claim.assertStatus(HttpStatus.SC_OK, connector.stubTriggerUploadWorkflow("o:emile.ingest.tei.1", getStream("objects/tei/import/tei_source.xml"), options.get(), "o:prototype.context", null));
 
         claim.assertStatus(HttpStatus.SC_OK, connector.stubUpdateAggregationDatastream(pid, "KML"));
         
         claim.assertStatus("https://HOSTNAME/context:emile/KML");
         claim.assertXML("https://HOSTNAME/context:emile/KML","//kml:Document" );
         
         claim.assertStatus(HttpStatus.SC_OK, connector.stubUpdateAggregationDatastream(pid, "PELAGIOS"));
         
         claim.assertStatus("https://HOSTNAME/context:emile/PELAGIOS");
         claim.assertXML("https://HOSTNAME/context:emile/PELAGIOS","//pelagios:relation[contains(@rdf:resource,'http://pelagios.github.io/vocab/relations#foundAt')]" );       

         claim.assertStatus(HttpStatus.SC_OK, connector.stubUpdateAggregationDatastream(pid, "CMIF"));
         
         claim.assertStatus("https://HOSTNAME/context:emile/CMIF");
         claim.assertXML("https://HOSTNAME/context:emile/CMIF","//tei:correspAction/tei:persName[contains(.,'Ascoli')]" );
         
    }    

    @Test 
    public void Test033HandleSystem() throws ClientProtocolException, IOException, Exception {
 
      	String pid = "o:emile.gml";
      	String hdl;
     	     	
      	// Handle server is configured
      	if (superuser && connector.stubIsConnectedToHandleSystem()) {
      	
          	log.info("Testing API Functions: Handle System Requests");

          	hdl = connector.stubHdlCreate(null, pid);
          	
      		// Workaround for duplicated content of response
      		hdl = StringUtils.countMatches(hdl, "hdl:") > 1 ? hdl.substring(0, hdl.length()/2) : hdl; 
   
          	claim.assertXML("https://HOSTNAME/"+pid+"/RELS-EXT","//oai:itemID[contains(.,'hdl:')]");
      		claim.assertStatus(HttpStatus.SC_OK, connector.stubHdlDelete(hdl, pid));      
      		
          	hdl = connector.stubHdlCreate(null, pid);
          	
      		// Workaround for duplicated content of response
      		hdl = StringUtils.countMatches(hdl, "hdl:") > 1 ? hdl.substring(0, hdl.length()/2) : hdl; 

          	claim.assertXML("https://HOSTNAME/"+pid+"/RELS-EXT","//oai:itemID[contains(.,'hdl:')]");
      		claim.assertStatus(HttpStatus.SC_OK, connector.stubHdlDelete(hdl, pid));      

       	}
         
    }
    
    @Test 
	public void Test034Skosify() throws ClientProtocolException, IOException, Exception {

     	log.info("Testing API Functions: Skosify");
	    
	    XMLUtils.createDocumentFromByteArray(connector.stubSkosify(getStream("objects/skos/ontology.xml")));
	    
	}

    @Test 
    public void Test035TransactionSystem() throws ClientProtocolException, IOException, Exception {	
		
	   	Element property = new Element("isMemberOf", Namespaces.xmlns_F3_relations).setAttribute("resource", "http://apache:82/context:cm4f", Namespaces.xmlns_rdf);
	       
     	String pid  = "o:emile.skos";
     	String dsid = "DITA";
       
        log.info("Testing Transaction System");	
        
        connector.stubTxBegin();
        connector.stubAddDatastream(pid, dsid + ".1", getStream("objects/odt/tei.docx"), "text/xml", null);
        connector.stubTxRollback();
        
        log.info("This test generates the error message 'Datastream o:emile.skos/DITA.1 not found' in the log file of fcgate container");
        claim.assertStatus(HttpStatus.SC_INTERNAL_SERVER_ERROR, "https://HOSTNAME/"+pid+"/"+dsid + ".1");
   
        
        connector.stubTxBegin();
        connector.stubAddProperty(pid, property);
        connector.stubAddDatastream(pid, dsid, getStream("objects/odt/tei.odt"), "application/vnd.oasis.opendocument.text", null);
        connector.stubTxCommit();

        claim.assertStatus(HttpStatus.SC_OK, "https://HOSTNAME/"+pid+"/"+dsid);
        
    }
  
	@Test 
    public void Test036GetMETSExportFormat() throws ClientProtocolException, IOException, Exception {	

     	log.info("Testing METS Export and Import Format");	
	 
		Document mets = new SAXBuilder().build(new StringReader(new String(connector.stubGetMETS("o:emile.bibtex"),"UTF-8")));
		
		Element fcontent = XMLUtils.getChild("//mets:file[contains(@ID,'DC')]/mets:FContent/mets:binData", mets);
		byte[] decodedBytes = Base64.getMimeDecoder().decode(fcontent.getText());
		
		Document dc = new SAXBuilder().build(new StringReader(new String(decodedBytes)));
		Assert.assertEquals(XMLUtils.getChild("//dc:publisher", dc).getText(), "emile.org");
				
	}

    @Test
    public void TestBIBTEXDisseminators() throws ClientProtocolException, IOException, Exception {

   	
        log.info("Testing BibTeX disseminators");
        
        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/BIBTEX");
        claim.assertXML("https://HOSTNAME/o:emile.bibtex/BIBTEX","//bibtex:book" );
      
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/datastreams/BIBTEX/content");
        claim.assertXML("https://HOSTNAME/archive/objects/o:emile.bibtex/datastreams/BIBTEX/content","//bibtex:book" );
       
        // --- Legacy Layer Requests
        
        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:Object/get");
        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:Object/getRDF");

        claim.assertStatus("https://HOSTNAME/o:emile.bibtex");
        claim.assertXML("https://HOSTNAME/o:emile.bibtex","//xhtml:p[contains(.,'Baravalle Robert')]" );
        
        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/get");
        claim.assertXML("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/get","//xhtml:p[contains(.,'Baravalle Robert')]" );
       
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/get");
        claim.assertXML("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/get","//xhtml:p[contains(.,'Baravalle Robert')]" );
        
        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getPDF");
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getPDF");
               
        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getBibTeX");
        claim.assertString("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getBibTeX", "Alma Universitas");
        
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getBibTeX");
        claim.assertString("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getBibTeX", "Alma Universitas");

        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getRIS");
        claim.assertString("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getRIS", "Alma Universitas");
          
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getRIS");
        claim.assertString("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getRIS", "Alma Universitas");

        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getENDNOTE");
        claim.assertString("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getENDNOTE", "Alma Universitas");
          
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getENDNOTE");
        claim.assertString("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getENDNOTE", "Alma Universitas");

        claim.assertStatus("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getMODS");
        claim.assertXML("https://HOSTNAME/o:emile.bibtex/sdef:BibTeX/getMODS","//mods:title[contains(.,'Alma Universitas')]" );
            
        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getMODS");
        claim.assertXML("https://HOSTNAME/archive/objects/o:emile.bibtex/sdef:BibTeX/getMODS","//mods:title[contains(.,'Alma Universitas')]" );

        // --- APIX Requests
        /*
        String path = LegacySystem.getPath("o:emile.bibtex");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:bibtex","//xhtml:p[contains(.,'Baravalle Robert')]" );

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex?method=get");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:bibtex","//xhtml:p[contains(.,'Baravalle Robert')]" );
           
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getpdf");
       
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getbibtex");
        claim.assertString("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getbibtex", "Alma Universitas");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getris");
        claim.assertString("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getris", "Alma Universitas");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getendnote");
        claim.assertString("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getendnote", "Alma Universitas");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getmods");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:bibtex?method=getmods","//mods:title[contains(.,'Alma Universitas')]" );
        */
        
    }
    
    
    @Test
    public void TestContextDisseminators() throws ClientProtocolException, IOException, Exception {

        log.info("Testing Context disseminators");

        claim.assertStatus("https://HOSTNAME/context:emile/QUERY");
        claim.assertString("https://HOSTNAME/context:emile/QUERY","context:emile");
        
        claim.assertStatus("https://HOSTNAME/archive/objects/context:emile/datastreams/QUERY/content");
        claim.assertString("https://HOSTNAME/archive/objects/context:emile/datastreams/QUERY/content","context:emile");

        // --- Legacy Layer Requests
    
        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Object/get");
        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Object/getRDF");

        claim.assertStatus("https://HOSTNAME/context:emile");
        claim.assertXML("https://HOSTNAME/context:emile","//xhtml:td[contains(.,'cm:Ontology')]" );

        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Context/get");
        claim.assertXML("https://HOSTNAME/context:emile/sdef:Context/get","//xhtml:td[contains(.,'cm:Ontology')]" );

        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Context/getIIIFCollection");
        claim.assertJSON("https://HOSTNAME/context:emile/sdef:Context/getIIIFCollection", "{@type: \"sc:Collection\"}");

        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Context/getMirador");
        claim.assertXML("https://HOSTNAME/context:emile/sdef:Context/getMirador","//div[@id='mirador']" );
        
        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Context/getNeighbours?identifier=o:emile.pdf");
        claim.assertXML("https://HOSTNAME/context:emile/sdef:Context/getNeighbours?identifier=o:emile.pdf","/neighbours/next/identifier" );

        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Context/getXML");
        claim.assertXML("https://HOSTNAME/context:emile/sdef:Context/getXML","//sparql:creator[contains(.,'Otto Geist')]" );

        // --- APIX Requests
        /*
        String path = LegacySystem.getPath("context:emile");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:context");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:context","//xhtml:td[contains(.,'cm:Ontology')]" );

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:context?method=get");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:context?method=get","//xhtml:td[contains(.,'cm:Ontology')]" );
    
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:context?method=getiiifcollection");
        claim.assertJSON("https://HOSTNAME/services/"+path+"/svc:context?method=getiiifcollection", "{@type: \"sc:Collection\"}");

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:context?method=getneighbours&identifier=o:emile.pdf");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:context?method=getneighbours&identifier=o:emile.pdf","/neighbours/next/identifier" );

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:context?method=getxml");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:context?method=getxml","//sparql:creator[contains(.,'Otto Geist')]" );
        */
    }
  
    
    @Test
    public void TestCorpusDisseminators() throws ClientProtocolException, IOException, Exception {
         
        log.info("Testing Corpus disseminators");

        claim.assertStatus("https://HOSTNAME/corpus:emile/QUERY");
        claim.assertString("https://HOSTNAME/corpus:emile/QUERY","corpus:emile");
        
        claim.assertStatus("https://HOSTNAME/archive/objects/corpus:emile/datastreams/QUERY/content");
        claim.assertString("https://HOSTNAME/archive/objects/corpus:emile/datastreams/QUERY/content","corpus:emile");

        claim.assertStatus("https://HOSTNAME/corpus:emile/CMDI");
        claim.assertXML("https://HOSTNAME/corpus:emile/CMDI","//cmd:MdCollectionDisplayName");
        
        claim.assertStatus("https://HOSTNAME/archive/objects/corpus:emile/datastreams/CMDI/content");
        claim.assertXML("https://HOSTNAME/archive/objects/corpus:emile/datastreams/CMDI/content","//cmd:MdCollectionDisplayName");

        claim.assertStatus("https://HOSTNAME/corpus:emile/DSPIN");
        claim.assertXML("https://HOSTNAME/corpus:emile/DSPIN","//cmd:MdCollectionDisplayName");
        
        claim.assertStatus("https://HOSTNAME/archive/objects/corpus:emile/datastreams/DSPIN/content");
        claim.assertXML("https://HOSTNAME/archive/objects/corpus:emile/datastreams/DSPIN/content","//cmd:MdCollectionDisplayName");
 
        claim.assertStatus("https://HOSTNAME/corpus:emile/TCF");
        claim.assertXML("https://HOSTNAME/corpus:emile/TCF","//tcf:TextCorpus");
        
        claim.assertStatus("https://HOSTNAME/archive/objects/corpus:emile/datastreams/TCF/content");
        claim.assertXML("https://HOSTNAME/archive/objects/corpus:emile/datastreams/TCF/content","//tcf:TextCorpus");    
        
        // --- Legacy Layer Requests

        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Object/get");
        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Object/getRDF");

        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Corpus/get");
        claim.assertXML("https://HOSTNAME/corpus:emile/sdef:Corpus/get","//xhtml:td[contains(.,'Test Object')]");

        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Corpus/getCMDI");
        claim.assertXML("https://HOSTNAME/corpus:emile/sdef:Corpus/getCMDI","//cmd:MdCollectionDisplayName");

        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Corpus/getDSPIN");
        claim.assertXML("https://HOSTNAME/corpus:emile/sdef:Corpus/getDSPIN","//cmd:MdCollectionDisplayName");
       
        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Corpus/getTCF");
        claim.assertXML("https://HOSTNAME/corpus:emile/sdef:Corpus/getTCF","//tcf:POStags");
       
        claim.assertStatus("https://HOSTNAME/corpus:emile/sdef:Corpus/getXML");
        claim.assertXML("https://HOSTNAME/corpus:emile/sdef:Corpus/getXML","//sparql:results/sparql:result");

        // --- APIX Requests
        /*
        String path = LegacySystem.getPath("corpus:emile");

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:corpus?method=get");        
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:corpus?method=get", "//xhtml:td[contains(.,'Test Object')]");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:corpus?method=getcmdi");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:corpus?method=getcmdi","//cmd:MdCollectionDisplayName");
      
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:corpus?method=getdspin");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:corpus?method=getdspin","//cmd:MdCollectionDisplayName");
              
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:corpus?method=gettcf");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:corpus?method=gettcf","//tcf:POStags");
       
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:corpus?method=getxml");
        claim.assertXML("https://HOSTNAME/services/"+path+"/svc:corpus?method=getxml","//sparql:results/sparql:result");
   		*/
     }    
  
     @Test
     public void TestGMLDisseminators() throws ClientProtocolException, IOException, Exception {
         
        log.info("Testing GML disseminators"); 
         
        claim.assertStatus("https://HOSTNAME/o:emile.gml/GML_SOURCE");
        claim.assertXML("https://HOSTNAME/o:emile.gml/GML_SOURCE", "//gml:featureMember" );

        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.gml/datastreams/GML_SOURCE/content");
        claim.assertXML("https://HOSTNAME/archive/objects/o:emile.gml/datastreams/GML_SOURCE/content", "//gml:featureMember" );
        
        // --- Legacy Layer Requests
        
        claim.assertStatus("https://HOSTNAME/o:emile.gml/sdef:Object/get");
        claim.assertStatus("https://HOSTNAME/o:emile.gml/sdef:Object/getRDF");

        claim.assertStatus("https://HOSTNAME/o:emile.gml");
        claim.assertHTML("https://HOSTNAME/o:emile.gml", "b:matches(^Eiffel Tower$)" );
        
        claim.assertStatus("https://HOSTNAME/o:emile.gml/sdef:GML/get");
        claim.assertHTML("https://HOSTNAME/o:emile.gml/sdef:GML/get", "b:matches(^Eiffel Tower$)" );

        claim.assertStatus("https://HOSTNAME/o:emile.gml/sdef:GML/getJSON");
        claim.assertJSON("https://HOSTNAME/o:emile.gml/sdef:GML/getJSON", "{\"type\": \"Feature\"}" );
        
        // --- APIX Requests 
        /*
        String path = LegacySystem.getPath("o:emile.gml");

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:gml");
        claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:gml", "p:matches(^125.6 10.1$)" );
                
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:gml?method=getJSON");
        claim.assertJSON("https://HOSTNAME/services/"+path+"/svc:gml?method=getJSON", "{\"type\": \"Feature\"}" );
        */
     }   
 
     
     @Test
     public void TestHTMLDisseminators() throws ClientProtocolException, IOException, Exception {
         
       	log.info("Testing HTML disseminators"); 
         
        claim.assertStatus("https://HOSTNAME/o:emile.html/HTML_STREAM");
        claim.assertHTML("https://HOSTNAME/o:emile.html/HTML_STREAM", "h1:matches(^HTML stream automatically generated by GAMS$)" );

        claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.html/datastreams/HTML_STREAM/content");
        claim.assertHTML("https://HOSTNAME/archive/objects/o:emile.html/datastreams/HTML_STREAM/content", "h1:matches(^HTML stream automatically generated by GAMS$)" );
        
        // --- Legacy Layer Requests
        
        claim.assertStatus("https://HOSTNAME/o:emile.html/sdef:Object/get");
        claim.assertStatus("https://HOSTNAME/o:emile.html/sdef:Object/getRDF");

        claim.assertStatus("https://HOSTNAME/o:emile.html");
        claim.assertHTML("https://HOSTNAME/o:emile.html", "h1:matches(^HTML stream automatically generated by GAMS$)" );
        
        claim.assertStatus("https://HOSTNAME/o:emile.html/sdef:HTML/get");
        claim.assertHTML("https://HOSTNAME/o:emile.html/sdef:HTML/get", "h1:matches(^HTML stream automatically generated by GAMS$)" );

        // --- APIX Requests 
        /*
        String path = LegacySystem.getPath("o:emile.html");
        
        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:html");
        claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:html", "h1:matches(^HTML stream automatically generated by GAMS$)" );

        claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:html?method=get");
        claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:html?method=get", "h1:matches(^HTML stream automatically generated by GAMS$)" );
        */
     }   
     
     
     @Test
     public void TestIIIFDisseminators() throws ClientProtocolException, IOException, Exception {
    	 
    	 String path;
    	 
         log.info("Testing IIIF disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.tei/METS_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.tei/METS_SOURCE","//mets:structMap[@TYPE='LOGICAL']" );
        
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.tei/datastreams/METS_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.tei/datastreams/METS_SOURCE/content","//mets:structMap[@TYPE='LOGICAL']" );

         // --- Legacy Layer Requests
       
         claim.assertStatus("https://HOSTNAME/iiif/o:emile.tei/FOL_1R/full/full/0/default.jpg");
         
         claim.assertStatus("https://HOSTNAME//iiif/o:emile.tei/FOL_1V/full/full/0/default.jpg");
           
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:IIIF/getManifest");
         claim.assertJSON("https://HOSTNAME/o:emile.tei/sdef:IIIF/getManifest", "{@id: \"https://"+hostname+"/o:emile.tei\"}");
         
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:IIIF/getTileSources");
         claim.assertJSON("https://HOSTNAME/o:emile.tei/sdef:IIIF/getTileSources", "{images: [\"https://"+hostname+"/iiif/o:emile.tei/FOL_1R/info.json\",\"https://"+hostname+"/iiif/o:emile.tei/FOL_1V/info.json\"]}");
         
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:IIIF/getMirador");
         claim.assertXML("https://HOSTNAME/o:emile.tei/sdef:IIIF/getMirador","//div[@id='mirador']" );

         claim.assertStatus("https://HOSTNAME/o:emile.lido/sdef:IIIF/getTileSources");
         claim.assertJSON("https://HOSTNAME/o:emile.lido/sdef:IIIF/getTileSources", "{images: [\"https://"+hostname+"/iiif/o:emile.lido/IMAGE.1/info.json\"]}");   
         
         // --- APIX Requests 
         /*
         path = LegacySystem.getPath("o:emile.tei");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:iiif?method=getmanifest");
         claim.assertJSON("https://HOSTNAME/services/"+path+"/svc:iiif?method=getmanifest", "{@id: \"https://"+hostname+"/o:emile.tei\"}");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:iiif?method=gettilesources");
         claim.assertJSON("https://HOSTNAME/services/"+path+"/svc:iiif?method=gettilesources", "{images: [\"https://"+hostname+"/iiif/o:emile.tei/FOL_1R/info.json\",\"https://"+hostname+"/iiif/o:emile.tei/FOL_1V/info.json\"]}");

         path = LegacySystem.getPath("o:emile.lido");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:iiif?method=gettilesources");
         claim.assertJSON("https://HOSTNAME/services/"+path+"/svc:iiif?method=gettilesources", "{images:  [\"https://"+hostname+"/iiif/o:emile.lido/IMAGE.1/info.json\"]}");
		 */
     }    
     
     
     @Test
     public void TestLATEXDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing LaTeX disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.latex/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.latex/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.latex/LATEX_SOURCE");
         claim.assertString("https://HOSTNAME/o:emile.latex/LATEX_SOURCE", "Introduction");

         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.latex/datastreams/LATEX_SOURCE/content");
         claim.assertString("https://HOSTNAME/archive/objects/o:emile.latex/datastreams/LATEX_SOURCE/content", "Introduction");

         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.latex");
         
         claim.assertStatus("https://HOSTNAME/o:emile.latex/sdef:LaTeX/get");
         
         claim.assertStatus("https://HOSTNAME/o:emile.latex/sdef:BibTeX/get");

         claim.assertStatus("https://HOSTNAME/o:emile.latex/sdef:BibTeX/getPDF");
         
         // --- APIX Requests 
         /*
         String path = LegacySystem.getPath("o:emile.latex");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:latex");
        
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:latex?method=get");
          */
     }

     
     @Test
     public void TestLIDODisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing LIDO disseminators");

         claim.assertStatus("https://HOSTNAME/o:emile.lido/LIDO_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.lido/LIDO_SOURCE","//lido:term[contains(.,'Einzelobjekt')]" );
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.lido/datastreams/LIDO_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.lido/datastreams/LIDO_SOURCE/content","//lido:term[contains(.,'Einzelobjekt')]" );
                
         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.lido/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.lido/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.lido");
         claim.assertXML("https://HOSTNAME/o:emile.lido","//xhtml:a[contains(., 'CC BY-SA 3.0 AT')]");
         
         claim.assertStatus("https://HOSTNAME/o:emile.lido/sdef:LIDO/get");
         claim.assertXML("https://HOSTNAME/o:emile.lido/sdef:LIDO/get","//xhtml:a[contains(., 'CC BY-SA 3.0 AT')]");
                
         claim.assertStatus("https://HOSTNAME/o:emile.lido/sdef:LIDO/getPDF");

         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.lido");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:lido");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:lido", "//xhtml:a[contains(., 'CC BY-SA 3.0 AT')]");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:lido?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:lido?method=get", "//xhtml:a[contains(., 'CC BY-SA 3.0 AT')]");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:lido?method=getpdf");
    	 */
     }
     
     
     @Test
     public void TestMAPDisseminators() throws ClientProtocolException, IOException, Exception {
         
         log.info("Testing MAP disseminators");
         
         // --- Legacy Layer Requests
         
         claim.assertStatus("https://HOSTNAME/context:emile/sdef:Map/get");
         claim.assertHTML("https://HOSTNAME/context:emile/sdef:Map/get", "legend:matches(^Load Data$)" );
         
         claim.assertStatus("https://HOSTNAME/context:emile/sdef:Map/getPlatin");
         claim.assertHTML("https://HOSTNAME/context:emile/sdef:Map/getPlatin", "legend:matches(^Load Data$)" );
         
 //        claim.assertStatus("https://HOSTNAME/context:emile/sdef:Map/getDariah");
 //        claim.assertHTML("https://HOSTNAME/context:emile/sdef:Map/getDariah", "h2:matches(^Dataset Information$)" );
                                           
     }

     @Test
     public void TestMEIDisseminators() throws ClientProtocolException, IOException, Exception {

       	 if (nokeycloakhost) return;
      	 
         log.info("Testing MEI disseminators");

         claim.assertStatus("https://HOSTNAME/o:emile.mei/MEI_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.mei/MEI_SOURCE","/mei:mei/mei:music" );
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.mei/datastreams/MEI_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.mei/datastreams/MEI_SOURCE/content","/mei:mei/mei:music" );
                
         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.mei/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.mei/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.mei");
         claim.assertXML("https://HOSTNAME/o:emile.mei","//xhtml:p[contains(., 'Frédéric Chopin')]");
         
         claim.assertStatus("https://HOSTNAME/o:emile.mei/sdef:MEI/get");
         claim.assertXML("https://HOSTNAME/o:emile.mei/sdef:MEI/get","//xhtml:p[contains(., 'Frédéric Chopin')]");
         
         claim.assertStatus("https://HOSTNAME/o:emile.mei/sdef:MEI/getScore");
         claim.assertXML("https://HOSTNAME/o:emile.mei/sdef:MEI/getScore","//div[@id='LoadStyler_Preloader']");
                
         claim.assertStatus("https://HOSTNAME/o:emile.mei/sdef:MEI/getSVG");
         claim.assertXML("https://HOSTNAME/o:emile.mei/sdef:MEI/getSVG","//svg:tspan[. = 'Etude in F Minor']");
       
 
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.mei");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:mei");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:mei","//xhtml:p[contains(., 'Frédéric Chopin')]");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:mei?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:mei?method=get","//xhtml:p[contains(., 'Frédéric Chopin')]");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:mei?method=getscore");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:mei?method=getscore","//script[contains(., '/services/objects/emile/2d864b5b/308b/3f09/a072/c5d0d714f9e9/svc:mei?method=getsvg')]");
                
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:mei?method=getsvg");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:mei?method=getsvg","//svg:tspan[. = 'Etude in F Minor']");
    	 */
     }
 
     
     @Test
     public void TestMETSDisseminators() throws ClientProtocolException, IOException, Exception {
         
         log.info("Testing METS disseminators");

         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.mets.3/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.mets.3/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.mets.3/sdef:METS/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.mets.3/sdef:METS/get","//div[@id='BookReader']" );
                                          
     }
   
     @Test
     public void TestObjectDisseminators() throws ClientProtocolException, IOException, Exception {
    	     	 
    	 log.info("Testing Object disseminators");
    	         
    	 claim.assertStatus("https://HOSTNAME/o:emile.tei");
    	 claim.assertXML("https://HOSTNAME/o:emile.tei","//xhtml:span[.='Lorem ipsum']" );
    	 
    	 claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getRDF");
    	 claim.assertXML("https://HOSTNAME/o:emile.tei/sdef:Object/getRDF", "//xhtml:a[contains(@href,'/o:emile.tei')]");
    	                
    	 //    claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/get"); //hasXsltToDefaultView
    	 //    claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getDC"); //hasXsltToDC
    	 //    claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getEDM"); //hasXsltToEDM
    	 //    claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getDatacite"); //hasXsltToDATACITE
    	 //    claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getDHAOntology"); //hasXsltToDHAOntology
    	         
    	 claim.assertStatus("https://HOSTNAME/o:emile.tei/METADATA");
    	 claim.assertXML("https://HOSTNAME/o:emile.tei/METADATA","//*[@uri='info:fedora/o:emile.tei']" );
    	 claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getMetadata");
    	 claim.assertXML("https://HOSTNAME/o:emile.tei/sdef:Object/getMetadata","//*[@uri='info:fedora/o:emile.tei']" );

    	 claim.assertStatus("https://HOSTNAME/context:emile");
    	 claim.assertXML("https://HOSTNAME/context:emile","//xhtml:td[.='info:fedora/cm:TEI']" );
    	     
    	 claim.assertStatus("https://HOSTNAME/context:emile/sdef:Object/getRDF");
    	 claim.assertXML("https://HOSTNAME/context:emile/sdef:Object/getRDF", "//xhtml:a[contains(@href,'/context:emile')]");

    	 claim.assertStatus("https://HOSTNAME/context:emile/METADATA");
    	 claim.assertXML("https://HOSTNAME/context:emile/METADATA","//*[@uri='info:fedora/cm:TEI']" );
    	 claim.assertStatus("https://HOSTNAME/context:emile/sdef:Object/getMetadata");
    	 claim.assertXML("https://HOSTNAME/context:emile/sdef:Object/getMetadata","//*[@uri='info:fedora/cm:TEI']" );
    	  
     }     
     
     @Test
     public void TestOntologyDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing Ontology disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.ontology/ONTOLOGY");
         claim.assertXML("https://HOSTNAME/o:emile.ontology/ONTOLOGY","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
                        
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.ontology/datastreams/ONTOLOGY/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.ontology/datastreams/ONTOLOGY/content","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
                        
         // --- Legacy Layer Requests
         
         claim.assertStatus("https://HOSTNAME/o:emile.ontology/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.ontology/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.ontology");
         claim.assertXML("https://HOSTNAME/o:emile.ontology","//xhtml:p[contains(@class,'dc:title')]" );

         claim.assertStatus("https://HOSTNAME/o:emile.ontology/sdef:Ontology/get");
         claim.assertXML("https://HOSTNAME/o:emile.ontology/sdef:Ontology/get","//xhtml:p[contains(@class,'dc:title')]" );
           
         claim.assertStatus("https://HOSTNAME/o:emile.ontology/sdef:Ontology/getPDF");
         
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.ontology");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:ontology");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:ontology","//xhtml:p[contains(@class,'dc:title')]" );
 
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:ontology?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:ontology?method=get","//xhtml:p[contains(@class,'dc:title')]" );
  
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:ontology?method=getpdf");
    	 */
     }
     
     
     @Test
     public void TestPDFDisseminators() throws ClientProtocolException, IOException, Exception {
         
         log.info("Testing PDF disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.pdf/PDF_STREAM");
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.pdf/datastreams/PDF_STREAM/content");
         
         // --- Legacy Layer Requests
         
         claim.assertStatus("https://HOSTNAME/o:emile.pdf/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.pdf/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.pdf");
         
         claim.assertStatus("https://HOSTNAME/o:emile.pdf/sdef:PDF/get");
         
         claim.assertStatus("https://HOSTNAME/o:emile.pdf/sdef:BibTeX/get");

         claim.assertStatus("https://HOSTNAME/o:emile.pdf/sdef:BibTeX/getPDF");
      
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.pdf");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:pdf");
  
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:pdf?method=get");
         */
     }     
         
     
     @Test
     public void TestQueryDisseminators() throws ClientProtocolException, IOException, Exception {
         
    	 String path;
    	 
         log.info("Testing Query disseminators against Blazegraph");

         claim.assertStatus("https://HOSTNAME/query:emile/QUERY");
         claim.assertString("https://HOSTNAME/query:emile/QUERY","skos:Concept");
         
         claim.assertStatus("https://HOSTNAME/archive/objects/query:emile/datastreams/QUERY/content");
         claim.assertString("https://HOSTNAME/archive/objects/query:emile/datastreams/QUERY/content","skos:Concept");

         // --- Legacy Layer Requests
       
         claim.assertStatus("https://HOSTNAME/query:emile?params=%241%7Cdeu");
         claim.assertHTML("https://HOSTNAME/query:emile?params=%241%7Cdeu", "td:matches(^Mykenisch$)" );
       
         claim.assertStatus("https://HOSTNAME/query:emile/sdef:Query/get?params=%241%7Cdeu");
         claim.assertHTML("https://HOSTNAME/query:emile/sdef:Query/get?params=%241%7Cdeu", "td:matches(^Mykenisch$)" );
      
         claim.assertStatus("https://HOSTNAME/query:emile/sdef:Query/getXML?params=%241%7Cdeu");
         claim.assertXML("https://HOSTNAME/query:emile/sdef:Query/getXML?params=%241%7Cdeu","//sparql:label[.='Mykenisch']" );

         claim.assertStatus("https://HOSTNAME/query:emile/sdef:Query/getHSSF?params=%241%7Cdeu");
  
         claim.assertStatus("https://HOSTNAME/query:emile/sdef:Query/getJSON?params=%241%7Ceng");
         claim.assertJSONArray("https://HOSTNAME/query:emile/sdef:Query/getJSON?params=%241%7Ceng", "[92,500]");
   
         // --- APIX Requests
         /*
         path = LegacySystem.getPath("query:emile");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query?params=%241%7Cdeu");
         claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:query?params=%241%7Cdeu", "td:matches(^Mykenisch$)" );
       
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query?method=get&params=%241%7Cdeu");
         claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:query?method=get&params=%241%7Cdeu", "td:matches(^Mykenisch$)" );

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query?method=getxml&params=%241%7Cdeu");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:query?method=getxml&params=%241%7Cdeu","//sparql:label[.='Mykenisch']" );

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query?method=gethssf&params=%241%7Cdeu");
         */
         
         // --- Legacy Layer Requests
         
         claim.assertStatus("https://HOSTNAME/query:construct/sdef:Query/getXML");
         claim.assertXML("https://HOSTNAME/query:construct/sdef:Query/getXML","//rdf:Description[@rdf:about='http://gams.uni-graz.at/skos/scheme/o:pth/#170010']" );
         
         
         // --- APIX Requests
         /*
         path = LegacySystem.getPath("query:construct");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:query?method=getxml","//rdf:Description[@rdf:about='http://gams.uni-graz.at/skos/scheme/o:pth/#170010']" );
         */
         
         log.info("Testing Query disseminators against RDF4J");

         // --- Legacy Layer Requests
         
         
         claim.assertStatus("https://HOSTNAME/query:geosparql");
         claim.assertHTML("https://HOSTNAME/query:geosparql", "a:matches(^http://example.org/eiffelTower$)" );
   
         claim.assertStatus("https://HOSTNAME/query:geosparql/sdef:Query/get");
         claim.assertHTML("https://HOSTNAME/query:geosparql/sdef:Query/get", "a:matches(^http://example.org/eiffelTower$)" );

         claim.assertStatus("https://HOSTNAME/query:geosparql/sdef:Query/getXML");
         claim.assertXML("https://HOSTNAME/query:geosparql/sdef:Query/getXML","//sparql:lmA[@uri='http://example.org/eiffelTower']" );
         

         // --- APIX Requests
         /*
         path = LegacySystem.getPath("query:geosparql");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query");
         claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:query", "a:matches(^http://example.org/eiffelTower$)" );

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query?method=get");
         claim.assertHTML("https://HOSTNAME/services/"+path+"/svc:query?method=get", "a:matches(^http://example.org/eiffelTower$)");
        
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:query?method=getxml");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:query?method=getxml","//sparql:lmA[@uri='http://example.org/eiffelTower']" );
         */
     }     

    
     @Test
     public void TestRDisseminators() throws ClientProtocolException, IOException, Exception {
         
         log.info("Testing R disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.r/PDF_STREAM");
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.r/datastreams/PDF_STREAM/content");

         claim.assertStatus("https://HOSTNAME/o:emile.r/DATASETS");
         claim.assertXML("https://HOSTNAME/o:emile.r/DATASETS", "//dataframe");

         claim.assertStatus("https://HOSTNAME/o:emile.r/RSCRIPT");
         claim.assertString("https://HOSTNAME/o:emile.r/RSCRIPT", "dataframe");

         
         // --- Legacy Layer Requests

         
         claim.assertStatus("https://HOSTNAME/o:emile.r/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.r/sdef:Object/getRDF");
         
         claim.assertStatus("https://HOSTNAME/o:emile.r");
         
         claim.assertStatus("https://HOSTNAME/o:emile.r/sdef:R/get");
         
         claim.assertStatus("https://HOSTNAME/o:emile.r/sdef:R/compute?scenario=SCENARIO.1");
         
         claim.assertStatus("https://HOSTNAME/o:emile.r/sdef:R/get?mode=SCENARIO.1");

      
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.r");

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:rs");
  
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:rs?method=get");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:rs?method=get&mode=SCENARIO.1");
         */
     }     

     @Test
     public void TestRDFDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing RDF disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.rdf/ONTOLOGY");
         claim.assertXML("https://HOSTNAME/o:emile.rdf/ONTOLOGY","//rdf:type[contains(@rdf:resource,'Chamois')]" );
                        
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.rdf/datastreams/ONTOLOGY/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.rdf/datastreams/ONTOLOGY/content","//rdf:type[contains(@rdf:resource,'Chamois')]" );
                        
         // --- Legacy Layer Requests
               
         claim.assertStatus("https://HOSTNAME/o:emile.rdf/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.rdf/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.rdf");
         claim.assertXML("https://HOSTNAME/o:emile.rdf","//h3[contains(.,'Chamois')]" );

         claim.assertStatus("https://HOSTNAME/o:emile.rdf/sdef:RDF/get");
         claim.assertXML("https://HOSTNAME/o:emile.rdf/sdef:RDF/get","//h3[contains(.,'Chamois')]");
                    
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.rdf");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:rdf");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:rdf","//h3[contains(.,'Chamois')]" );
 
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:rdf?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:rdf?method=get","//h3[contains(.,'Chamois')]" );
      	 */
     }
     
     @Test
     public void TestRDODisseminators() throws ClientProtocolException, IOException, Exception {
         
         log.info("Testing RDO disseminators");

         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.rdo/DESCRIPTION");
         claim.assertXML("https://HOSTNAME/o:emile.rdo/DESCRIPTION","//dc:title[contains(.,'Test Object')]" );
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.rdo/datastreams/DESCRIPTION/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.rdo/datastreams/DESCRIPTION/content","//dc:title[contains(.,'Test Object')]" );
                
         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.rdo/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.rdo/sdef:Object/getRDF");
        
         claim.assertStatus("https://HOSTNAME/o:emile.rdo");
         claim.assertXML("https://HOSTNAME/o:emile.rdo","//xhtml:h4[contains(., 'Test Object')]");
         
         claim.assertStatus("https://HOSTNAME/o:emile.rdo/sdef:RDO/get");
         claim.assertXML("https://HOSTNAME/o:emile.rdo/sdef:RDO/get","//xhtml:h4[contains(., 'Test Object')]");
  
     }
   
  
     @Test
     public void TestResourceDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing Resource disseminators");
         
         // --- Legacy Layer Requests
         
         
         claim.assertStatus("https://HOSTNAME/o:emile.resource/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.resource/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.resource");
         claim.assertHTML("https://HOSTNAME/o:emile.resource","p:matches(^Zentrum für Informationsmodellierung)" );
         
         claim.assertStatus("https://HOSTNAME/o:emile.resource/sdef:Resource/get");
         claim.assertHTML("https://HOSTNAME/o:emile.resource/sdef:Resource/get","p:matches(^Zentrum für Informationsmodellierung)" );
            
     }
    
 
     @Test
     public void TestRTIDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing RTI disseminators");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/LIDO_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.1/LIDO_SOURCE","//lido:legalBodyWeblink[contains(.,'https://example.com')]" );
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.ingest.rti.1/datastreams/LIDO_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.ingest.rti.1/datastreams/LIDO_SOURCE/content","//lido:legalBodyWeblink[contains(.,'https://example.com')]" );
                
         // --- Legacy Layer Requests

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/sdef:Object/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.1/sdef:Object/get","//a[contains(., '/o:emile.ingest.rti.1')]");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2/sdef:Object/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.2/sdef:Object/get","//a[contains(., '/o:emile.ingest.rti.2')]");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2/sdef:Object/getRDF");
        
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.1","//script[contains(., '/o:emile.ingest.rti.1')]");
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.1/sdef:RTI/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.1/sdef:RTI/get","//script[contains(., '/o:emile.ingest.rti.1')]");
                
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.2","//script[contains(., '/o:emile.ingest.rti.2')]");
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.rti.2/sdef:RTI/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.rti.2/sdef:RTI/get","//script[contains(., '/o:emile.ingest.rti.2')]");

         
     }
     
     @Test
     public void TestSKOSDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing SKOS disseminators");

         claim.assertStatus("https://HOSTNAME/o:emile.skos/ONTOLOGY");
         claim.assertXML("https://HOSTNAME/o:emile.skos/ONTOLOGY","//skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
                       
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.skos/datastreams/ONTOLOGY/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.skos/datastreams/ONTOLOGY/content","//skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
                       
         // --- Legacy Layer Requests
         
         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.skos");
         claim.assertXML("https://HOSTNAME/o:emile.skos","//xhtml:p[contains(@class,'dc:title')]" );

         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:SKOS/get");
         claim.assertXML("https://HOSTNAME/o:emile.skos/sdef:SKOS/get","//xhtml:p[contains(@class,'dc:title')]" );
           
         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:SKOS/getPDF");

         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptByURI?uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170010");
         claim.assertXML("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptByURI?uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170010","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
     
         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptByPrefLabel?preflabel=Afrikanisch");
         claim.assertXML("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptByPrefLabel?preflabel=Afrikanisch","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
                                
         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptByExternalID?externalid=");
         claim.assertXML("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptByExternalID?externalid=","rdf:RDF" );
                   
         claim.assertStatus("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptRelatives?uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170000&relation=narrower");
         claim.assertXML("https://HOSTNAME/o:emile.skos/sdef:SKOS/getConceptRelatives?uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170000&relation=narrower","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
              
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.skos");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:skos","//xhtml:p[contains(@class,'dc:title')]" );
       
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:skos?method=get","//xhtml:p[contains(@class,'dc:title')]" );
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos?method=getpdf");
           
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptbyuri&uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170010");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptbyuri&uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170010","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptbypreflabel&preflabel=Afrikanisch");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptbypreflabel&preflabel=Afrikanisch","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );

         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptbyexternalid&externalid=");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptbyexternalid&externalid=","rdf:RDF" );
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptrelatives&uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170000&relation=narrower");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:skos?method=getconceptrelatives&uri=http://gams.uni-graz.at/skos/scheme/o:pth/%23170000&relation=narrower","//skos:Concept/skos:prefLabel[@xml:lang='deu' and .= 'Afrikanisch']" );
         */
     }
     
     @Test
     public void TestSpectralDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing Spectral disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1/METS_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.1/METS_SOURCE","//mets:div[@LABEL='REFERENCE.IMAGE']" );
    
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.ingest.spectral.1/datastreams/METS_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.ingest.spectral.1/datastreams/METS_SOURCE/content","//mets:div[@LABEL='REFERENCE.IMAGE']" );
            
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4/METS_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.4/METS_SOURCE","//mets:div[@LABEL='REFERENCE.IMAGE']" );
    
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.ingest.spectral.4/datastreams/METS_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.ingest.spectral.4/datastreams/METS_SOURCE/content","//mets:div[@LABEL='REFERENCE.IMAGE']" );
            
         // --- Legacy Layer Requests
             
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1/sdef:Object/getDC");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4/sdef:Object/getDC");

         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4");
         
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.1","//xhtml:h3[contains(., 'K 4984, Folio 1r')]");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.1/sdef:Spectral/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.1/sdef:Spectral/get","//xhtml:h3[contains(., 'K 4984, Folio 1r')]" );
              
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.4","//xhtml:h3[contains(., 'K 4984, Folio 1r')]");
         claim.assertStatus("https://HOSTNAME/o:emile.ingest.spectral.4/sdef:Spectral/get");
         claim.assertXML("https://HOSTNAME/o:emile.ingest.spectral.4/sdef:Spectral/get","//xhtml:h3[contains(., 'K 4984, Folio 1r')]" );
             
     }           
     
     @Test
     public void TestStoryJSDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing StoryJS disseminators");

         
         claim.assertStatus("https://HOSTNAME/o:emile.story/STORY");
         claim.assertXML("https://HOSTNAME/o:emile.story/STORY","//pid" );
         
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.story/datastreams/STORY/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.story/datastreams/STORY/content","//pid" );
         
         // --- Legacy Layer Requests
         
         claim.assertStatus("https://HOSTNAME/o:emile.story/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.story/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.story");
         claim.assertXML("https://HOSTNAME/o:emile.story","//xhtml:meta[contains(@content,'StoryMapJS')]" );

         claim.assertStatus("https://HOSTNAME/o:emile.story/sdef:Story/get");
         claim.assertXML("https://HOSTNAME/o:emile.story/sdef:Story/get","//xhtml:meta[contains(@content,'StoryMapJS')]" );
         
         claim.assertStatus("https://HOSTNAME/o:emile.story/sdef:Story/getJSON");
         claim.assertJSON("https://HOSTNAME/o:emile.story/sdef:Story/getJSON", "{storymap: { pid: \"o:emile.story\"} }");

         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.story");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:story");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:story","//xhtml:meta[contains(@content,'StoryMapJS')]" );
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:story?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:story?method=get","//xhtml:meta[contains(@content,'StoryMapJS')]" );
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:story?method=getjson");
         claim.assertJSON("https://HOSTNAME/services/"+path+"/svc:story?method=getjson", "{storymap: { pid: \"o:emile.story\"} }");
         */
     }

     @Test
     public void TestTEIDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing TEI disseminators");
         
         claim.assertStatus("https://HOSTNAME/o:emile.tei/TEI_SOURCE");
         claim.assertXML("https://HOSTNAME/o:emile.tei/TEI_SOURCE","//tei:pb[@facs='fol_1r']" );
    
         claim.assertStatus("https://HOSTNAME/archive/objects/o:emile.tei/datastreams/TEI_SOURCE/content");
         claim.assertXML("https://HOSTNAME/archive/objects/o:emile.tei/datastreams/TEI_SOURCE/content","//tei:pb[@facs='fol_1r']" );
            
         // --- Legacy Layer Requests
             
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/get");
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:Object/getRDF");

         claim.assertStatus("https://HOSTNAME/o:emile.tei");
         claim.assertXML("https://HOSTNAME/o:emile.tei","//xhtml:a[@href='fol_1v']");
                  
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:TEI/get");
         claim.assertXML("https://HOSTNAME/o:emile.tei/sdef:TEI/get","//xhtml:a[@href='fol_1v']" );
              
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:TEI/getPDF") ;
         
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:TEI/getSource");
         claim.assertXML("https://HOSTNAME/o:emile.tei/sdef:TEI/getSource","//tei:pb[@facs='fol_1r']" );
    
         claim.assertStatus("https://HOSTNAME/o:emile.odt/sdef:TEI/getLaTeX") ;
         claim.assertString("https://HOSTNAME/o:emile.odt/sdef:TEI/getLaTeX", "Vokalien");
             
         claim.assertStatus("https://HOSTNAME/o:emile.odt/sdef:TEI/getLaTeXPDF") ;
     
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:TEI/getHSSF") ;
             
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:TEI/getDataMatrix") ;
             
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:TEI/getSnippet") ;
         claim.assertXML("https://HOSTNAME/o:emile.tei/sdef:TEI/getSnippet","/snippet" );
                  
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:BibTeX/get");
    
         claim.assertStatus("https://HOSTNAME/o:emile.tei/sdef:BibTeX/getPDF");
             
             
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("o:emile.tei");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:tei","//xhtml:a[@href='fol_1v']" );
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=get");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:tei?method=get","//xhtml:a[@href='fol_1v']" );
             
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=getpdf");
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=getsource");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:tei?method=getsource","//tei:pb[@facs='fol_1r']" );
            
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=getlatex");
         claim.assertString("https://HOSTNAME/services/"+path+"/svc:tei?method=getlatex", "Lorem ipsum");
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=getlatexpdf");
                               
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=gethssf");
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=getdatamatrix");
    
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:tei?method=getsnippet");
         claim.assertXML("https://HOSTNAME/services/"+path+"/svc:tei?method=getsnippet","/snippet" );         
         */                  
     }
           	
     @Test
     public void TestZIPDisseminators() throws ClientProtocolException, IOException, Exception {

         log.info("Testing ZIP disseminators");
         
         // --- Legacy Layer Requests
 
         claim.assertStatus("https://HOSTNAME/context:emile/sdef:ZIP/get") ;
       
         // --- APIX Requests
         /*
         String path = LegacySystem.getPath("context:emile");
         
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:zip");
 
         claim.assertStatus("https://HOSTNAME/services/"+path+"/svc:zip?method=get");
         */
     }    
 
     
 
     /**
     * The datastream validation during upload is configured by file container:apache/config/fitstests.xml
     * 
     * @throws ClientProtocolException
     * @throws IOException
     * @throws Exception
     */
    @Test 
     public void TestZZZFitsWebServiceDuringIngest() throws ClientProtocolException, IOException, Exception {
    	 
     	  log.info("Testing FITS Web Service");         
    	
          connector.stubCloneObject("o:prototype.tei", "o:emile.ingest.tei.4", group);  
        
          claim.assertStatus(HttpStatus.SC_OK, connector.stubAddDatastream("o:emile.ingest.tei.4", "XML_SOURCE", getStream("objects/fits/test.xml"), "text/xml", "WELLFORMED"));
          claim.assertStatus(HttpStatus.SC_OK, connector.stubAddDatastream("o:emile.ingest.tei.4", "XML_SOURCE", getStream("objects/tei/tei_source.xml"), "text/xml", "VALID"));

          claim.assertStatus(HttpStatus.SC_OK, connector.stubAddDatastream("o:emile.ingest.tei.4", "PDF_STREAM", getStream("objects/fits/pdf_stream.pdf"), "application/pdf", "WELLFORMED"));
          claim.assertStatus(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, connector.stubAddDatastream("o:emile.ingest.tei.4", "PDF_STREAM", getStream("objects/fits/pdf_stream.pdf"), "application/pdf", "VALID"));
          claim.assertStatus(HttpStatus.SC_OK, connector.stubAddDatastream("o:emile.ingest.tei.4", "PDF_STREAM", getStream("objects/fits/pdf-a_stream.pdf"), "application/pdf", "VALID"));
          claim.assertStatus(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, connector.stubAddDatastream("o:emile.ingest.tei.4", "PDF_STREAM", getStream("objects/fits/unknown_binary.pdf"), "application/pdf", "VALID"));

          claim.assertStatus(HttpStatus.SC_OK, connector.stubAddDatastream("o:emile.ingest.tei.4", "IMAGE", getStream("objects/fits/image.jpg"), "image/jpeg", "WELLFORMED"));
          claim.assertStatus(HttpStatus.SC_UNSUPPORTED_MEDIA_TYPE, connector.stubAddDatastream("o:emile.ingest.tei.4", "IMAGE", getStream("objects/fits/corrupted-image.jpg"), "image/jpeg", "WELLFORMED"));
          
    }      
      
    //@Test 
    public void TestZZZInfrastructureRelatedFunctions() throws ClientProtocolException, IOException, Exception {
   	 
     	log.info("Testing infrastructure-related functions");
    	 
	   	claim.assertStatus(HttpStatus.SC_OK, connector.stubOpenConnection("https", hostname, "c3po", passwd ));

	   	connector.stubResetSystemBusyFlag("chamois");
        claim.assertJSON("https://HOSTNAME/cm4f/health", "{\"status\":\"UP\"}");
      	connector.stubResetSystemBusyFlag();
    	
	   	claim.assertStatus(HttpStatus.SC_OK, connector.stubOpenConnection("https", hostname, username, passwd ));

      	connector.stubResetSystemBusyFlag("chamois");
        claim.assertJSON("https://HOSTNAME/cm4f/health", "{\"status\":\"BUSY\"}");
        connector.stubResetSystemBusyFlag();
        
        claim.assertJSON("https://HOSTNAME/cm4f/health", "{\"status\":\"UP\"}");
        connector.stubCheckHealth();
          	    	 
   /*
    *   Do not use this test on production servers. It would take too long.
    *
    *	claim.assertStatus(HttpStatus.SC_NOT_IMPLEMENTED, connector.stubReorganizeRepository());
    *   claim.assertStatus(HttpStatus.SC_OK, connector.stubReorganizeGraphDatabases());	
    *   
    *	claim.assertStatus(HttpStatus.SC_OK, connector.stubReorganizeGraphDatabase("triplestore"));	
    *   claim.assertStatus(HttpStatus.SC_OK, connector.stubReorganizeGraphDatabase("marmotta"));	
    *
    */
        
    }      
    
    @Test 
    public void TestZZZSOLRSearchEngine() throws ClientProtocolException, IOException, Exception {
   	 
      	log.info("Testing Solr search engine");

        claim.assertXML("https://HOSTNAME/solr/fcrepo/select?indent=on&q=*:*&rows=100&wt=xml","//str[@name='cm4f.hasModel' and contains(.,'http://cm4f.org')]");
        claim.assertXML("https://HOSTNAME/solr/fcrepo/select?indent=on&q=cm4f.owner:test&rows=100&wt=xml","//str[@name='cm4f.hasModel' and contains(.,'http://cm4f.org')]");
        claim.assertXML("https://HOSTNAME/solr/fcrepo/select?indent=on&q=dc.identifier:*emile*&rows=100&wt=xml","//str[@name='cm4f.hasModel' and contains(.,'http://cm4f.org')]");
        claim.assertXML("https://HOSTNAME/solr/fcrepo/select?indent=on&q=*:*&rows=100&wt=xml","//str[@name='cm4f.hasModel' and contains(.,'http://cm4f.org')]");
        claim.assertXML("https://HOSTNAME/solr/fcrepo/select?indent=on&q=dc.rights:http*&rows=100&wt=xml","//str[@name='cm4f.hasModel' and contains(.,'http://cm4f.org')]");

    }
    
     /**
     * 
     * The OAI Harvester pipline is configured by file container:apache/config/oaiproviders.xml
     * 
     * Remark: This test is only executed in log level debug or when running tests against localhost
     *         Running test against it030021.uni-graz.at simply takes too long.
     *         
     * @throws ClientProtocolException
     * @throws IOException
     * @throws Exception
     */
    @Test 
    public void TestZZZOAIHarvester() throws ClientProtocolException, IOException, Exception {
    	 
     	 if (log.isDebugEnabled() || hostname.equals("localhost")) {
         
             log.info("Testing OAI Harvester");         
            
          	 connector.stubTriggerOAIHarvesterPipeline();
          	 
           	 claim.assertXML("https://localhost/oai-pmh/?verb=GetRecord&identifier=hdl:11471/1001&metadataPrefix=oai_dc", "//oai_dc:dc[dc:type='CompoundObject' and dc:identifier='o:emile.tei']");
           	 claim.assertXML("https://localhost/oai-pmh/?verb=GetRecord&identifier=hdl:11471/1002&metadataPrefix=oai_dc", "//oai_dc:dc[dc:type='CompoundObject' and dc:identifier='o:emile.bibtex']");
          	 claim.assertXML("https://localhost/oai-pmh/?verb=GetRecord&identifier=hdl:11471/1004&metadataPrefix=oai_dc", "//oai_dc:dc[dc:type='CompoundObject' and dc:identifier='o:emile.latex']");
          	 claim.assertXML("https://localhost/oai-pmh/?verb=GetRecord&identifier=hdl:11471/1005&metadataPrefix=oai_dc", "//oai_dc:dc[dc:type='CompoundObject' and dc:identifier='o:emile.ontology']");
           	 claim.assertXML("https://localhost/oai-pmh/?verb=GetRecord&identifier=hdl:11471/1007&metadataPrefix=oai_dc", "//oai_dc:dc[dc:type='CompoundObject' and dc:identifier='o:emile.odt']");
 
     	 }
         
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
      
     private static boolean check(String header, String cmd, String text) {
    	 
     	 HttpResponse response = null;
         Integer statuscode = new Integer(400);    	 
         int repeats = 0; 
         String service = cmd.replaceAll("HOSTNAME", hostname);
            
         try { 
        	 if (header != null) log.info("Checking "+header+" services");
       	    
       		 HttpUriRequest request = RequestBuilder
			         .get(service)
			         .build();    
        		 
       		 while (true) {  
    	   		try (CloseableHttpClient httpclient = SSLConnectionFactory.createTrustSelfSigned()) {
     	   			response = httpclient.execute(request);
    	   			statuscode = new Integer(response.getStatusLine().getStatusCode());
    	   			
    	   			if (text != null) {
    	   				ByteArrayOutputStream bos = new ByteArrayOutputStream(); 
    	   				response.getEntity().writeTo(bos);
    	   				if (new String(bos.toByteArray()).contains(text)) break;
    	   				bos.close();
    	   				continue;
    	   			}      	    	  
   	    			if (statuscode == 200 || repeats > MAXREPEATS) break;
   	    			repeats++;
   	    			Thread.sleep(2000);	   	
   	    		}
       		 }	
    	      	
             if (repeats > MAXREPEATS) notreadyError();
            
       		 
	     } catch (Exception e)  {
	    	 log.error(e.getMessage());
	     }
         
    	 return true;
    	
      } 
     
     
      private static void notreadyError() {
     	 log.error("The CM4F Framework is not ready to work. The integration test cannot be executed. Check the availability of the Docker containers in your framework.");
     	 System.exit(500);	  
      }
     
}    
