/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.test.ce.testsuite.cluster.http.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class ResponseFilter implements Filter {
    private static final Logger log = Logger.getLogger(ResponseFilter.class.getName());

    private static final String KEY = "__attrib";

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest hsrq = (HttpServletRequest) request;
        HttpServletResponse hsrp = (HttpServletResponse) response;

        HttpSession session = hsrq.getSession();
        log.info("SessionID: " + session.getId());

        Object attrib = session.getAttribute(KEY);
        if (attrib != null) {
            hsrp.getWriter().write(String.valueOf(attrib));
        } else {
            session.setAttribute(KEY, "CE!!");
            hsrp.getWriter().write("OK");
        }
    }

    public void destroy() {
    }

    public static String readInputStream(InputStream is) throws Exception {
        try {
            String content = "";
            int ch;
            while ((ch = is.read()) != -1) {
                content += ((char) ch);
            }
            return content;
        } finally {
            is.close();
        }
    }
}
