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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import mml.Utils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import mml.exception.*;
import mml.constants.Params;
import mml.database.*;
/**
 * Handle uploads of images
 * @author desmond
 */
public class MMLPostImageHandler extends MMLPostHandler
{
    ArrayList<ImageFile> images;
    /**
     * Parse the import params from the request
     * @param request the http request
     */
    void parseImportParams( HttpServletRequest request ) throws MMLException
    {
        try
        {
            FileItemFactory factory = new DiskFileItemFactory();
            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            // Parse the request
            List items = upload.parseRequest( request );
            for ( int i=0;i<items.size();i++ )
            {
                FileItem item = (FileItem) items.get( i );
                if ( item.isFormField() )
                {
                    String fieldName = item.getFieldName();
                    if ( fieldName != null )
                    {
                        String contents = item.getString();
                        if ( fieldName.equals(Params.DOCID) )
                        {
                            int index = contents.lastIndexOf(".");
                            if ( index != -1 )
                                contents = contents.substring(0,index);
                            docid = contents;
                        }
                        else if ( fieldName.equals(Params.ENCODING) )
                            encoding = contents;
                    }
                }
                else if ( item.getName().length()>0 )
                {
                    try
                    {
                        // item.getName retrieves the ORIGINAL file name
                        String type = item.getContentType();
                        if ( type != null && type.startsWith("image/") )
                        {
                            InputStream is = item.getInputStream();
                            ByteHolder bh = new ByteHolder();
                            while ( is.available()>0 )
                            {
                                byte[] b = new byte[is.available()];
                                is.read( b );
                                bh.append( b );
                            }
                            ImageFile iFile = new ImageFile(
                                item.getName(), 
                                item.getContentType(), 
                                bh.getData() );
                            if ( images == null )
                                images = new ArrayList<>();
                            images.add( iFile );
                        }
                    }
                    catch ( Exception e )
                    {
                        throw new MMLException( e );
                    }
                }
            }
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
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
                    Connector.getConnection().putImageToDb( 
                        database, docid, iFile.getData(), iFile.getWidth(), 
                        iFile.getHeight(), iFile.type );
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
