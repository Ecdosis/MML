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

import calliope.core.exception.CalliopeException;
import calliope.core.exception.CalliopeExceptionMessage;
import calliope.core.Utils;
import calliope.core.database.Connector;
import calliope.core.database.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Enumeration;
import mml.handler.MMLPutHandler;
import mml.handler.MMLHandler;
import mml.handler.MMLDeleteHandler;
import mml.handler.get.MMLGetHandler;
import mml.handler.post.MMLPostHandler;
import mml.exception.MMLException;

/**
 *
 * @author desmond
 */
public class MMLWebApp extends HttpServlet
{
    static String host = "localhost";
    static String user ="admin";
    static String password = "jabberw0cky";
    static int dbPort = 27017;
    public static int wsPort = 8080;
    public static String webRoot = "/var/www/";
    static Repository repository = Repository.MONGO;
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, java.io.IOException
    {
        try
        {
            String method = req.getMethod();
            String target = req.getRequestURI();
            target = Utils.pop( target );
            MMLHandler handler;
            if ( method.equals("GET") )
                handler = new MMLGetHandler();
            else if ( method.equals("PUT") )
                handler = new MMLPutHandler();
            else if ( method.equals("DELETE") )
                handler = new MMLDeleteHandler();
            else if ( method.equals("POST") )
                handler = new MMLPostHandler();
            else
                throw new MMLException("Unknown http method "+method);
            resp.setStatus(HttpServletResponse.SC_OK);
            handler.handle( req, resp, target );
        }
        catch ( Exception e )
        {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            CalliopeException he = new CalliopeException( e );
            resp.setContentType("text/html");
            try 
            {
                resp.getWriter().println(
                    new CalliopeExceptionMessage(he).toString() );
            }
            catch ( Exception e2 )
            {
                e.printStackTrace( System.out );
            }
        }
    }
}