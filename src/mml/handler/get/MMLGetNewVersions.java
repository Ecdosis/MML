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

package mml.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import calliope.core.constants.JSONKeys;
import mml.MMLWebApp;
import java.util.HashMap;
import java.util.Iterator;
import java.util.HashSet;
import java.util.Set;
import java.io.File;
import java.io.IOException;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import calliope.core.database.Connector;
import calliope.core.constants.Database;
import calliope.core.database.Connection;
import calliope.core.exception.DbException;
import calliope.core.handler.EcdosisMVD;

/**
 * Get the unedited (new) versions of a document
 * @author desmond
 */
public class MMLGetNewVersions extends MMLGetHandler
{
    /**
     * Get the edited versions from the MVD 
     * @return a JSON array of version ids in string form
     */
    String getVersionsFromMvd()
    {
        try
        {
            Connection conn = Connector.getConnection();
            String str = conn.getFromDb(Database.CORTEX,docid);
            if ( str != null )
            {
                JSONObject jObj = (JSONObject)JSONValue.parse(str);
                EcdosisMVD eMvd = new EcdosisMVD(jObj);
                String[] all = eMvd.getAllVersions();
                JSONArray jArr = new JSONArray();
                for ( int i=0;i<all.length;i++ )
                    jArr.add(all[i]);
                return jArr.toJSONString().replaceAll("\\\\/","/");
            }
            else
                return "[]";
        }
        catch ( DbException de )
        {
            return "[]";
        }
        
    }
    /**
     * Scan an image directory looking for version directories
     * @param dir the root dir to start from
     * @param path the path starting here
     * @param vids a set of vids already found
     */
    void scanDir( File dir, String path, HashMap<String,Metadata> vids )
    {
        File[] files = dir.listFiles();
        for ( int i=0;i<files.length;i++ )
        {
            if ( files[i].isDirectory() )
                scanDir(files[i],path+"/"+files[i].getName(),vids);
            // the file name is an image, not a version
            else if ( !vids.containsKey(path) )
            {
                Metadata md = new Metadata(files[i]);
                vids.put(path,md);
            }
            else
            {
                Metadata md = vids.get(path);
                md.add(files[i]);
            }
        }
    }
    /**
     * Reduce a list of versions including layer-names to pure version names
     * @param jArr a JSONArray of full version names
     * @return a reduced list of only pure versions
     */
    JSONArray reduceToRealVersions( JSONArray jArr )
    {
        HashSet<String> pure = new HashSet<String>();
        for ( int i=0;i<jArr.size();i++ )
        {
            String vName = (String)jArr.get(i);
            int index = vName.lastIndexOf("/layer-");
            if ( index != -1 )
                vName = vName.substring(0,index);
            pure.add( vName );
        }
        JSONArray rep = new JSONArray();
        Iterator<String> iter = pure.iterator();
        while ( iter.hasNext() )
            rep.add( iter.next());
        return rep;
    }
    /**
     * Handle the request
     * @param request the http request object
     * @param response the response to write
     * @param urn the residual urn
     * @throws MMLException 
     */
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException 
    {
        docid = request.getParameter(JSONKeys.DOCID);
        JSONArray jArr = new JSONArray();
        if ( docid != null )
        {
            String vStr = getVersionsFromMvd();
            JSONArray vArr = (JSONArray)JSONValue.parse(vStr);
            vArr = reduceToRealVersions( vArr );
            String path = MMLWebApp.webRoot+"/corpix/"+docid;
            File dir = new File( path );
            if ( dir.exists() && dir.isDirectory() )
            {
                HashMap<String,Metadata> vids = new HashMap<String,Metadata>();
                scanDir( dir, "", vids );
                // remove versions already in vArr
                for ( int i=0;i<vArr.size();i++ )
                {
                    String version = (String)vArr.get(i);
                    if ( vids.containsKey(version) )
                        vids.remove(version);
                }
                Set<String> keys = vids.keySet();
                Iterator<String> iter = keys.iterator();
                while ( iter.hasNext() )
                {
                    JSONObject jObj = new JSONObject();
                    String key = iter.next();
                    jObj.put("vid",key);
                    Metadata md = vids.get(key);
                    jObj.put("desc", md.toString());
                    jArr.add(jObj);
                }
            }
        }
        else
            throw new MMLException("Missing docid");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String mdStr = jArr.toJSONString().replaceAll("\\\\/","/");
        try
        {
            response.getWriter().println(mdStr);
        }
        catch ( IOException ioe )
        {
            throw new MMLException(ioe);
        }
    }
}
