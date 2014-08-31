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
import mml.constants.JSONKeys;
import mml.database.Connection;
import mml.database.Connector;
import mml.exception.*;
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
     * @param offset the offset in the text
     * @return the text associated with tag-start
     */
    String mmlStartTag( JSONObject defn, int offset )
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
                if ( defn.containsKey("tag") )
                    return (String)defn.get("tag");
                else
                    return (String)defn.get("leftTag");
            case paraformats:
                return (String)defn.get("leftTag");
            case milestones:
                return ((offset>0)?"\n":"")+(String)defn.get("leftTag");
            default:
                return "";
        }
    }
    /**
     * Emit the end-tag associated with an MML property
     * @param defn the property definition
     * @param length of the property
     * @return the text associated with the end of the property
     */
    String mmlEndTag( JSONObject defn, int len )
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
                StringBuilder sb = new StringBuilder();
                for ( int i=0;i<len-1;i++ )
                    sb.append(defn.get("tag"));
                return "\n"+sb.toString()+"\n\n";
            case dividers:
                return (String)defn.get("tag")+"\n";
            case charformats:
                if ( defn.containsKey("tag") )
                    return (String)defn.get("tag");
                else
                    return (String)defn.get("rightTag");
            case paraformats:
                return (String)defn.get("rightTag")+"\n\n";
            case milestones:
                return (String)defn.get("rightTag")+"\n";
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
                case softhyphens:
                    if (((Boolean)value).booleanValue() )
                    {
                        JSONObject sh = new JSONObject();
                        sh.put("kind","charformats");
                        sh.put("leftTag","");
                        sh.put("rightTag","\n");
                        this.invertIndex.put("soft-hyphen",sh);
                        JSONObject hh = new JSONObject();
                        hh.put("kind","charformats");
                        hh.put("leftTag","");
                        hh.put("rightTag","-\n");
                        this.invertIndex.put("hard-hyphen",sh);
                    }
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
                case paraformats: 
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,"p");
                    }
                case milestones:
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,"span");
                    }
                break;
            }
        }
    }
    /**
     * Prune trailing NLs in mml the max of trailing and leading NLs
     * @param tag the new tag start with leading NLs
     */
    private void normaliseNewlines( String tag )
    {
        int leadingNLs = 0;
        for( int i=0;i<tag.length();i++ )
            if ( tag.charAt(i)=='\n')
                leadingNLs++;
            else
                break;
        int trailingNLs =0;
        for ( int i=mml.length()-1;i>=0;i-- )
            if ( mml.charAt(i)=='\n' )
                trailingNLs++;
            else
                break;
        int both = Math.max(leadingNLs,trailingNLs);
        int total = leadingNLs+trailingNLs;
        int delenda = total-both;
        mml.setLength(mml.length()-delenda);
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
            Long len = (Long)r.get("len");
            Long relOff = (Long)r.get("reloff");
            String name = (String)r.get("name");
            if ( len.intValue() > 0 && invertIndex.containsKey(name) )
            {
                JSONObject def = invertIndex.get(name);
                String startTag = mmlStartTag(def,offset);
                String endTag = mmlEndTag(def,len.intValue());
                int start = offset+relOff.intValue();
                // 1. insert pending end-tags and text
                int pos = offset;
                while ( !stack.isEmpty() && stack.peek().offset <= start )
                {
                    int tagEnd = stack.peek().offset;
                    for ( int j=pos;j<tagEnd;j++ )
                        mml.append(text.charAt(j));
                    pos = tagEnd;
                    // newlines are not permitted before tag end
                    while ( mml.length()>0 && mml.charAt(mml.length()-1)=='\n')
                        mml.setLength(mml.length()-1);
                    mml.append( stack.pop().text );
                }
                // 2. insert intervening text
                for ( int j=pos;j<start;j++ )
                    mml.append(text.charAt(j));
                // 3. insert new start tag
                normaliseNewlines(startTag);
                mml.append(startTag);
                stack.push(new EndTag(start+len.intValue(),endTag));
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
            response.setCharacterEncoding(encoding);
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
                JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
                String bodyStr = (String)jDoc.get(JSONKeys.BODY);
                JSONObject body = (JSONObject)JSONValue.parse(bodyStr);
                if ( body == null )
                    throw new JSONException("body key not found");
                return body;
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
