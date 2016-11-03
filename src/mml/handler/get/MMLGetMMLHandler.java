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
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.constants.Database;
import mml.constants.Params;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.JSONKeys;
import calliope.core.json.corcode.Range;
import calliope.core.json.corcode.Annotation;
import mml.exception.*;
import mml.handler.scratch.Scratch;
import mml.handler.scratch.ScratchVersion;
import mml.handler.scratch.ScratchLayer;
import calliope.core.Utils;
import mml.handler.json.DialectKeys;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.HashSet;
import org.json.simple.JSONValue;

/**
 * Get an MML representation of a file
 * @author desmond
 */
public class MMLGetMMLHandler extends MMLGetHandler
{
    /** reverse index property-names to their MML definitions */
    HashMap<String,JSONObject> invertIndex;
    HashMap<Character,String> globals;
    JSONObject dialect;
    StringBuilder mml;
    HashSet<String> lineFormats;
    
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
            case charformats:
                if ( defn.containsKey("tag") )
                    return (String)defn.get("tag");
                else
                    return (String)defn.get("leftTag");
            case lineformats:
                return (String)defn.get("leftTag");
            case paraformats:
                return (String)defn.get("leftTag");
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
            case headings:
                StringBuilder sb = new StringBuilder();
                for ( int i=0;i<len-1;i++ )
                    sb.append(defn.get("tag"));
                return "\n"+sb.toString()+"\n\n";
            case charformats:
                if ( defn.containsKey("tag") )
                    return (String)defn.get("tag");
                else
                    return (String)defn.get("rightTag");
            case lineformats:
                return (String)defn.get("rightTag")+"\n";
            case paraformats:
                return (String)defn.get("rightTag")+"\n\n";
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
    void reduceGlobal( JSONObject jObj )
    {
        String seq = (String)jObj.get("seq");
        String rep = (String)jObj.get("rep");
        seq = seq.trim();
        rep = rep.trim();
        jObj.put( "seq", seq );
        jObj.put( "rep", rep);
    }
    /**
     * Make a reverse index of the dialect file
     */
    private void invertDialect() throws Exception
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
                    enterProp(value,keyword,"paragraph");
                    break;
                case headings: case charformats:
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
                    break;
                case lineformats: 
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject obj = (JSONObject)array.get(i);
                        enterProp(obj,keyword,"line");
                    }
                    break;
                case globals:
                    array = (JSONArray)value;
                    for ( int i=0;i<array.size();i++ )
                    {
                        JSONObject jObj = (JSONObject)array.get(i);
                        String rep = (String)jObj.get("rep");
                        if ( rep.length() != 1 )
                            reduceGlobal(jObj);
                        rep = (String)jObj.get("rep");
                        if ( rep.length() != 1 )
                            throw new Exception("Global replacement should be 1 char");
                        char repChar = rep.charAt(0);
                        globals.put(repChar,(String)jObj.get("seq"));
                    }
                    break;
                case smartquotes:
                    if ( ((Boolean)value).booleanValue() )
                    {
                        globals.put('‘',"'");
                        globals.put('’',"'");
                        globals.put('“',"\"");
                        globals.put('”',"\"");
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
     * Are we in a section governed by a line format?
     * @param stack the tag stack
     * @return true if it is true
     */
    boolean isLineFormat( Stack<EndTag> stack )
    {
        if ( !stack.isEmpty() )
        {
            EndTag top = stack.peek();
            if ( top != null )
            {
                String prop = (String) top.def.get("prop");
                return lineFormats.contains(prop);
            }
            else
                return false;
        }
        else return false;
    }
    /**
     * Insert the line-start and end for an internal lineformat
     * @param stack the tag stack to tell us the depth
     */
    void startPreLine( Stack<EndTag> stack )
    {
        JSONObject lf = stack.peek().def;
        String pre = (String)lf.get("leftTag");
        String post = (String)lf.get("rightTag");
        mml.append(post);
        mml.append("\n");
        mml.append(pre);
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
    private void addAbsoluteOffsets( JSONArray arr )
    {
        int offset = 0;
        for ( int i=0;i<arr.size();i++ )
        {
            JSONObject jObj = (JSONObject)arr.get(i);
            Number reloff = (Number)jObj.get(JSONKeys.RELOFF);
            offset += reloff.intValue();
            jObj.put( JSONKeys.OFFSET, offset );
        }
    }
    /**
     * Merge two corcode sets
     * @param cc1 the first corcode as a STIL JSON object
     * @param cc2 the second corcode as a STIL JSON object
     * @return a STIL object with the two merged arrays
     */
    JSONObject mergeCorcodes( JSONObject cc1, JSONObject cc2 )
    {
        JSONArray iArr = (JSONArray)cc1.get("ranges");
        JSONArray jArr = (JSONArray)cc2.get("ranges");
        String style = (String)cc1.get(JSONKeys.STYLE);
        addAbsoluteOffsets( iArr );
        addAbsoluteOffsets( jArr );
        int i = 0;
        int j = 0;
        JSONArray all = new JSONArray();
        while ( i < iArr.size() || j < jArr.size() )
        {
            if ( i == iArr.size() )
                all.add(jArr.get(j++));
            else if ( j == jArr.size() )
                all.add(iArr.get(i++));
            else
            {
                JSONObject iObj = (JSONObject)iArr.get(i);
                JSONObject jObj = (JSONObject)jArr.get(j);
                int iOffset = ((Number)iObj.get(JSONKeys.OFFSET)).intValue();
                int jOffset = ((Number)jObj.get(JSONKeys.OFFSET)).intValue();
                if ( iOffset < jOffset )
                {
                    all.add( iObj );
                    i++;
                }
                else if ( jOffset < iOffset )
                {
                    all.add( jObj );
                    j++;
                }
                else    // equal
                {
                    int iLen = ((Number)iObj.get(JSONKeys.LEN)).intValue();
                    int jLen = ((Number)jObj.get(JSONKeys.LEN)).intValue();
                    if ( (iLen == 0 && jLen != 0) || (iLen > jLen) ) 
                    {
                        all.add( iObj);
                        i++;
                    }
                    else if ( (jLen == 0 && iLen != 0) || (jLen > iLen) )
                    {
                        all.add( jObj);
                        j++;
                    }
                    else 
                    {
                        all.add( iObj);
                        i++;
                    }
                }
            }
        }
        JSONObject combined = new JSONObject();
        combined.put(JSONKeys.STYLE,style);
        int prev = 0;
        for ( i=0;i<all.size();i++ )
        {
            JSONObject jObj = (JSONObject)all.get(i);
            int offset = ((Number)jObj.get(JSONKeys.OFFSET)).intValue();
            int reloff = offset - prev;
            prev = offset;
            jObj.remove(JSONKeys.OFFSET);
            jObj.put(JSONKeys.RELOFF,reloff);
        }
        combined.put(JSONKeys.RANGES,all);
        return combined;
    }
    /**
     * Count the numebr of newlines at the end of the MML text being built
     * @param sb the mml text
     * @return the number of terminal NLs
     */
    int countTerminalNLs( StringBuilder sb )
    {
        int nNLs = 0;
        for ( int i=sb.length()-1;i>0;i-- )
        {
            if ( sb.charAt(i) == '\n' )
                nNLs++;
            else
                break;
        }
        return nNLs;
    }
    /**
     * Create the MMLtext using the invert index and the cortex and corcode
     * @param cortex the plain text version
     * @param ccDflt the default STIL markup for that plain text
     * @param ccPages the page-breaks or null
     * @param layer the number of the layer to build
     */
    void createMML( ScratchVersion cortex, ScratchVersion ccDflt, 
        ScratchVersion ccPages, int layer )
    {
        String text = cortex.getLayerString(layer);
        mml = new StringBuilder();
        String stilDflt = ccDflt.getLayerString(layer);
        String stilPages = (ccPages==null)?null:ccPages.getLayerString(layer);
        JSONObject mDflt = (JSONObject)JSONValue.parse(stilDflt);
        if ( stilPages != null )
        {
            JSONObject mPages = (JSONObject)JSONValue.parse(stilPages);
            mDflt = mergeCorcodes(mDflt,mPages);
        }
        JSONArray ranges = (JSONArray)mDflt.get("ranges");
        Stack<EndTag> stack = new Stack<EndTag>();
        int offset = 0;
        for ( int i=0;i<ranges.size();i++ )
        {
            JSONObject r = (JSONObject)ranges.get(i);
            Number len = (Number)r.get("len");
            Number relOff = (Number)r.get("reloff");
            String name = (String)r.get("name");
            if ( invertIndex.containsKey(name) )
            {
                JSONObject def = invertIndex.get(name);
                String startTag = mmlStartTag(def,offset);
                String endTag = mmlEndTag(def,len.intValue());
                int start = offset+relOff.intValue();
                // 1. insert pending end-tags and text before current range
                int pos = offset;
                while ( !stack.isEmpty() && stack.peek().offset <= start )
                {
                    // check for NLs here if obj is of type lineformat
                    int tagEnd = stack.peek().offset;
                    boolean isLF = isLineFormat( stack );
                    for ( int j=pos;j<tagEnd;j++ )
                    {
                        char c = text.charAt(j);
                        if ( c!='\n' )
                        {
                            if ( globals.containsKey(c) )
                                mml.append(globals.get(c));
                            else
                                mml.append(c);
                        }
                        else if ( isLF && j<tagEnd-1 )
                            startPreLine(stack);
                        else
                            mml.append(c);
                    }
                    pos = tagEnd;
                    // newlines are not permitted before tag end
                    while ( mml.length()>0 && mml.charAt(mml.length()-1)=='\n')
                        mml.setLength(mml.length()-1);
                    mml.append( stack.pop().text );
                }
                // 2. insert intervening text
                boolean inPre = isLineFormat(stack);
                int nNLs = countTerminalNLs(mml);
                for ( int j=pos;j<start;j++ )
                {
                    char c = text.charAt(j);
                    if ( c == '\n' )
                    {
                        if ( mml.length()==0||nNLs==0 )
                            mml.append(c);
                        if ( nNLs > 0 )
                            nNLs--;
                        if ( inPre )
                            startPreLine(stack);
                    }
                    else
                    {
                        mml.append(c);
                        nNLs = 0;
                    }
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
            boolean inPre = isLineFormat( stack );
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
     * Build a quick lookup ltable for lineformats
     */
    void buildLineFormats()
    {
        JSONArray lfs = (JSONArray)this.dialect.get("lineformats");
        this.lineFormats = new HashSet<String>();
        for ( int i=0;i<lfs.size();i++ )
        {
            JSONObject lf = (JSONObject)lfs.get(i);
            String lfProp = (String) lf.get("prop");
            lineFormats.add(lfProp);
        }
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
            ScratchVersion cortex, corcodeDefault,corcodePages;
            cortex = Scratch.getVersion( docid, version1, Database.CORTEX );
            corcodeDefault = Scratch.getVersion( docid+"/default", version1, Database.CORCODE );
            corcodePages = Scratch.getVersion( docid+"/pages", version1, Database.CORCODE );
            String shortID = shortenDocID(docid);
            String dialectStr = getDialect( shortID, version1 );
            this.dialect = (JSONObject)JSONValue.parse(dialectStr);
            globals = new HashMap<Character,String>();
            buildLineFormats();
            invertDialect();
            //printInvertIndex();
            int[] layers = cortex.getLayerNumbers();
            Arrays.sort(layers);
            JSONObject jObj = new JSONObject();
            jObj.put( JSONKeys.VERSION1, version1 );
            JSONArray jArr = new JSONArray();
            jObj.put(JSONKeys.LAYERS,jArr);
            for ( int i=0;i<layers.length;i++ )
            {
                createMML(cortex,corcodeDefault,corcodePages,layers[i]);
                ScratchLayer sl = new ScratchLayer(mml.toString(),
                    ScratchVersion.simpleLayerName(layers[i]));
                jArr.add( sl.toJSONObject() );
            }
            response.setContentType("application/json");
            response.setCharacterEncoding(encoding);
            String jStr = jObj.toJSONString();
            response.getWriter().println(jStr);
            //System.out.println(jObj.toJSONString());
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
            String path = docid;
            if ( version1 != null && !version1.equals("/base") )
                path += version1;
            String dialect = conn.getFromDb(Database.DIALECTS,path);
            if ( dialect != null )
            {
                JSONObject jObj = (JSONObject)JSONValue.parse(dialect);
                dialect = (String)jObj.get(JSONKeys.BODY);
            }
            else
            {
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
            }
            if ( dialect == null )
                throw new MMLException("No dialect for "+path+" found");
            else
                return dialect;
        }
        catch ( Exception e )
        {
            throw new MMLTestException(e);
        }
    }
}
