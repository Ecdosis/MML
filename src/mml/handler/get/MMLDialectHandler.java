/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.constants.Database;
import mml.constants.JSONKeys;
import mml.constants.Params;
import mml.database.Connection;
import mml.database.Connector;
import mml.exception.JSONException;
import mml.exception.MMLDbException;
import mml.exception.MMLException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * @author desmond
 */
public class MMLDialectHandler extends MMLGetHandler
{
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
            String jStr = Connector.getConnection().getFromDb(
                Database.DIALECTS,urn);
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
