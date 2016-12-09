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
 *  (c) copyright Desmond Schmidt 2015
 */
package mml.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import mml.constants.Params;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import java.io.File;
import java.util.Arrays;
import mml.MMLWebApp;

/**
 * Get the version1 attribute of a CORTEX
 * @author desmond
 */
public class MMLGetVersion1Handler extends MMLGetHandler
{
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException 
    {
        try {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            String res = conn.getFromDb(Database.CORTEX,docid);
            if ( res != null )
            {
                JSONObject jObj = (JSONObject)JSONValue.parse(res);
                if ( jObj.containsKey(JSONKeys.VERSION1) )
                {
                    version1 = (String)jObj.get(JSONKeys.VERSION1);
                    if ( !Layers.isNewStyleLayer(version1) )
                    {
                        if ( ((String)jObj.get(JSONKeys.FORMAT)).startsWith("MVD"))
                        {
                            String body = (String)jObj.get(JSONKeys.BODY);
                            if ( body != null )
                            {
                                MVD mvd = MVDFile.internalise(body);
                                String[] all = getAllVersions(mvd);
                                version1 = Layers.upgradeLayerName( all, version1);
                            }
                        }
                        else
                        {
                            String[] all = new String[1];
                            all[0] = version1;
                            version1 = Layers.upgradeLayerName(all,version1);
                        }
                    }
                }
                else if ( ((String)jObj.get(JSONKeys.FORMAT)).startsWith("MVD"))
                {
                    String body = (String)jObj.get(JSONKeys.BODY);
                    if ( body != null )
                    {
                        MVD mvd = MVDFile.internalise(body);
                        String[] all = getAllVersions(mvd);
                        String groupPath = mvd.getGroupPath((short)1);
                        String shortName = mvd.getVersionShortName((short)1);
                        version1 = Layers.upgradeLayerName( all, groupPath+"/"+shortName);
                        jObj.put(JSONKeys.VERSION1, version1);
                        jObj.remove(JSONKeys._ID);
                        conn.putToDb(Database.CORTEX,docid,jObj.toJSONString());
                    }
                    else
                        version1 = "";  // nothing there
                }
                else
                    version1 = "/base/layer-final";
            }
            else
            {
                // try to get it from corpix
                String path = MMLWebApp.webRoot+"/corpix/"+docid;
                File dir = new File(path);
                version1 = "";
                if ( dir.exists() )
                {
                    String[] files = dir.list();
                    Arrays.sort(files);
                    for ( int i=0;i<files.length;i++ )
                    {
                        if ( files[i].startsWith(dir.getName()) )
                        {
                            version1 = "/"+files[i]+"/layer-final";
                            break;
                        }
                    }        
                }
                else if ( dir.getParentFile().exists() )
                    version1 = "/base/layer-final";
                else
                    version1 = "";
            }
            response.setContentType("text/plain");
            response.getWriter().write(version1.replaceAll("\\\\/", "/"));
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
