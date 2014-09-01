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
import mml.exception.MMLException;
import mml.Utils;
import mml.constants.Database;
/**
 * Handle some kind of import
 * @author desmond
 */
public class MMLPostLiteralHandler extends MMLPostHandler
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        String first = Utils.first(urn);
        urn = Utils.pop(urn);
        if ( first.equals(Database.CORFORM) )
            new MMLPostResourceHandler(Database.CORFORM).handle(request,response,urn);
        else if ( first.equals(Database.CORPIX) )
            new MMLPostImageHandler().handle(request,response,urn);
        else if ( first.equals(Database.CORTEX) )
            new MMLPostResourceHandler(Database.CORTEX).handle(request,response,urn);
        else if ( first.equals(Database.CORCODE) )
            new MMLPostResourceHandler(Database.CORCODE).handle(request,response,urn);
        else if ( first.equals(Database.DIALECTS) )
            new MMLPostResourceHandler(Database.DIALECTS).handle(request,response,urn);
        else
            throw new MMLException("Unknown service "+first);
    }
}
