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
 *  (c) copyright Desmond Schmidt 2015
 */
package mml;
import calliope.core.database.Connector;
import calliope.core.database.Repository;
import java.util.Enumeration;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextListener;
import static mml.MMLWebApp.dbPort;
import static mml.MMLWebApp.webRoot;
 
public class MMLContextListener implements ServletContextListener
{
    /**
     * Safely convert a string to an integer
     * @param value the value probably an integer
     * @param def the default if it is not
     * @return the value or the default
     */
    private int getInteger( String value, int def )
    {
        int res = def;
        try
        {
            res = Integer.parseInt(value);
        }
        catch ( NumberFormatException e )
        {
        }
        return res;
    }
    /**
     * Safely convert a string to a Repository enum
     * @param value the value probably a repo type
     * @param def the default if it is not
     * @return the value or the default
     */
    private Repository getRepository( String value, Repository def )
    {
        Repository res = def;
        try
        {
            res = Repository.valueOf(value);
        }
        catch ( IllegalArgumentException e )
        {
        }
        return res;
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent arg0) 
    {
        System.out.println("ServletContextListener destroyed");
    }

    //Run this before web application is started
    @Override
    public void contextInitialized(ServletContextEvent event) 
    {
        try
        {
            ServletContext context = event.getServletContext();
            Enumeration params = context.getInitParameterNames();
            while (params.hasMoreElements()) 
            {
                String param = (String) params.nextElement();
                String value = 
                    context.getInitParameter(param);
                if ( param.equals("webRoot") )
                {
                    webRoot = value;
                }
                else if ( param.equals("dbPort") )
                    dbPort = getInteger(value,27017);
                else if (param.equals("wsPort"))
                    MMLWebApp.wsPort= getInteger(value,8080);
                else if ( param.equals("username") )
                    MMLWebApp.user = value;
                else if ( param.equals("password") )
                    MMLWebApp.password = value;
                else if ( param.equals("repository") )
                    MMLWebApp.repository = getRepository(value,Repository.MONGO);
                else if ( param.equals("host") )
                    MMLWebApp.host = value;
            }
            Connector.init( MMLWebApp.repository, MMLWebApp.user, 
                MMLWebApp.password, MMLWebApp.host, "calliope", 
                MMLWebApp.dbPort, MMLWebApp.wsPort, MMLWebApp.webRoot );
        }
        catch ( Exception e )
        {
            e.printStackTrace(System.out);
        }
    }
}