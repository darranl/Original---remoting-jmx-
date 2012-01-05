/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
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

package org.jboss.remoting3.jmx;

import java.io.IOException;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.jboss.remoting3.jmx.common.JMXRemotingServer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 * The base class for the remote tests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public abstract class AbstractTestBase {

    private static final int PORT = 12345;

    protected static final String URL = "service:jmx:remoting://localhost:" + PORT;
    protected static final String DEFAULT_DOMAIN = "org.jboss.remoting3.jmx";

    private static JMXRemotingServer remotingServer;
    protected static MBeanServer mbeanServer;
    private static JMXServiceURL serviceURL;

    protected JMXConnector connector;

    @BeforeClass
    public static void setupServer() throws IOException {
        mbeanServer = MBeanServerFactory.createMBeanServer(DEFAULT_DOMAIN);

        remotingServer = new JMXRemotingServer(PORT, mbeanServer);
        remotingServer.start();
        serviceURL = new JMXServiceURL(URL);
    }

    @AfterClass
    public static void tearDownServer() throws IOException {
        try {
            remotingServer.stop();
        } finally {
            remotingServer = null;
        }

    }

    @Before
    public void connect() throws IOException {
        connector = JMXConnectorFactory.connect(serviceURL);
    }

    @After
    public void disconnect() throws IOException {
        try {
            connector.close();
        } finally {
            connector = null;
        }
    }

}
