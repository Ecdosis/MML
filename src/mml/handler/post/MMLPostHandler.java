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
import org.jsoup.nodes.*;
import calliope.AeseSpeller;
import mml.database.*;

import mml.exception.*;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.Utils;
import mml.handler.json.STILDocument;
import mml.handler.json.Range;
import mml.handler.mvd.Archive;
import mml.constants.Params;
import mml.constants.Database;
import mml.constants.Formats;
import mml.constants.Service;
import mml.handler.MMLHandler;
import mml.handler.json.Dialect;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

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
