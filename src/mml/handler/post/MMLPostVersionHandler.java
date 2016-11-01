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
 *  (c) copyright Desmond Schmidt 2016
 */
package mml.handler.post;

import calliope.core.Utils;
import calliope.core.constants.JSONKeys;
import calliope.core.exception.DbException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import mml.handler.get.MMLGetDialectHandler;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import mml.handler.scratch.*;
import calliope.core.constants.Database;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import java.net.URLDecoder;
import mml.handler.json.STILDocument;
/**
 * Post a version of an MVD composed of layers
 * @author desmond
 */
public class MMLPostVersionHandler extends MMLPostHTMLHandler
{
    String longName;
    /**
     * Find the closest matching doialect for the current docid
     * @return a JSON =Object
     * @throws DbException 
     */
    JSONObject getDialectFromDocid() throws DbException
    {
        String res = null;
        String docID = new String(docid);
        try
        {
            Connection conn = Connector.getConnection();
            while ( res == null && docID.length() > 0 )
            {
                res = conn.getFromDb(Database.DIALECTS,docID);
                if ( res == null )
                    docID = Utils.chomp( docID );
            }
            if ( res == null )
                res = MMLGetDialectHandler.DEFAULT_DIALECT;
            else
            {
                JSONObject jObj = (JSONObject)JSONValue.parse(res);
                res = (String) jObj.get(JSONKeys.BODY);
            }
            return (JSONObject)JSONValue.parse(res);
        }
        catch ( DbException e )
        {
            throw e;
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
            String value = request.getParameter("data");
            if ( value != null )
            {
                JSONObject jObj = (JSONObject) JSONValue.parse(value);
                this.version1 = (String)jObj.get(JSONKeys.VERSION1);
                if ( version1==null )
                    version1 = "/base";
                else
                    version1 = URLDecoder.decode(version1,"UTF-8");
                this.longName = (String)jObj.get(JSONKeys.LONGNAME);                
                this.docid = (String)jObj.get(JSONKeys.DOCID);
                this.dialect = getDialectFromDocid();
                JSONArray layers = (JSONArray)jObj.get("layers");
                ScratchVersion corcodeDefault = new ScratchVersion(
                    version1, longName, docid+"/default", 
                    Database.CORCODE,null, true);
                ScratchVersion corcodePages = new ScratchVersion(
                    version1, longName, docid+"/pages", 
                    Database.CORCODE,null,true);
                ScratchVersion text = new ScratchVersion(version1, 
                    longName, docid, Database.CORTEX,null,true);
                this.style = ScratchVersionSet.getDefaultStyleName(docid);
                for ( int i=0;i<layers.size();i++ )
                {
                    JSONObject layer = (JSONObject)layers.get(i);
                    String name = (String)layer.get(JSONKeys.NAME);
                    String html = (String)layer.get(JSONKeys.BODY);
                    stil = new STILDocument(style);
                    pages = new STILDocument(style);
                    // reduce html to text, corcode-default and corcode-pages
                    Document doc = Jsoup.parseBodyFragment(html);
                    Element body = doc.body();  
                    parseBody( body );
                    int num = ScratchVersion.layerNumber(name);
                    text.addLayer(sb.toString().toCharArray(),num);
                    corcodeDefault.addLayer(stil.toString().toCharArray(),num);
                    corcodePages.addLayer(pages.toString().toCharArray(),num);               
                }
                Scratch.save(text);
                Scratch.save(corcodeDefault);
                Scratch.save(corcodePages);
                response.getWriter().write("OK");
            }
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
