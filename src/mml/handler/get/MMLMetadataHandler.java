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

import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.Acronym;
import calliope.core.Utils;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.DocType;
import mml.constants.Params;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import org.json.simple.*;
import java.util.Set;
import java.util.Iterator;

/**
 * Get metadata about a document
 * @author desmond
 */
public class MMLMetadataHandler extends MMLGetHandler 
{
    String trimZeros(String num)
    {
        int i = 0;
        while ( num.charAt(i)=='0' )
            i++;
        return num.substring(i);
    }
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException {
        try
        {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            if ( docid == null || docid.length()== 0 )
                docid = urn;
            JSONObject md = new JSONObject();
            boolean changed = false;
            String docId = docid;
            do
            {
                String jStr = conn.getMetadata( docId );
                if ( jStr != null )
                {
                    JSONObject jObj = (JSONObject)JSONValue.parse(jStr);
                    Set<String> keys = jObj.keySet();
                    Iterator<String> iter = keys.iterator();
                    while ( iter.hasNext() )
                    {
                        String key = iter.next();
                        md.put( key, jObj.get(key) );
                    }
                    changed = true;
                }
                else
                {
                    String ctStr = conn.getFromDb(Database.CORTEX,docId);
                    if ( ctStr != null )
                    {
                        JSONObject jObj = (JSONObject)JSONValue.parse(ctStr);
                        if ( jObj.containsKey(JSONKeys.DESCRIPTION) )
                        {
                            String desc = ((String)jObj.get(JSONKeys.DESCRIPTION)).replaceAll("%20"," ");
                            if ( desc.startsWith("\"") )
                                desc = desc.substring(1);
                            if ( desc.endsWith("\"") )
                                desc = desc.substring(0,desc.length()-2);
                            desc = desc.replaceAll("\"\"","\"");
                            md.put(JSONKeys.TITLE,desc);
                        }
                        else if ( !md.containsKey(JSONKeys.TITLE) && DocType.isLetter(docId))
                        {
                            int index = docId.lastIndexOf("/");
                            String shortId;
                            if ( index != -1 )
                                shortId = docId.substring(index+1);
                            else
                                shortId = docId;
                            String[] parts = shortId.split("-");
                            StringBuilder sb = new StringBuilder();
                            String projid = Utils.getProjectId(docId);
                            if ( parts.length>=2 )
                            {
                                String from = Acronym.expand(projid,parts[parts.length-2]);
                                String to = Acronym.expand(projid,parts[parts.length-1]);
                                sb.append("Letter from "+from);
                                sb.append(" to ");
                                sb.append(to);
                                sb.append(",");
                            }
                            if ( parts.length>=3 )
                            {
                                for ( int i=0;i<3;i++ )
                                {
                                    if ( DocType.isDay(parts[i]) )
                                    {
                                        sb.append(" ");
                                        sb.append(trimZeros(parts[i]));
                                    }
                                    else if ( DocType.isMonth(parts[i]))
                                    {
                                        sb.append(" ");
                                        sb.append(Acronym.expand(projid,parts[i]));
                                    }
                                    else if ( DocType.isYear(parts[i]) )
                                    {
                                        sb.append(" ");
                                        sb.append(parts[i]);
                                        // maybe only a year
                                        break;
                                    }
                                }
                                md.put(JSONKeys.TITLE,sb.toString());
                            }
                        }
                        else
                            System.out.println("No metadata found for "+docId);
                    }
                    else
                        System.out.println("No metadata found for "+docId);
                    changed = false;
                }
                docId = Utils.chomp(docId);
            } while ( changed );
            response.setContentType("application/json");
            response.setCharacterEncoding(encoding);
            String mdStr = md.toJSONString();
            response.getWriter().println(mdStr);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}
