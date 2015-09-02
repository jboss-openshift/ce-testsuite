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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class FooServlet extends HttpServlet {
    private static final String KEY = "__attrib";

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession();
        log("SessionID: " + session.getId());

        Object attrib = session.getAttribute(KEY);
        if (attrib != null) {
            resp.getWriter().write(String.valueOf(attrib));
        } else {
            session.setAttribute(KEY, "CE!!");
            resp.getWriter().write("OK");
        }
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