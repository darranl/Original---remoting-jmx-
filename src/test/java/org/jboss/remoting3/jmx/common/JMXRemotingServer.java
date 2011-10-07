/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.remoting3.jmx.common;

import javax.management.MBeanServer;
import javax.management.remote.JMXConnectorServer;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.Security;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.OpenListener;
import org.jboss.remoting3.Registration;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.jmx.RemotingConnectorServer;
import org.jboss.remoting3.jmx.Version;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.remoting3.security.SimpleServerAuthenticationProvider;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.jboss.sasl.JBossSaslProvider;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Sequence;
import org.xnio.Xnio;
import org.xnio.channels.AcceptingChannel;
import org.xnio.channels.ConnectedStreamChannel;

/**
 * A test server to test exposing the local MBeanServer using Remoting.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class JMXRemotingServer {

    private static final Logger log = Logger.getLogger(JMXRemotingServer.class);

    static {
        Security.insertProviderAt(new JBossSaslProvider(), 1);
    }

    private final int listenerPort;


    private Endpoint endpoint;
    // TODO - This may not live here - maybe in the RemotingConnectorServer
    private AcceptingChannel<? extends ConnectedStreamChannel> server;
    private JMXConnectorServer connectorServer;

    /**
     * Constructor to instantiate a JMXRemotingServer with a
     * specified listener port.
     *
     * @param port
     */
    public JMXRemotingServer(final int port) {
        this.listenerPort = port;
    }

    public void start() throws IOException {
        log.infof("Starting JMX Remoting Server %s", Version.getVersionString());


        // Initialise general Remoting - this step would be implemented elsewhere when
        // running within an application server.
        final ExecutorService es = Executors.newFixedThreadPool(5);
        endpoint = Remoting.createEndpoint("JMXRemoting", es, OptionMap.EMPTY);
        final Xnio xnio = Xnio.getInstance();
        endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(xnio), OptionMap.create(Options.SSL_ENABLED, false));

        final NetworkServerProvider nsp = endpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        final SocketAddress bindAddress = new InetSocketAddress(InetAddress.getLocalHost(), listenerPort);
        final SimpleServerAuthenticationProvider authenticationProvider = new SimpleServerAuthenticationProvider();
        final OptionMap serverOptions = OptionMap.create(Options.SASL_MECHANISMS, Sequence.of("ANONYMOUS"), Options.SASL_POLICY_NOANONYMOUS, Boolean.FALSE);

        server = nsp.createServer(bindAddress, serverOptions, authenticationProvider);

        // Initialise the components that will provide JMX connectivity.

        // This is the MBeanServer client requests will be delegated to.
        // TODO - Create a dummy impl for the purpose of testing.
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        // TODO - Create Service to wrap the MBeanServer
        connectorServer = new RemotingConnectorServer(mbeanServer, endpoint);
        connectorServer.start();
    }

    public void stop() throws IOException {
        log.infof("Stopping JMX Remoting Server %s", Version.getVersionString());

        // Services using an existing Remoting installation only need to stop the JMXConnectorServer
        // to disassociate it from Remoting.
        connectorServer.stop();

        // TODO - Also tear down the remoting portion as that was specific to the test case.

    }

}
