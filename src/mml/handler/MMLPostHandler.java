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

import mml.exception.*;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.handler.json.STILDocument;
import mml.handler.json.Range;
import mml.constants.Params;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/**
 * Handle a POST request
 * @author desmond
 */
public class MMLPostHandler extends MMLHandler
{
    InetAddress poster;
    String html;
    StringBuilder sb;
    STILDocument stil;
    JSONObject dialect;
    String langCode;
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
    private void parseBody( Element body ) throws JSONException
    {
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
            // get required docid
            docid = request.getParameter(Params.DOCID);
            if ( docid == null || docid.length()==0 )
                throw new MMLSaveException("Save where? No docid, mate");
            // get required dialect
            String d = request.getParameter(Params.DIALECT);
            if ( d == null )
                throw new MMLSaveException("Save how? Supply a dialect");
            JSONObject jv = (JSONObject) JSONValue.parse(d);
            if ( jv.get("language") != null )
                this.langCode = (String)jv.get("language");
            this.dialect = jv;
            html = request.getParameter(Params.HTML);
            if ( html == null )
                throw new MMLSaveException("Save what? Supply some HTML!");
            // get optional encoding if present
            String enc = request.getParameter(Params.ENCODING);
            if ( enc != null && enc.length()>0 )
                this.encoding = enc;
            sb = new StringBuilder();
            Document doc = Jsoup.parseBodyFragment(html);
            Element body = doc.body();  
            parseBody( body );
            // to do: send the text and STIL to the database
            System.out.println(sb.toString());
            System.out.println(dialect);
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
