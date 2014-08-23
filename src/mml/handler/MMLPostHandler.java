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
package mml.handler;
import java.util.List;

import mml.exception.*;
import java.net.InetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import mml.constants.*;
/**
 * Handle a POST request
 * @author desmond
 */
public class MMLPostHandler extends MMLHandler
{
    InetAddress poster;
    String html;
    public MMLPostHandler()
    {
        encoding = "UTF-8";
    }
    /**
     * Parse the import params from the request
     * @param request the http request
     */
    private void parseImportParams( HttpServletRequest request ) 
        throws MMLException
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
                        if ( fieldName.equals(Params.ENCODING) )
                        {
                            encoding = item.getString();
                        }
                    }
                }
                else if ( item.getName().length()>0 )
                {
                    try
                    {
                        // assuming that the contents are text
                        // item.getName retrieves the ORIGINAL file name
                        String type = item.getContentType();
                        if ( type != null && type.equals("text/html") )
                        {
                            byte[] rawData = item.get();
                            //System.out.println(encoding);
                            html = new String(rawData, encoding);
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
    /**
     * Get the sender's IP-address (prevent DoS via too many uploads)
     * @param request raw request
     * @return the server'sIP as a string
     */
    private InetAddress getIPAddress( HttpServletRequest request ) 
        throws Exception
    {
        String ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (ipAddress == null) {
            ipAddress = request.getRemoteAddr();
        }
        InetAddress addr = InetAddress.getByName(ipAddress);
        return addr;
    }
    /**
     * Handle a POST request
     * @param request the raw request
     * @param response the response we will write to
     * @param urn the rest of the URL after stripping off the context
     * @throws TiltException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
    }
}
