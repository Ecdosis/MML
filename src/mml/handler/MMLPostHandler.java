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
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import calliope.AeseSpeller;
import mml.database.*;

import mml.exception.*;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.handler.json.STILDocument;
import mml.handler.json.Range;
import mml.handler.mvd.Archive;
import mml.constants.Params;
import mml.constants.Database;
import mml.constants.Formats;
import mml.handler.json.Dialect;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/**
 * Handle a POST request
 * @author desmond
 */
public class MMLPostHandler extends MMLHandler
{
    AeseSpeller speller;
    InetAddress poster;
    String html;
    StringBuilder sb;
    STILDocument stil;
    JSONObject dialect;
    String langCode;
    String author;
    String title; 
    String style; 
    String format;
    String section;
    String version1;
    String docid;
    /**
     * Create a POST handler for HTML that used to be MML
     */
    public MMLPostHandler()
    {
        encoding = "UTF-8";
        langCode = Locale.getDefault().getLanguage();
        stil = new STILDocument();
    }
    /**
     * Parse a div (section)
     * @param div the div
     * @throws JSONException 
     */
    private void parseDiv( Element div ) throws JSONException
    {
        List<Node> children = div.childNodes();
        int offset = sb.length();
        for ( Node child: children )
        {
            if ( child instanceof Element )
            {
                String nName = child.nodeName().toLowerCase();
                if ( nName.equals("p") )
                    parsePara( (Element)child, "p" );
                else if ( nName.matches("(h|H)\\d") )
                    parsePara( (Element) child, nName );
                else if ( child.nodeName().toLowerCase().equals("span") )
                    parseSpan( (Element)child );
                else
                    parseOtherElement((Element)child);
            }
        }
        sb.append("\n\n");
        int len = sb.length()-offset;
        String name = div.attr("class");
        if ( name == null )
            name = "section";
        Range r = new Range( name, offset, len );
        stil.add( r );
    }
    /**
     * May happen but should not
     * @param elem an element that is not a span, p or div
     */
    private void parseOtherElement( Element elem )
    {
        List<Node> children = elem.childNodes();
        for ( Node child: children )
        {
            if ( child instanceof Element )
                parseOtherElement( (Element)child );
            else if ( child instanceof TextNode )
                sb.append( ((TextNode)child).text() );
        }
    }
    /**
     * Parse a paragraph. These are always "p" elements, often with classes
     * @param p the paragraph element from the document fragment
     * @param defaultName the default name for the property
     */
    private void parsePara( Element p, String defaultName ) throws JSONException
    {
        List<Node> children = p.childNodes();
        int offset = sb.length();
        for ( Node child: children )
        {
            if ( child instanceof Element )
            {
                String nName = child.nodeName().toLowerCase();
                if ( nName.equals("span") )
                    parseSpan( (Element)child );
                else
                    parseOtherElement((Element)child);
            }
            else if ( child instanceof TextNode )
            {
                TextNode tn = (TextNode)child;
                sb.append(tn.text());
            }
        }
        sb.append("\n");
        int len = sb.length()-offset;
        String name = p.attr("class");
        if ( name == null )
            name = defaultName;
        Range r = new Range( name, offset, len );
        stil.add( r );
    }
    /**
     * Check if the span name is a milestone
     * @param name the name of the milestone property
     * @return true if there is a prop in the milestones called name
     */
    boolean isMilestone( String name )
    {
        Object obj = dialect.get("milestones");
        if ( obj != null )
        {
            JSONArray milestones = (JSONArray)obj;
            for ( Object milestone : milestones )
            {
                if ( milestone instanceof JSONObject )
                {
                    JSONObject m = (JSONObject)milestone;
                    if ( m.containsKey("prop") && ((String)m.get("prop")).equals(name) )
                        return true;
                }
            }
        }
        return false;
    }
    /**
     * Get the text of the element
     * @param elem the element in question
     * @return 
     */
    String getTextOf( Node elem )
    {
        if ( elem instanceof TextNode )
            return ((TextNode)elem).text();
        else if ( elem instanceof Element )
        {
            String nName = elem.nodeName().toLowerCase();
            // skip milestones
            if ( nName.equals("span") 
                && ((Element)elem).attr("class") != null 
                && isMilestone(((Element)elem).attr("class")) )
                return getTextOf(elem.nextSibling());
            else
            {
                List<Node> children = elem.childNodes();
                StringBuilder concat = new StringBuilder();
                for ( Node child: children )
                {
                    concat.append(getTextOf(child));
                }
                return concat.toString();
            }
        }
        else
            return "";
    }
    /**
     * Get the next word AFTER the given element
     * @param span the element span) after which we seek the next word
     * @return the word
     */
    String nextWord( Element span )
    {
        StringBuilder word = new StringBuilder();
        Node next = span.nextSibling();
        String text = "";
        if ( next != null )
        {
            if ( next instanceof TextNode )
            {
                text = ((TextNode)next).text();
            }
            else if ( next instanceof Element )
            {
                text = getTextOf(next);
            }
        }
        text = text.trim();
        for ( int i=0;i<text.length();i++ )
            if ( !Character.isWhitespace(text.charAt(i)) )
                word.append(text.charAt(i));
            else
                break;
        return word.toString();
    }
    /**
     * Remove leading and trailing punctuation
     * @param input the raw string
     * @param leading true if this is the first word of a hyphenated pair
     * @return the trimmed string
     */
    private String clean( String input, boolean leading )
    {
        int start = 0;
        while ( start < input.length() )
            if ( !Character.isLetter(input.charAt(start)) )
                start++;
            else
                break;
        int end = input.length()-1;
        while ( end >= 0 )
            if ( !Character.isLetter(input.charAt(end)) )
                end--;
            else
                break;
        // reset start or end after internal punctuation
        if ( leading )
        {
            for ( int i=start;i<=end;i++ )
            {
                if ( !Character.isLetter(input.charAt(i)) )
                    start = i+1;
            }
        }
        else
        {
            for ( int i=end;i>=start;i-- )
            {
                if ( !Character.isLetter(input.charAt(i)) )
                    end = i-1;
            }
        }
        return (start<=end)?input.substring(start,end+1):"";
    }
    /**
     * Parse a span with a class or not
     * @param span the span in HTML
     */
    private void parseSpan( Element span ) throws JSONException
    {
        if ( span.hasText() )
        {
            if ( span.hasAttr("class") )
            {
                String name = span.attr("class");
                int offset = sb.length();
                this.sb.append( span.text() );
                if ( isMilestone(name) )
                {
                    if ( sb.charAt(offset)!='\n' && offset >0 )
                        sb.insert(offset,'\n');
                    this.sb.append("\n");
                }
                else if ( name.equals("soft-hyphen") )
                {
                    int i = sb.length()-1;
                    while ( i > 0 && !Character.isWhitespace(sb.charAt(i)) )
                        i--;
                    if ( i > 0 )
                        i++;
                    String prev = clean(sb.substring(i),true);
                    String next = clean(nextWord(span),false);  
                    if ( this.speller.isHardHyphen(prev,next) )
                        name = "hard-hyphen";
                }
                int len = sb.length()-offset;
                Range r = new Range(name, offset, len);
                stil.add( r );
            }
            else
                sb.append( span.text() );
        }
        // else strangely no text: ignore it
    }
    /**
     * Parse the body of the HTML fragment
     * @param body should be contents of the target div in the editor
     * @throws JSONException 
     */
    private void parseBody( Element body ) throws MMLSaveException
    {
        try
        {
            this.speller = new AeseSpeller( this.langCode );
            this.sb = new StringBuilder();
            if ( body.nodeName().toLowerCase().equals("div") )
                parseDiv( body );
            else
            {
                List<Node> children = body.childNodes();
                for ( Node child: children )
                {
                    if ( child instanceof Element )
                    {
                        String nName = child.nodeName().toLowerCase();
                        if ( nName.equals("div") )
                            parseDiv( (Element)child);
                        else if ( nName.equals("p") )
                            parsePara((Element)child,"p");
                        else if ( nName.equals("span") )
                            parseSpan( (Element) child );
                        else if ( nName.matches("(h|H)\\d") )
                            parsePara( (Element) child, nName );
                        else
                            parseOtherElement( (Element)child );

                    }
                   // else it is insignificant white space
                }
            }
            this.speller.cleanup();
        }
        catch ( Exception e )
        {
            if ( this.speller != null )
                this.speller.cleanup();
            throw new MMLSaveException( e );
        }
    }
    /**
     * Load one param
     * @param request the request to read from
     * @param key the param key
     * @param dflt the default value or null
     * @param reqdMessage if not null the param is required 
     * @return the param value
     * @throws MMLSaveException if it was required and not present
     */
    private String checkParam( HttpServletRequest request, String key, 
        String dflt, String reqdMessage ) throws MMLSaveException
    {
        String value = request.getParameter(key);
        if ( value == null || value.length()==0 )
        {
            if ( reqdMessage != null )
                throw new MMLSaveException( reqdMessage );
            else
                value = dflt;
        }
        return value;
    }
    /**
     * Read all the params you can from the request
     * @param request the current request
     * @throws MMLSaveException 
     */
    private void readParams( HttpServletRequest request ) 
        throws MMLSaveException
    {
        this.docid = checkParam( request,Params.DOCID, null,
            "Save where? No docid, mate");
        String json = checkParam( request, Params.DIALECT, null,
            "Save how? Supply a dialect");
        JSONObject jv = (JSONObject) JSONValue.parse(json);
        if ( jv.get("language") != null )
            this.langCode = (String)jv.get("language");
        this.dialect = jv;
        html = checkParam( request, Params.HTML, null,
            "Save what? Supply some HTML!");
        this.encoding = checkParam(request,Params.ENCODING,"UTF-8",null);
        this.author = checkParam( request, Params.AUTHOR, "Anon", null );
        this.title = checkParam( request, Params.TITLE, "Untitled", null );
        this.style = checkParam( request, Params.STYLE,"TEI/default",null); 
        this.format = checkParam( request, Params.FORMAT, "MVD/TEXT",null);
        this.section = checkParam( request, Params.SECTION, "", null);
        this.version1 = checkParam( request, Params.VERSION1, "/Base/first", null);
    }
    /**
     * Add the archive to the database
     * @param archive the archive
     * @param db cortex or corcode
     * @throws MMLException 
     */
    protected void addToDBase( Archive archive, String db, StringBuilder log ) 
        throws MMLException
    {
        // now get the json docs and add them at the right docid
        if ( !archive.isEmpty() )
        {
            String path = new String(docid);
            if ( db.equals("corcode") )
                path += "/default";
            Connector.getConnection().putToDb( db, path, 
                archive.toResource(db) );
            log.append( archive.getLog() );
        }
        else
            log.append("No "+db+" created (empty)\n");
    }
    /**
     * Reduce the docid to the language/author/work triple
     * @return a string
     */
    String baseDocID()
    {
        String[] parts = docid.split("/");
        if ( parts.length >= 3 )
            return parts[0]+"/"+parts[1]+"/"+parts[2];
        else
            return docid;
    }
    /**
     * Handle a POST request
     * @param request the raw request
     * @param response the response we will write to
     * @param urn the rest of the URL after stripping off the context
     * @throws MMLException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            readParams( request);
            sb = new StringBuilder();
            Document doc = Jsoup.parseBodyFragment(html);
            Element body = doc.body();  
            parseBody( body );
            // send the text,STIL and dialect to the database
            Archive cortex = new Archive(title, 
                this.author,Formats.MVD_TEXT,encoding);
            cortex.put( version1, html.getBytes(encoding) );
            Archive corcode = new Archive(title, 
                this.author,Formats.MVD_STIL,encoding);
            cortex.setStyle( style );
            corcode.setStyle( style );
            corcode.put( version1, stil.toString().getBytes(encoding) );
            StringBuilder log = new StringBuilder();
            addToDBase( cortex, Database.CORTEX, log );
            addToDBase( corcode, Database.CORCODE, log );
            String baseid = baseDocID();
            String oldDialect = Connector.getConnection().getFromDb(
                Database.DIALECTS, baseid );
            if ( oldDialect == null || !Dialect.compare(dialect,
                (JSONObject)JSONValue.parse(oldDialect)) )
                log.append( Connector.getConnection().putToDb(Database.DIALECTS, 
                    baseid, dialect.toJSONString()));
            System.out.println(log.toString() );
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
