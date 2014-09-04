/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.Utils;
import mml.constants.JSONKeys;
import mml.constants.Database;
import mml.constants.Params;
import mml.database.Connection;
import mml.database.Connector;
import mml.exception.MMLDbException;
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
            Connection conn = Connector.getConnection();
            String original = new String(urn);
            String jStr = null;
            do
            {
                jStr = Connector.getConnection().getFromDb(
                    database,urn);
                if ( jStr == null )
                {
                    if ( this.database.equals(Database.CORFORM) )
                    {
                        String last = Utils.last(urn);
                        if ( last.equals("default") )
                        {
                            urn = Utils.chomp(urn);
                            if ( !urn.equals("/") )
                                urn = Utils.chomp(urn)+"/"+last;
                            else
                                break;
                        }
                        else
                            urn = Utils.chomp(urn);
                    }
                    else
                        break;
                }
            }
            while ( jStr == null );
            if ( jStr == null )
                throw new MMLDbException("Failed to find "+original);
            String newEncoding = request.getParameter(Params.ENCODING);
            if ( newEncoding != null && newEncoding.length()>0 )
                this.encoding = encoding;
            String bodyStr="";
            if ( jStr != null )
            {
                JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
                bodyStr = (String)jDoc.get(JSONKeys.BODY);
            }
            else
                throw new MMLDbException("body key not found");
            response.setCharacterEncoding(encoding);
            response.getWriter().println(bodyStr);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
