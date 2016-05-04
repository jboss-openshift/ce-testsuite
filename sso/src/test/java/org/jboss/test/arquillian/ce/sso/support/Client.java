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

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;

import org.jboss.arquillian.ce.api.OpenShiftHandle;

import com.squareup.okhttp.Response;


public class Client {
	
	protected final Logger log = Logger.getLogger(getClass().getName());
	
	protected Map<String, String> params;
    protected String basicUrl;
    protected HttpClient client;
    protected CookieStore cookieStore = new BasicCookieStore();

    public Client(String basicUrl) throws Exception {
    	this.basicUrl = trimPort(basicUrl);
        
        client = createHttpClient_AcceptsUntrustedCerts(cookieStore);
    }
    
    public static String trimPort(String url){
    	if (url.contains(":443"))
    		url = url.replace(":443", "");
    	else if (url.contains(":80"))
    		url = url.replace(":80", "");
    	
        return url;
    }
    
    public void setParams(Map<String, String> params){
    	this.params = params;
    }
    
    public void setBasicUrl(String basicUrl){
    	this.basicUrl = basicUrl;
    }
    
    public String get() {
    	return get(null, null);
    }
    
    public String get(String key) {
    	return get(key, null);
    }
    
    public CookieStore getCookieStore(){
    	return cookieStore;
    }

    public String get(String key, Map<String, String> headers) {
        try {
        	String url = basicUrl;
        	if (key != null){
	        	url = basicUrl + "/" + key;
	        	if (basicUrl.endsWith("/"))
	        		url = basicUrl + key;
        	}
        	
            HttpRequest request = HttpClientBuilder.doGET(url);

        	if (headers != null){
                for (Map.Entry<String, String> header : headers.entrySet())
                    request.setHeader(header.getKey(), header.getValue());
            }
            
            HttpResponse response = client.execute(request);
            
            int statusCode = response.getResponseCode();
            log.warning("Response Code : " + statusCode);
            
            return response.getResponseBodyAsString();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
    
    public String post(){
    	return post(null);
    }

    public String post(String key) {
        try {
        	String url = basicUrl;
        	if (key != null){
        		url = basicUrl + "/" + key;
        		if (basicUrl.endsWith("/"))
        			url = basicUrl + key;
    		}

            HttpRequest request = HttpClientBuilder.doPOST(url);
            
            if (params != null)
            	request.setEntity(params);

            HttpResponse response = client.execute(request);

            int statusCode = response.getResponseCode();
            
            log.warning("Response Code : " + statusCode);
            
            if (statusCode == 302 ) {
                String location = response.getHeader("Location");
                if (location != null) {
                    return location;
                }
            }

            return response.getResponseBodyAsString();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }
    
    public HttpClient createHttpClient_AcceptsUntrustedCerts(CookieStore cookieStore) throws Exception {
        return HttpClientBuilder.create().untrustedConnectionClientBuilder().setCookieStore(cookieStore).build();
    }
    
}