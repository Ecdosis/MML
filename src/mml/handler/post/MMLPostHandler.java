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
import calliope.AeseSpeller;
import java.io.InputStream;

import mml.exception.*;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.Utils;
import mml.constants.Params;
import mml.handler.json.STILDocument;
import mml.constants.Service;
import mml.handler.MMLHandler;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.json.simple.JSONObject;

/**
 * Handle a POST request
 * @author desmond
 */
public class MMLPostHandler extends MMLHandler
{
    AeseSpeller speller;
    InetAddress poster;
    String html;
    StringBuilder sb;
    STILDocument stil;
    JSONObject dialect;
    String langCode;
    String author;
    String title; 
    String style; 
    String format;
    String section;
    String version1;
    protected String docid;
    ArrayList<ImageFile> images;
    ArrayList<String> files;
    
    /**
     * Create a POST handler for HTML that used to be MML
     */
    public MMLPostHandler()
    {
        encoding = "UTF-8";
        langCode = Locale.getDefault().getLanguage();
        stil = new STILDocument();
    }
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
                        String contents = item.getString(this.encoding);
                        if ( fieldName.equals(Params.DOCID) )
                        {
                            int index = contents.lastIndexOf(".");
                            if ( index != -1 )
                                contents = contents.substring(0,index);
                            docid = contents;
                        }
                        else if ( fieldName.equals(Params.AUTHOR) )
                            this.author = contents;
                        else if ( fieldName.equals(Params.TITLE) )
                            this.title = contents; 
                        else if ( fieldName.equals(Params.STYLE) )
                            this.style = contents;
                        else if ( fieldName.equals(Params.FORMAT) )
                            this.format = contents;
                        else if ( fieldName.equals(Params.SECTION) )
                            this.section = contents;
                        else if ( fieldName.equals(Params.VERSION1) )
                            this.version1 = contents;
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
                        if ( type != null )
                        {
                            if ( type.startsWith("image/") )
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
                            else if ( type.equals("text/plain") )
                            {
                                InputStream is = item.getInputStream();
                                ByteHolder bh = new ByteHolder();
                                while ( is.available()>0 )
                                {
                                    byte[] b = new byte[is.available()];
                                    is.read( b );
                                    bh.append( b );
                                }
                                String style = new String( bh.getData(), encoding );
                                if ( files == null )
                                    files = new ArrayList<>();
                                files.add( style );
                            }
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
     * Handle a POST request
     * @param request the raw request
     * @param response the response we will write to
     * @param urn the rest of the URL after stripping off the context
     * @throws MMLException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            String service = Utils.first(urn);
            urn = Utils.pop(urn);
            if ( service.equals(Service.HTML) )
                new MMLPostHTMLHandler().handle(request,response,urn);
            else if ( service.equals(Service.IMPORT) )
                new MMLPostImportHandler().handle(request,response,urn);
            else
                throw new MMLException("invalid POST urn: "+urn);
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
