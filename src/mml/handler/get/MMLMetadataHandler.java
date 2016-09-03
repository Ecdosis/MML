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

package mml.handler.get;

import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.Utils;
import mml.constants.Params;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import org.json.simple.*;
import java.util.Set;
import java.util.Iterator;

/**
 * Get metadata about a document
 * @author desmond
 */
public class MMLMetadataHandler extends MMLGetHandler 
{
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException {
        try
        {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            if ( docid == null || docid.length()== 0 )
                docid = urn;
            JSONObject md = new JSONObject();
            boolean changed = false;
            String docId = docid;
            do
            {
                String jStr = conn.getMetadata( docId );
                if ( jStr != null )
                {
                    JSONObject jObj = (JSONObject)JSONValue.parse(jStr);
                    Set<String> keys = jObj.keySet();
                    Iterator<String> iter = keys.iterator();
                    while ( iter.hasNext() )
                    {
                        String key = iter.next();
                        md.put( key, jObj.get(key) );
                    }
                    changed = true;
                }
                else
                {
                    System.out.println("No metadata found for "+docId);
                    changed = false;
                }
                docId = Utils.chomp(docId);
            } while ( changed );
            response.setContentType("application/json");
            response.setCharacterEncoding(encoding);
            response.getWriter().println(md.toJSONString());
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
