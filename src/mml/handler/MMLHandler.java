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
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.exception.DbException;
import mml.exception.MMLException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.Charset;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Abstract super-class for all handlers: PUT, POST, DELETE, GET
 * @author ddos
 */
abstract public class MMLHandler 
{
    protected String encoding;
    protected String docid;
    protected String version1;
    private boolean isAnnotation( JSONObject jObj )
    {
        if ( jObj.containsKey(JSONKeys.OFFSET) 
            && jObj.containsKey(JSONKeys.LEN)
            && jObj.containsKey(JSONKeys.USER) 
            && jObj.containsKey(JSONKeys.CONTENT) )
            return true;
        else
            return false;
    }
    /**
     * Fetch one annotation from some database
     * @param conn the connection to the database
     * @param coll the database collection
     * @param docId the specific docid to match against
     * @return a JSONObject or null
     * @throws DbException 
     */
    protected JSONObject fetchAnnotation( Connection conn, String coll, 
        String docId ) throws DbException
    {
        String jDoc = conn.getFromDb(coll,docId);
        JSONObject jObj = (JSONObject)JSONValue.parse( jDoc );
        if ( isAnnotation(jObj) )
        {
            if ( jObj.containsKey(JSONKeys.VERSIONS) )
            {
                JSONArray versions = (JSONArray)jObj.get(
                    JSONKeys.VERSIONS);
                if ( versions.contains(version1) )
                    return jObj;
            }
        }
        return null;
    }
    public MMLHandler()
    {
        this.encoding = Charset.defaultCharset().name();
    }
    public abstract void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException;
}
