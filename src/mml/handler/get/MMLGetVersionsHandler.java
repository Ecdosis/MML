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

import calliope.core.constants.Database;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.constants.Params;
import mml.exception.MMLException;
import mml.handler.AeseResource;

/**
 * Get the versions of the current docid
 * @author desmond
 */
public class MMLGetVersionsHandler extends MMLGetHandler {
    /**
     * Get the version listing from the MVD
     * @param request the request
     * @param response the response
     * @param urn the remaining urn being empty
     * @throws MMLException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException 
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            if ( docid == null )
                throw new Exception("You must specify a docid parameter");
            AeseResource res = doGetResource( Database.CORTEX, docid );
            StringBuilder json = new StringBuilder();
            json.append("[ ");
            if ( res != null )
            {
                String[] versions = res.listVersions();
                for ( int i=0;i<versions.length;i++ )
                {
                    json.append("{ \"vid\":");
                    json.append( "\"" );
                    json.append(versions[i]);
                    json.append( "\",\"desc\":\"");
                    json.append(res.getVersionLongName(i+1));
                    json.append("\"}");
                    if ( i < versions.length-1 )
                        json.append(",");
                }
            }
            json.append(" ]");
            response.setContentType("application/json");
            response.setCharacterEncoding(encoding);
            response.getWriter().println(json.toString());
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }  
    }
}
