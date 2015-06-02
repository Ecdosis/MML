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
package mml.handler.get;

import calliope.core.database.Connector;
import calliope.core.database.Connection;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.exception.DbException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.constants.Params;
import mml.exception.MMLException;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/**
 * Handle requests to get annotations
 * @author desmond
 */
public class MMLGetAnnotationsHandler extends MMLGetHandler 
{
    /**
     * Handle a request for options
     * @param request the http request
     * @param response the http response
     * @param urn the urn (ignored)
     * @throws MMLException 
     */
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            JSONArray annotations = new JSONArray();
            docid = request.getParameter(Params.DOCID);
            version1 = request.getParameter(Params.VERSION1);
            if ( docid != null && version1 != null )
            {
                Connection conn = Connector.getConnection();
                String[] docids = conn.listDocuments(Database.SCRATCH, 
                    docid+".*", JSONKeys.DOCID);
                if ( docids != null && docids.length>0 )
                {
                    for ( int i=0;i<docids.length;i++ )
                    {
                        JSONObject jObj = fetchAnnotation(conn, 
                            Database.SCRATCH, docids[i] );
                        if ( jObj != null )
                            annotations.add( jObj );
                    }
                }
                if ( annotations.isEmpty() )  // nothing in SCRATCH space
                {
                    docids = conn.listDocuments(Database.ANNOTATIONS, 
                        docid+".*", JSONKeys.DOCID );
                    if ( docids != null && docids.length>0 )
                    {
                        for ( int i=0;i<docids.length;i++ )
                        {
                            JSONObject jObj = fetchAnnotation(conn, 
                                Database.ANNOTATIONS, docids[i] );
                            if ( jObj != null )
                                annotations.add( jObj );
                        }
                    }
                }
            }
            String res = annotations.toJSONString();
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write(res);
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
