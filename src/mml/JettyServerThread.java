/*
 * This file is part of MML.
 *
 *  MML is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  MML is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MML.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */
package mml;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Connector;
/**
 * This launches an instance of the Jetty service
 * @author desmond
 */
public class JettyServerThread extends Thread 
{
    /**
     * Run the server
     */
    public void run()
    {
        try
        {
            Server server = new Server(MMLWebApp.wsPort);
            Connector[] connectors = server.getConnectors();
            connectors[0].setHost(MMLWebApp.host);
            // initialise autosave by forcing class to load
            Autosave.lock = false;
            Autosave.inProgress = false;
            server.setHandler(new JettyServer());
            System.out.println("starting...");
            server.start();
            server.join();
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
        }
    }
}
