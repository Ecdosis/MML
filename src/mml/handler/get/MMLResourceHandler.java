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
 *  (c) copyright Desmond Schmidt 2016
 */

package mml.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.Utils;
import calliope.core.constants.JSONKeys;
import calliope.core.constants.Database;
import mml.constants.Params;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.exception.DbException;
import mml.exception.MMLException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Get a general resource
 * @author desmond
 */
public class MMLResourceHandler extends MMLGetHandler
{
    String database;
    public MMLResourceHandler( String database )
    {
        this.database = database;
    }
    String getDefaultResource( String urn ) 
        throws DbException
    {
        Connection conn = Connector.getConnection();
        String original = new String(urn);
        String jStr = null;
        do
        {
            jStr = conn.getFromDb(database,urn);
            if ( jStr == null )
            {
                if ( this.database.equals(Database.CORFORM) )
                {
                    jStr = conn.getFromDb(database,urn+"/default");
                    if ( jStr == null )
                        urn = Utils.chomp(urn);
                    else
                        break;
                }
                else
                    break;
            }
        }
        while ( jStr == null );
        if ( jStr == null )
            throw new DbException("Failed to find "+original);
        String bodyStr="";
        if ( jStr != null )
        {
            JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
            bodyStr = (String)jDoc.get(JSONKeys.BODY);
        }
        else
            throw new DbException("body key not found");
        return bodyStr;
    }
    void setEncoding( HttpServletRequest request )
    {
        String newEncoding = request.getParameter(Params.ENCODING);
        if ( newEncoding != null && newEncoding.length()>0 )
            this.encoding = newEncoding;
        else
            encoding = "UTF-8";
    }
    /**
     * Handle the request
     * @param request the request
     * @param response the response
     * @param urn the remaining urn of the request
     * @throws MMLException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException 
    {
        try
        {
            String bodyStr = getDefaultResource(urn);
            setEncoding(request);
            response.setContentType("text/plain");
            response.setCharacterEncoding(encoding);
            response.getWriter().println(bodyStr);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
