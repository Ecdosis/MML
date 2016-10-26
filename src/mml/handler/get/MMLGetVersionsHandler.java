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
            JSONArray jVersions = new JSONArray();
            if ( res != null )
            {
                String[] versions = res.listVersions();
                HashMap<String,JSONObject> vSet = new HashMap<String,JSONObject>();
                for ( int i=0;i<versions.length;i++ )
                {
                    String base = Layers.stripLayer(versions[i]);
                    JSONObject jObj = vSet.get(base);
                    if ( jObj == null )
                        jObj = new JSONObject();
                    String upgraded = Layers.upgradeLayerName(versions,versions[i]);
                    if ( !upgraded.equals(versions[i]) )
                    {
                        JSONArray repl = (JSONArray)jObj.get("replacements");
                        if ( repl == null )
                        {
                            repl = new JSONArray();
                            jObj.put("replacements",repl);
                        }
                        JSONObject entry = new JSONObject();
                        entry.put("old",versions[i]);
                        entry.put("new",upgraded);
                        repl.add( entry);
                    }
                    if ( upgraded.endsWith("layer-final") )
                        jObj.put("desc",res.getVersionLongName(i+1));
                    // add the layer names
                    if ( upgraded.endsWith("layer-final")
                        || upgraded.matches(".*layer-[0-9]+$") )
                    {
                        JSONArray jArr = (JSONArray)jObj.get("layers");
                        if ( jArr == null )
                            jArr = new JSONArray();
                        int index = upgraded.lastIndexOf("layer");
                        String layerName = upgraded.substring(index);
                        if ( !jArr.contains(layerName) )
                            jArr.add(layerName);
                        if ( !jObj.containsKey("layers") )
                            jObj.put("layers",jArr);
                    }
                    if ( !vSet.containsKey( base ) )
                        vSet.put(base,jObj);
                }
                // convert hashmap to array
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
