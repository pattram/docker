package org.emile.cirilo.utils;

import static org.hamcrest.CoreMatchers.not;
import static org.xmlunit.assertj.XmlAssert.assertThat;

import java.io.IOException;
import java.util.HashMap;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jdom.Namespace;
import org.jsoup.Jsoup;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.ArraySizeComparator;

public class Claim {
    
	private String hostname;
	
	private final static HashMap<String, String> namespaces = new HashMap<String, String>();
    static {
        
        namespaces.put("skos", "http://www.w3.org/2004/02/skos/core#");
        namespaces.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        namespaces.put("xhtml", "http://www.w3.org/1999/xhtml");
        namespaces.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        namespaces.put("mets", "http://www.loc.gov/METS/");
        namespaces.put("tei", "http://www.tei-c.org/ns/1.0");
        namespaces.put("g2o", "http://gams.uni-graz.at/ontology/#");
        namespaces.put("hssf", "urn:schemas-microsoft-com:office:spreadsheet");
        namespaces.put("cocoon", "http://apache.org/cocoon/request/2.0");
        namespaces.put("bibtex", "http://bibtexml.sf.net/");
        namespaces.put("mods", "http://www.loc.gov/mods/v3");
        namespaces.put("sparql", "http://www.w3.org/2001/sw/DataAccess/rf1/result");
        namespaces.put("s", "http://www.w3.org/2005/sparql-results#");
        namespaces.put("dc", "http://purl.org/dc/elements/1.1/");
        namespaces.put("oai_dc", "http://www.openarchives.org/OAI/2.0/oai_dc/");
        namespaces.put("lido", "http://www.lido-schema.org");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");
        namespaces.put("cmd", "http://www.clarin.eu/cmd/1");
        namespaces.put("tcf", "http://www.dspin.de/data/textcorpus");      
        namespaces.put("oai", "http://www.openarchives.org/OAI/2.0/");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("geo", "http://www.opengis.net/ont/geosparql#");
        namespaces.put("edm", "http://www.europeana.eu/schemas/edm/");
        namespaces.put("pelagios", "http://pelagios.github.io/vocab/terms#");
        namespaces.put("fits", "http://hul.harvard.edu/ois/xml/ns/fits/fits_output");
        namespaces.put("mei", "http://www.music-encoding.org/ns/mei");
        namespaces.put("svg", "http://www.w3.org/2000/svg");
        namespaces.put("tc", "http://www.dspin.de/data/textcorpus");
        namespaces.put("x", "adobe:ns:meta/");
        namespaces.put("exif", "http://ns.adobe.com/exif/1.0/");
        
    }

    public Claim (String hostname) {
    	this.hostname = hostname;
    }
    
    public void addNamespace(String prefix, String uri) {
    	namespaces.put(prefix, uri);
    }
    
    public void assertXML (String url, String xpath) throws IOException, Exception {
        
        String body = SSLConnectionFactory.createTrustSelfSigned().execute(new HttpGet( resolveServiceURL(url)), new DefaultResponseHandler());        
        assertThat(body).withNamespaceContext(namespaces).hasXPath(xpath); 
    }

   public void assertXML (byte[] stream, String xpath) throws IOException, Exception {
        
        String body = new String (stream);       
        assertThat(body).withNamespaceContext(namespaces).hasXPath(xpath); 
    }

    public  void assertJSON (String url, String json) throws IOException, Exception {
        
        String body = SSLConnectionFactory.createTrustSelfSigned().execute(new HttpGet( resolveServiceURL(url)), new DefaultResponseHandler());        
        JSONAssert.assertEquals(json, body, false); 
    }

    public void assertJSONArray (String url, String json) throws IOException, Exception {
        
        String body = SSLConnectionFactory.createTrustSelfSigned().execute(new HttpGet( resolveServiceURL(url)), new DefaultResponseHandler());        
        JSONAssert.assertEquals(json, body, new ArraySizeComparator(JSONCompareMode.LENIENT)); 
    }
      
    public void assertHTML (String url, String matcher) throws IOException, Exception {
        
        String body = SSLConnectionFactory.createTrustSelfSigned().execute(new HttpGet( resolveServiceURL(url)), new DefaultResponseHandler()); 
        org.junit.Assert.assertThat((Object)Jsoup.parse(body).select(matcher).size(), not(0));
    }
        
    public void assertStatus (String url) throws IOException, Exception {
        
        HttpResponse response = execute(url);
        Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
    }
    
    public void assertStatus (int httpstatus, String url) throws IOException, Exception {
        
        HttpResponse response = execute(url);
        Assert.assertEquals(httpstatus, response.getStatusLine().getStatusCode());
    }
    
    public void assertStatus (int httpstatus, int requeststatus) throws IOException, Exception {
        
        Assert.assertEquals(httpstatus, requeststatus);
    }
     
    public void assertString (String url, String text) throws IOException, Exception {
        
        String body = SSLConnectionFactory.createTrustSelfSigned().execute(new HttpGet( resolveServiceURL(url)), new DefaultResponseHandler()); 
        Assert.assertTrue(body.contains(text));
    }
    
         
    public HttpResponse execute(String url) throws Exception {
        
        CloseableHttpClient http = SSLConnectionFactory.createTrustSelfSigned();
        HttpResponse response = http.execute( new HttpGet( resolveServiceURL(url) ) );
        http.close();
        return response;
    }
     
    private String resolveServiceURL(String s) {
       return s.replaceAll("HOSTNAME", hostname);
    }
}
