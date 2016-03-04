/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.test.arquillian.ce.sso.support;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

public class Client {
	
    String basicUrl;

    public Client(String basicUrl) {
        this.basicUrl = basicUrl;
        
        System.out.println("!!!!!!!!!!!!! basicUrl " + basicUrl);
    }
    
    public String get(String key) {
    	return get(key, null);
    }
    
    public String get(String key, List<NameValuePair> headers) {
        try {
        	
        	HttpClient client = createHttpClient_AcceptsUntrustedCerts();
        	HttpGet request = new HttpGet(basicUrl + "/" + key);
        	
        	System.out.println("!!!!!!!!!!!!! url " + basicUrl + "/" + key);
        	
        	if (headers != null){
        		for (NameValuePair header : headers) 
        			request.addHeader(header.getName(), header.getValue());
        	}
            
            HttpResponse response = client.execute(request);

            System.out.println("Response Code : " 
                    + response.getStatusLine().getStatusCode());

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
            	result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
    
    public String post(String key, List<NameValuePair> params) {
        try {
        	
        	HttpClient client = createHttpClient_AcceptsUntrustedCerts();
        	
        	URIBuilder builder = new URIBuilder(basicUrl + "/" + key);
  
        	HttpPost request = new HttpPost(builder.build());
        	
        	UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params, Consts.UTF_8);
        	request.setEntity(entity);
        	
        	System.out.println("!!!!!!!!!!!!! request " + request.getURI());
             
            HttpResponse response = client.execute(request);
            
            int statusCode = response.getStatusLine().getStatusCode();
            
            System.out.println("Response Code : " + statusCode);
            
            if (statusCode == 302 ) {
            	Header[] location = response.getHeaders("Location");
            	for (Header header : location){
            		System.out.println("!!!! header " + header);
            		return header.getValue();
            	}
            }         

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

            StringBuilder result = new StringBuilder();
            String line = "";
            while ((line = rd.readLine()) != null) {
            	result.append(line);
            }
            return result.toString();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
    
    public HttpClient createHttpClient_AcceptsUntrustedCerts() throws Exception {
        HttpClientBuilder b = HttpClientBuilder.create();
     
        // setup a Trust Strategy that allows all certificates.
        //
        SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
            public boolean isTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                return true;
            }
        }).build();
        b.setSslcontext( sslContext);
     
        // don't check Hostnames, either.
        //      -- use SSLConnectionSocketFactory.getDefaultHostnameVerifier(), if you don't want to weaken
        X509HostnameVerifier allowAllHostnameVerifier = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

        // here's the special part:
        //      -- need to create an SSL Socket Factory, to use our weakened "trust strategy";
        //      -- and create a Registry, to register it.
        //
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext, allowAllHostnameVerifier);
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();
     
        // now, we create connection-manager using our Registry.
        //      -- allows multi-threaded use
        PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager( socketFactoryRegistry);
        b.setConnectionManager( connMgr);
     
        // finally, build the HttpClient;
        //      -- done!
        return b.build();
    }
    
}