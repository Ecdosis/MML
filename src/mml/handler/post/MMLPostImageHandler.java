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
package mml.handler.post;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import calliope.core.Utils;
import mml.exception.*;
import calliope.core.image.Corpix;
import mml.MMLWebApp;
/**
 * Handle uploads of images
 * @author desmond
 */
public class MMLPostImageHandler extends MMLPostHandler
{
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            String database = Utils.first(urn);
            if (ServletFileUpload.isMultipartContent(request) )
            {
                parseImportParams( request );
                for ( int i=0;i<images.size();i++ )
                {
                    ImageFile iFile = images.get(i);
                    Corpix.addImage( MMLWebApp.webRoot, docid, 
                        iFile.getName(),null,iFile.type,iFile.getData() );
                }
                response.setContentType("text/html;charset=UTF-8");
            } 
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
