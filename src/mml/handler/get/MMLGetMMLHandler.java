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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.constants.Database;
import mml.constants.Params;
import mml.database.Connection;
import mml.database.Connector;
import mml.exception.MMLException;
import mml.exception.MMLDbException;
import mml.handler.AeseVersion;
import mml.handler.json.DialectKeys;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 * Get an MML representation of a file
 * @author desmond
 */
public class MMLGetMMLHandler extends MMLGetHandler
{
    HashMap<String,JSONObject> invertIndex;
    JSONObject dialect;
    JSONObject stil;
    String docid;
    String version1;
    StringBuilder mml;
    
    /**
     * Class to represent the postponed end-tag 
     */
    class EndTag
    {
        String text;
        int offset;
        EndTag( int offset, String text )
        {
            this.offset = offset;
            this.text = text;
        }
    }
    /**
     * Emit the start "tag" of an MML property
     * @param defn the property definition
     * @return the text associated with tag-start
     */
    String mmlStartTag( JSONObject defn )
    {
        String kind = (String)defn.get("kind");
        DialectKeys key = DialectKeys.valueOf(kind);
        switch ( key )
        {
            case section:
            case paragraph:
            case headings:
                return "";
            case codeblocks:
                return "    ";
            case quotations:
                return "> ";
            case dividers:
                return "\n";
            case charformats:
                return (String)defn.get("tag");
            case paraformats:
                return (String)defn.get("startTag");
            case milestones:
                return "\n"+(String)defn.get("startTag");
            default:
                return "";
        }
    }
    /**
     * Emit the end-tag associated with an MML property
     * @param defn the property definition
     * @return the text associated with the end of the property
     */
    String mmlEndTag( JSONObject defn )
    {
        String kind = (String)defn.get("kind");
        DialectKeys key = DialectKeys.valueOf(kind);
        switch ( key )
        {
            case section:
                return "\n\n\n";
            case paragraph:
                return "\n\n";
            case codeblocks:
                return "\n";
            case quotations:
                return "\n";
            case headings:
                int len = 0;
                for ( int i=mml.length()-1;i>=0;i-- )
                {
                    if ( mml.charAt(i)=='\n' )
                        break;
                    else
                        len++;
                }
                StringBuilder sb = new StringBuilder();
                for ( int i=0;i<len;i++ )
                    sb.append(defn.get("tag"));
                return sb.toString()+"\n\n";
            case dividers:
                return (String)defn.get("tag")+"\n";
            case charformats:
                return (String)defn.get("tag");
            case paraformats:
                return (String)defn.get("endTag")+"\n\n";
            case milestones:
                return (String)defn.get("endTag")+"\n";
            default:
                return "";
        }
    }
    /**
     * Build the invert index one property at a time
     * @param value the object describing the markup
     * @param kind the kind of markup object
     * @param def the default value for the property name
     */
    void enterProp( Object value, String kind, String def )
    {
        String prop = (String)((JSONObject)value).get("prop");
        ((JSONObject)value).put("kind",kind);
        if ( prop != null && prop.length()>0 )
            invertIndex.put( prop, (JSONObject)value );
        else if ( def.length()>0 )
            invertIndex.put(def,(JSONObject)value );
    }
    /**
     * Make a reverse index of the dialect file
     */
    private void invertDialect()
    {
        this.invertIndex = new HashMap<>();
        JSONArray array;
        Set<String> keys = this.dialect.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String keyword = iter.next();
            DialectKeys key = DialectKeys.valueOf(keyword);
            Object value = dialect.get(keyword);
            switch ( key )
            {
                case section:
                    enterProp(value,keyword,"div");
                    break;
                case paragraph:
                    enterProp(value,keyword,"p");
                    break;
                case codeblocks:
                    enterProp(value,keyword,"pre");
                    break;
                case quotations:
                    enterProp(value,keyword,"quote");
                    break;
                case headings: case dividers: case charformats:
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,(String)obj.get("tag"));
                    }
                    break;
                case paraformats: case milestones:
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,(String)obj.get(""));
                    }
                break;
            }
        }
    }
    /**
     * Create the MMLtext using the invert index and the cortex and corcode
     * @param cortex the plain text version
     * @param corcode the STIL markup for that plain text
     */
    void createMML( AeseVersion cortex, AeseVersion corcode )
    {
        String text = cortex.getVersionString();
        mml = new StringBuilder();
        String stil = corcode.getVersionString();
        JSONObject markup = (JSONObject)JSONValue.parse(stil);
        JSONArray ranges = (JSONArray)markup.get("ranges");
        Stack<EndTag> stack = new Stack<>();
        int offset = 0;
        for ( int i=0;i<ranges.size();i++ )
        {
            JSONObject r = (JSONObject)ranges.get(i);
            Integer len = (Integer)r.get("len");
            Integer relOff = (Integer)r.get("refoff");
            String name = (String)r.get("name");
            if ( len.intValue() > 0 && invertIndex.containsKey(name) )
            {
                JSONObject def = invertIndex.get(name);
                String startTag = mmlStartTag(def);
                String endTag = mmlEndTag(def);
                if ( stack.peek().offset <= offset )
                    mml.append( stack.pop().text );
                for ( int j=offset;j<offset+relOff.intValue();j++ )
                    mml.append(text.charAt(j));
                mml.append(startTag);
                stack.push(new EndTag(offset+len.intValue(),endTag));
            }
            offset += relOff.intValue();
        }
    }
    /**
     * Get the short form of the full docid
     * @return language/author/work only
     */
    String shortenDocID()
    {
        String[] parts = docid.split("/");
        if ( parts.length <= 3 )
            return docid;
        else
            return parts[0]+"/"+parts[1]+"/"+parts[2];
    }
    /**
     * Handle the request
     * @param request the request
     * @param response the response
     * @param urn the remaining urn of the request
     * @throws MMLException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException 
    {
        try
        {
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            version1 = request.getParameter(Params.VERSION1);
            AeseVersion cortex = doGetResourceVersion( Database.CORTEX, 
                docid, version1 );
            AeseVersion corcode = doGetResourceVersion( Database.CORCODE, 
                docid+"/default", version1 );
            String shortID = shortenDocID();
            this.dialect = getDialect( shortID );
            invertDialect();
            createMML(cortex,corcode);
            response.getWriter().println(mml.toString());
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
    /**
     * Get a dialect file direct from the database
     * @param docID the dialect's short docid
     * @return a JSONObject already parsed 
     * @throws MMLException if it wasn't retrievable
     */
    JSONObject getDialect( String docID ) throws MMLException
    {
        try
        {
            String jStr = Connector.getConnection().getFromDb(
                Database.DIALECTS,docID);
            if ( jStr != null )
            {
                return (JSONObject)JSONValue.parse( jStr );
            }
            else
                throw new MMLDbException("couldn't find dialect "+docID );
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
