/* This file is part of MML.
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
 */

package mml.handler.post;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.Utils;
import mml.database.Connector;
import mml.exception.MMLException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;
import mml.constants.Database;
import mml.constants.JSONKeys;

/**
 *Handle uploads of CSS files
 * @author desmond
 */
public class MMLPostCSSHandler  extends MMLPostHandler
{
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            if (ServletFileUpload.isMultipartContent(request) )
            {
                parseImportParams( request );
                for ( int i=0;i<files.size();i++ )
                {
                    String style = files.get(i);
                    JSONObject jDoc = new JSONObject();
                    jDoc.put( JSONKeys.BODY, style );
                    Connector.getConnection().putToDb( Database.CORFORM, 
                        docid, jDoc.toJSONString() );
                }
            } 
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
