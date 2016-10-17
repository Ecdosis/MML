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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/**
 * Get the versions of the current docid
 * @author desmond
 */
public class MMLGetVersionsHandler extends MMLGetHandler {
    /**
     * Striper a version identifier of its layer specification
     * @param vid the version identifier
     * @return the base version name
     */
    String getBaseVersion( String vid )
    {
        String[] parts = vid.split("/");
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<parts.length;i++ )
        {
            if ( parts[i].length()>0 )
            {
                if ( parts[i].contains("layer-") )
                    break;
                sb.append("/");
                sb.append(parts[i]);
            }
        }
        if ( sb.length()==0 )
            sb.append("/base");
        return sb.toString();
    }
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
            JSONArray jVersions = new JSONArray();
            json.append("[ ");
            if ( res != null )
            {
                String[] versions = res.listVersions();
                HashMap<String,JSONObject> vSet = new HashMap<String,JSONObject>();
                for ( int i=0;i<versions.length;i++ )
                {
                    String base = getBaseVersion(versions[i]);
                    JSONObject jObj = vSet.get(base);
                    if ( jObj == null )
                        jObj = new JSONObject();
                    if ( versions[i].endsWith("layer-final")
                        ||!versions[i].contains("layer-"))
                        jObj.put("desc",res.getVersionLongName(i+1));
                    if ( versions[i].contains("layer-") )
                    {
                        JSONArray jArr = (JSONArray)jObj.get("layers");
                        if ( jArr == null )
                            jArr = new JSONArray();
                        int index = versions[i].lastIndexOf("layer");
                        String layerName = versions[i].substring(index);
                        if ( !jArr.contains(layerName) )
                            jArr.add(layerName);
                        if ( !jObj.containsKey("layers") )
                            jObj.put("layers",jArr);
                    }
                    if ( !vSet.containsKey( base ) )
                        vSet.put(base,jObj);
                }
                Set<String> keys = vSet.keySet();
                Iterator<String> iter = keys.iterator();
                while ( iter.hasNext() )
                {
                    String version = iter.next();
                    JSONObject jObj = vSet.get(version);
                    jObj.put("vid",version);
                    jVersions.add( jObj );
                }
            }
            json.append(" ]");
            response.setContentType("application/json");
            response.setCharacterEncoding(encoding);
            String jStr = jVersions.toJSONString().replaceAll("\\\\/", "/");
            response.getWriter().println(jStr);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }  
    }
}
