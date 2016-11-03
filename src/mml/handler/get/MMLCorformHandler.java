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
import calliope.core.constants.Database;
import calliope.core.database.Connection;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;

/**
 * Fetch a corform resource
 * @author desmond
 */
public class MMLCorformHandler extends MMLResourceHandler
{
    public MMLCorformHandler()
    {
        super(Database.CORFORM);
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
            String jBody = null;
            docid = request.getParameter(JSONKeys.DOCID);
            if ( docid != null )
            {
                Connection conn = Connector.getConnection();
                String jStr = conn.getFromDb( Database.CORTEX, docid );
                JSONObject jObj = (JSONObject)JSONValue.parse(jStr);
                String style = (String) jObj.get(JSONKeys.STYLE);
                jStr = conn.getFromDb(Database.CORFORM,style);
                if ( jStr == null )
                    jBody = getDefaultResource(docid);
                else
                {
                    jObj = (JSONObject)JSONValue.parse(jStr);
                    jBody = (String)jObj.get(JSONKeys.BODY);
                }
            }
            else
                jBody = getDefaultResource("default");
            setEncoding(request);
            response.setContentType("text/plain");
            response.setCharacterEncoding(encoding);
            response.getWriter().println(jBody);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }

}
