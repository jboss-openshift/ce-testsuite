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

import java.util.Map;

import org.jboss.arquillian.ce.httpclient.HttpClient;
import org.jboss.arquillian.ce.httpclient.HttpClientBuilder;
import org.jboss.arquillian.ce.httpclient.HttpRequest;
import org.jboss.arquillian.ce.httpclient.HttpResponse;


public class Client {
	
    String basicUrl;

    public Client(String basicUrl) {
        this.basicUrl = basicUrl;
    }
    
    public String get(String key) {
    	return get(key, null);
    }

    public String get(String key, Map<String, String> headers) {
        try {
        	HttpClient client = createHttpClient_AcceptsUntrustedCerts();
        	
            HttpRequest request = HttpClientBuilder.doGET(basicUrl + "/" + key);

        	if (headers != null){
                for (Map.Entry<String, String> header : headers.entrySet())
                    request.setHeader(header.getKey(), header.getValue());
            }
            
            HttpResponse response = client.execute(request);
            return response.getResponseBodyAsString();
        } catch (Exception e) {
        	e.printStackTrace();
            throw new RuntimeException(e);
        } 
    }

    public String post(String key, Map<String, String> params) {
        try {
        	
        	HttpClient client = createHttpClient_AcceptsUntrustedCerts();

            HttpRequest request = HttpClientBuilder.doPOST(basicUrl + "/" + key);

            request.setEntity(params);

            HttpResponse response = client.execute(request);

            int statusCode = response.getResponseCode();
            
            System.out.println("Response Code : " + statusCode);
            
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
    
    public HttpClient createHttpClient_AcceptsUntrustedCerts() throws Exception {
        return HttpClientBuilder.untrustedConnectionClient();
    }
    
}