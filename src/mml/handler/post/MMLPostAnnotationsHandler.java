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
package mml.handler.post;

import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;
import java.util.HashMap;
import java.util.UUID;

/**
 * Handle posting of annotations to scratch database only
 * @author desmond
 */
public class MMLPostAnnotationsHandler extends MMLPostHandler
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            if (ServletFileUpload.isMultipartContent(request) )
            {
                parseImportParams( request );
                if ( docid != null && version1 != null && annotations != null )
                {
                    Connection conn = Connector.getConnection();
                    String[] docids = conn.listDocuments(Database.SCRATCH, 
                        docid+".*", JSONKeys.DOCID);
                    if ( docids != null && docids.length>0 )
                    {
                        HashMap<Integer,JSONObject> map = 
                            new HashMap<Integer,JSONObject>();
                        for ( int i=0;i<docids.length;i++ )
                        {
                            JSONObject jObj = fetchAnnotation(conn, 
                                Database.SCRATCH, docids[i] );
                            if ( jObj != null && jObj.containsKey(JSONKeys.ID) )
                            {
                                int key = ((Number)jObj.get(JSONKeys.ID)).intValue();
                                map.put( key, jObj );
                            }
                        }
                        // existing annotations are int eh map
                        // overwrite them with the new ones
                        // and add any new ones
                        // any annotations in SCRATCH have been put there 
                        // by this method
                        for ( int i=0;i<annotations.size();i++ )
                        {
                            JSONObject ann = (JSONObject)annotations.get(i);
                            int key = ((Number)ann.get(JSONKeys.ID)).intValue();
                            if ( map.containsKey(key) )
                            {
                                JSONObject old = (JSONObject)map.get(key);
                                conn.removeFromDb(Database.SCRATCH, 
                                    (String) old.get(JSONKeys.DOCID) );
                                conn.putToDb( Database.SCRATCH, docid+"/"
                                    +version1+"/"+UUID.randomUUID().toString(), 
                                    ann.toJSONString() );
                            }
                        }
                    }
                }
                response.setContentType("text/html;charset=UTF-8");
                response.getWriter().write("<p>OK</p>");
            } 
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
