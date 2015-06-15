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

import calliope.core.exception.DbException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.constants.Database;
import mml.constants.Params;
import mml.DocType;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.JSONKeys;
import mml.exception.*;
import calliope.core.Utils;
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
    StringBuilder mml;
    
    /**
     * Class to represent the postponed end-tag 
     */
    class EndTag
    {
        String text;
        int offset;
        JSONObject def;
        EndTag( int offset, String text, JSONObject def )
        {
            this.offset = offset;
            this.text = text;
            this.def = def;
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
        StringBuilder sb;
        switch ( key )
        {
            case sections:
                if ( !defn.containsKey("prop")
                    ||((String)defn.get("prop")).length()==0 )
                    return "\n\n\n";
                else
                    return "\n\n\n{"+(String)defn.get("prop")+"}\n";
            case paragraph:
            case headings:
                return "";
            case codeblocks:
                sb = new StringBuilder();
                for ( int i=0;i<(Integer)defn.get("level");i++ )
                    sb.append("    ");
                return sb.toString();
            case quotations:
                return "> ";
            case dividers:
                return "";
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
            case sections:
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
                return "\n\n"+(String)defn.get("tag")+"\n\n";
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
        this.invertIndex = new HashMap<String,JSONObject>();
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
                case sections:
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,"div");
                    }
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
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,(String)obj.get("tag")+i);
                        // remember level
                        obj.put("level",i+1);
                    }
                    break;
                case quotations:
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,(String)obj.get("prop"));
                        // remember level
                        obj.put("level",i+1);
                    }
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
        {
            char c = tag.charAt(i);
            if ( c =='\n')
                leadingNLs++;
            else if ( c != '\r'&&c!=' '&&c!='\t' )
                break;
        }
        int trailingNLs =0;
        for ( int i=mml.length()-1;i>=0;i-- )
        {
            char c = mml.charAt(i);
            if ( c=='\n' )
                trailingNLs++;
            else if ( c != '\r'&&c!=' '&&c!='\t' )
                break;
        }
        int both = Math.max(leadingNLs,trailingNLs);
        int total = leadingNLs+trailingNLs;
        int delenda = total-both;
        int mmlEnd = mml.length()-1;
        while ( mmlEnd> 0 && delenda > 0 )
        {
            char c =  mml.charAt(mmlEnd);
            if ( c=='\n' )
                delenda--;
            mmlEnd--;
        }
        mml.setLength(mmlEnd+1);
    }
    /**
     * Are we in a preformatted section?
     * @param stack the tag stack
     * @return true if it is true
     */
    boolean isInPre( Stack<EndTag> stack )
    {
        if ( !stack.isEmpty() )
        {
            EndTag top = stack.peek();
            if ( top != null )
            {
                JSONObject obj = top.def;
                return "codeblocks".equals(obj.get("kind"));
            }
            else
                return false;
        }
        else return false;
    }
    /**
     * Insert the line-start spacing in a preformatted section
     * @param stack the tag stack to tell us the depth
     */
    void startPreLine( Stack<EndTag> stack )
    {
        JSONObject oldDef = stack.peek().def;
        Integer level = (Integer)oldDef.get("level");
        for (int k=0;k<level;k++ )
            mml.append("    ");
    }
    /**
     * Debug: Check that the corcode stil ranges do not go beyond text end
     * @param stil the stil markup as text
     * @param text the text it refers to
     * @return true if it was OK, else false
     */
    boolean verifyCorCode(String stil, String text )
    {
        JSONObject jObj = (JSONObject)JSONValue.parse(stil);
        JSONArray ranges = (JSONArray)jObj.get(JSONKeys.RANGES);
        int offset = 0;
        for ( int i=0;i<ranges.size();i++ )
        {
            JSONObject range = (JSONObject)ranges.get(i);
            offset += ((Number)range.get("reloff")).intValue();
            int len = ((Number)range.get("len")).intValue();
            if ( offset+len > text.length() )
                return false;
        }
        return true;
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
        Stack<EndTag> stack = new Stack<EndTag>();
        int offset = 0;
        for ( int i=0;i<ranges.size();i++ )
        {
            JSONObject r = (JSONObject)ranges.get(i);
            Long len = (Long)r.get("len");
            Long relOff = (Long)r.get("reloff");
            String name = (String)r.get("name");
            if ( name.startsWith("metamark") )
                System.out.println(name);
            if ( invertIndex.containsKey(name) )
            {
                JSONObject def = invertIndex.get(name);
                String startTag = mmlStartTag(def,offset);
                String endTag = mmlEndTag(def,len.intValue());
                int start = offset+relOff.intValue();
                // 1. insert pending end-tags and text
                int pos = offset;
                while ( !stack.isEmpty() && stack.peek().offset <= start )
                {
                    // check for NLs here if obj is of type codeblocks
                    // and insert however many spaces approporiate for that level
                    int tagEnd = stack.peek().offset;
                    boolean inPre = isInPre( stack );
                    if ( inPre && mml.charAt(mml.length()-1) == '\n' )
                        startPreLine( stack );
                    for ( int j=pos;j<tagEnd;j++ )
                    {
                        char c = text.charAt(j);
                        mml.append(c);
                        if ( c=='\n' && inPre && j<tagEnd-1 )
                            startPreLine(stack);
                    }
                    pos = tagEnd;
                    // newlines are not permitted before tag end
                    while ( mml.length()>0 && mml.charAt(mml.length()-1)=='\n')
                        mml.setLength(mml.length()-1);
                    mml.append( stack.pop().text );
                }
                // 2. insert intervening text
                boolean inPre =isInPre(stack);
                for ( int j=pos;j<start;j++ )
                {
                    char c = text.charAt(j);
                    if ( c == '\n' )
                    {
                        if ( mml.length()==0||mml.charAt(mml.length()-1)!='\n')
                            mml.append(c);
                        if ( inPre )
                        {
                            startPreLine(stack);
                        }
                    }
                    else
                        mml.append(c);
                }
                // 3. insert new start tag
                normaliseNewlines(startTag);
                mml.append(startTag);
                stack.push(new EndTag(start+len.intValue(),endTag,def));
            }
            else
                System.out.println("Ignoring tag "+name);
            offset += relOff.intValue();
        }
        //empty stack
        int pos = offset;
        while ( !stack.isEmpty() )
        {
            int tagEnd = stack.peek().offset;
            boolean inPre = isInPre( stack );
            if ( inPre && mml.charAt(mml.length()-1) == '\n' )
                startPreLine( stack );
            for ( int j=pos;j<tagEnd;j++ )
            {
                char c = text.charAt(j);
                mml.append(c);
                if ( c=='\n' && inPre && j<tagEnd-1 )
                    startPreLine(stack );
            }
            pos = tagEnd;
            // newlines are not permitted before tag end
            while ( mml.length()>0 && mml.charAt(mml.length()-1)=='\n')
                mml.setLength(mml.length()-1);
            mml.append( stack.pop().text );
        }
    }
    /**
     * Get the short form of the full docid
     * @return language/author/work only
     */
    String shortenDocID(String longDocId)
    {
        String[] parts = docid.split("/");
        if ( parts.length <= 3 )
            return docid;
        else
            return parts[0]+"/"+parts[1]+"/"+parts[2];
    }
    /**
     * Debug: print the invert index (properties to definitions)
     */
    private void printInvertIndex()
    {
        Set<String> keys = invertIndex.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            System.out.println(key+","+invertIndex.get(key).toJSONString());
        }
    }
    /**
     * Get a cortex or corcode from the scratch collection
     * @param docId the docid of the resource
     * @param version the version to fetch
     * @param cortex is this a cortex? if false corcode is assumed
     * @return null if not there else the AeseVersion of the resource
     * @throws DbException 
     */
    AeseVersion getScratchVersion( String docId, String version, 
        boolean cortex ) throws DbException
    {
        try
        {
            Connection conn = Connector.getConnection();
            String[] docids = conn.listDocuments(Database.SCRATCH, docid+".*", 
                JSONKeys.DOCID );
            for ( int i=0;i<docids.length;i++ )
            {
                String jDoc = conn.getFromDb(Database.SCRATCH, docids[i] );
                JSONObject jObj = (JSONObject) JSONValue.parse(jDoc);
                if ( docids[i].equals(docId)
                    && (cortex&&DocType.classifyObj(jObj)==DocType.CORTEX)
                    ||(!cortex&&DocType.classifyObj(jObj)==DocType.CORCODE) )
                {
                    AeseVersion av = new AeseVersion();
                    String format = (String)jObj.get(JSONKeys.FORMAT);
                    if ( format == null )
                        format = "TEXT";
                    av.setFormat( format );
                    String style = (String)jObj.get(JSONKeys.STYLE);
                    if ( style == null )
                        style = "TEI/default";
                    av.setStyle( style );
                    String body = (String)jObj.get(JSONKeys.BODY);
                    String encoding = (String)jObj.get(JSONKeys.ENCODING);
                    if ( encoding == null )
                        encoding = "UTF-8";
                    av.setVersion1( version );
                    av.setVersion(body.getBytes(encoding));
                    return av;
                }
            }
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
        return null;
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
            docid = request.getParameter(Params.DOCID);
            if ( docid == null )
                throw new Exception("You must specify a docid parameter");
            version1 = request.getParameter(Params.VERSION1);
            AeseVersion cortex, corcode;
            cortex = getScratchVersion( docid, version1, true );
            corcode = getScratchVersion( docid, version1+"/default", false );
            if ( cortex == null || corcode == null )
            {
                corcode = doGetResourceVersion( Database.CORCODE, 
                    docid+"/default", version1 );
                cortex = doGetResourceVersion( Database.CORTEX, 
                    docid, version1 );
            }
            String shortID = shortenDocID(docid);
            String dialectStr = getDialect( shortID, version1 );
            this.dialect = (JSONObject)JSONValue.parse(dialectStr);
            invertDialect();
            //printInvertIndex();
            createMML(cortex,corcode);
            response.setContentType("text/plain");
            response.setCharacterEncoding(encoding);
            response.getWriter().println(mml.toString());
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
    /**
     * Is this piece of text JSON?
     * @param text the text to test
     * @return true if it was else false
     */
    private static boolean isJSON( String text )
    {
        try
        {
            Object res = JSONValue.parse(text);
            if ( res==null )
                return false;
        }
        catch ( Exception e )
        {
            return false;
        }
        return true;
    }
    /**
     * Get a dialect
     * @param docid the docid of the dialect
     * @param version1 the version id that may specify a dialect variant
     * @return an Element (div) containing the content
     */
    public static String getDialect( String docid, String version1 ) 
        throws MMLTestException
    {
        try
        {
            Connection conn = Connector.getConnection();
            String path = docid+version1;
            String dialect = conn.getFromDb(Database.DIALECTS,path);
            while ( path.length()>0 && dialect == null )
            {
                path = Utils.chomp( path );
                String bson = conn.getFromDb(Database.DIALECTS,path);
                if ( bson != null )
                {
                    JSONObject jObj = (JSONObject)JSONValue.parse(bson);
                    dialect = (String)jObj.get(JSONKeys.BODY);
                }
            }
            if ( dialect == null )
                throw new MMLException("No dialect for "+docid+" found");
            else
                return dialect;
        }
        catch ( Exception e )
        {
            throw new MMLTestException(e);
        }
    }
}
