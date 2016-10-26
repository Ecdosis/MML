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

package mml.handler.post;

import calliope.AeseSpeller;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connection;
import calliope.core.exception.DbException;
import mml.constants.Formats;
import mml.constants.Params;
import calliope.core.database.Connector;
import mml.exception.JSONException;
import mml.exception.MMLException;
import mml.exception.MMLSaveException;
import mml.handler.json.Range;
import mml.handler.mvd.Archive;
import mml.Autosave;
import mml.handler.json.STILDocument;
import mml.handler.scratch.ScratchVersionSet;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import java.util.HashSet;

/**
 * Handle POST events. Mostly saves.
 * @author desmond
 */
public class MMLPostHTMLHandler extends MMLPostHandler
{
    static HashSet<String> milestones;
    static
    {
        milestones = new HashSet<String>();
        milestones.add("page");
        // add more milestone keywords here
    }
    boolean prevWasMilestone;
    void parseRequest( HttpServletRequest request ) throws FileUploadException, 
        Exception
    {
        if ( ServletFileUpload.isMultipartContent(request) )
        {
            DiskFileItemFactory factory = new DiskFileItemFactory();
            ServletFileUpload upload = new ServletFileUpload(factory);
            upload.setHeaderEncoding(encoding);
            List<FileItem> items = upload.parseRequest(request);
            for ( int i=0;i<items.size();i++ )
            {
                FileItem item = (FileItem) items.get( i );
                if ( item.isFormField() )
                {
                    String fieldName = item.getFieldName();
                    if ( fieldName != null )
                    {
                        String contents = item.getString(encoding);
                        if ( fieldName.equals(Params.DOCID) )
                            this.docid = contents;
                        else if ( fieldName.equals(Params.DIALECT) )
                        {
                            JSONObject jv = (JSONObject) JSONValue.parse(contents);
                            if ( jv.get("language") != null )
                                this.langCode = (String)jv.get("language");
                            this.dialect = jv;
                        }
                        else if ( fieldName.equals( Params.HTML ) )
                        {
                            html = contents;
                        }
                        else if ( fieldName.equals(Params.ENCODING) )
                            encoding = contents;
                        else if ( fieldName.equals(Params.AUTHOR) )
                            author = contents;
                        else if ( fieldName.equals(Params.TITLE) )
                            title = contents;
                        else if ( fieldName.equals(Params.STYLE) )
                            style = contents;
                        else if ( fieldName.equals(Params.FORMAT) )
                            format = contents;
                        else if ( fieldName.equals(Params.SECTION) )
                            section = contents;
                        else if ( fieldName.equals(Params.VERSION1) )
                            version1 = contents;
                        else if ( fieldName.equals(Params.DESCRIPTION) )
                            description = contents;
                    }
                }
                // we're not uploading files
            }
            if ( encoding == null )
                encoding = "UTF-8";
            if ( author == null )
                author = "Anon";
            if ( style == null )
                style = "TEI/default";
            if ( format == null )
                format = "MVD/TEXT";
            if ( section == null )
                section = "";
            if ( version1 == null )
                version1 = "/Base/first";
            if ( description == null )
                description = "Version "+version1;
            if ( docid == null )
                throw new Exception("missing docid");
            if ( html == null )
                throw new Exception( "Missing html");
            if ( dialect == null )
                throw new Exception("Missing dialect");
        }       
    }
    /**
     * Parse a paragraph. These may be "p" or "hN" elements, often with classes
     * @param p the paragraph/heading element from the document fragment
     * @param defaultName the default name for the property
     */
    private void parsePara( Element p, String defaultName ) throws JSONException
    {
        List<Node> children = p.childNodes();
        String name = p.attr("class");
        if ( name == null || name.length()==0 )
            name = defaultName;
        if ( isLineFormat(name) || prevWasMilestone )
            ensure(1,false);
        else
            ensure(2,true);
        int offset = sb.length();
        Range r = new Range( name, offset, 0 );
        stil.add( r );
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
                sb.append(tn.getWholeText());
            }
        }
        if ( isLineFormat(name) )
            ensure(1,true);
        else
            ensure(2,true);
        this.stil.updateLen(r,sb.length()-offset);
        prevWasMilestone = false;
    }
    /**
     * May happen but should not
     * @param elem an element that is not a span, p or div
     */
    private void parseOtherElement( Element elem ) throws JSONException
    {
        List<Node> children = elem.childNodes();
        int offset = sb.length();
        String name = elem.attr("class");
        if ( name == null || name.length()==0 )
            name = elem.nodeName();
        Range r = new Range( name, offset, 0 );
        stil.add( r );
        for ( Node child: children )
        {
            if ( child instanceof Element )
                parseOtherElement( (Element)child );
            else if ( child instanceof TextNode )
                sb.append( ((TextNode)child).getWholeText() );
        }
        this.stil.updateLen(r,sb.length()-offset);
        prevWasMilestone = false;
    }
    /**
     * Ensure that there are at least a given number of NLs
     * @param nNLs the number of newlines that must be at the end of sb
     * @param erase true if we are allowed to erase existing NLs
     */
    private void ensure( int nNLs, boolean erase )
    {
        int nExisting = 0;
        if ( sb.length()>0 )
        {
            char c = sb.charAt(sb.length()-1);
            int index = 1;
            while ( index<=sb.length() && (c == 10 || c == 13) ) 
            {
                nExisting++;
                index++;
                c = sb.charAt(sb.length()-index);
            }
        }
        if ( nNLs>nExisting )
        {
            for ( int i=0;i<nNLs-nExisting;i++ )
                sb.append("\n");
        }
        else if ( erase && nNLs < nExisting )
        {
            sb.setLength(sb.length()-(nExisting-nNLs));
        }
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
        String name = div.attr("class");
        if ( name == null||name.length()==0 )
            name = "section";
        Range r = new Range( name, offset, 0 );
        stil.add( r );
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
                else if ( nName.equals("pre") )
                    parsePre( (Element) child );
                else
                    parseOtherElement((Element)child);
            }
        }
        ensure(3,true);
        this.stil.updateLen(r,sb.length()-offset);
        prevWasMilestone = false;
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
     * Check if the span name is a line format
     * @param name the name of the milestone property
     * @return true if there is a prop in the milestones called name
     */
    boolean isMilestone( String name )
    {
        return milestones.contains(name);
    }
    boolean isLineFormat( String name )
    {
        JSONArray lfs = (JSONArray)dialect.get("lineformats");
        for ( int i=0;i<lfs.size();i++ )
        {
            JSONObject lf = (JSONObject)lfs.get(i);
            if ( name.equals((String)lf.get("prop")) )
                return true;
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
            return ((TextNode)elem).getWholeText();
        else if ( elem instanceof Element )
        {
            String nName = elem.nodeName().toLowerCase();
            // skip milestones
            if ( nName.equals("span") 
                && ((Element)elem).attr("class") != null 
                && isMilestone(((Element)elem).attr("class")) )
            {
                int offset = sb.length();
                String name = ((Element)elem).attr("class");
                Range r = new Range( name, offset, 0 );
                try
                {
                    pages.add(r);
                }
                catch ( JSONException e )
                {
                }
                return getTextOf(elem.nextSibling());
            }
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
                text = ((TextNode)next).getWholeText();
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
     * Parse a codeblock
     * @param elem the element to parse
     * @throws a JSON exception
     */
    private void parsePre( Element elem ) throws JSONException
    {
        if ( elem.hasText() )
        {
            int offset = sb.length();
            String name = elem.attr("class");
            if ( name == null||name.length()==0 )
                name = "pre";
            Range r = new Range( name, offset, 0 );
            stil.add( r );
            if ( elem.hasAttr("class") )
            {
                List<Node> children = elem.childNodes();
                for ( Node child: children )
                {
                    if ( child instanceof Element )
                    {
                        if ( child.nodeName().equals("span") )
                            parseSpan( (Element)child );
                        else
                            parseOtherElement( (Element)child );
                    }
                    else if ( child instanceof TextNode )
                        sb.append( ((TextNode)child).getWholeText() );
                }
            }
            else
                sb.append( elem.text() );
            this.stil.updateLen(r,sb.length()-offset);
        }
        prevWasMilestone = false;
        ensure(1,false);
    }
    /**
     * Parse a span with a class or not
     * @param span the span in HTML
     */
    private void parseSpan( Element span ) throws JSONException
    {
        if ( span.hasText() )
        {
            int offset = sb.length();
            String name = span.attr("class");
            if ( name == null||name.length()==0 )
                name = "span";
            Range r = new Range( name, offset, 0 );
            if ( span.hasAttr("class") )
            {
                name = span.attr("class");
                this.sb.append( span.text() );
                if ( isMilestone(name) )
                {
                    pages.add(r);
                    this.sb.append("\n");
                    pages.updateLen(r,sb.length()-offset);
                    prevWasMilestone = true;
                }
                else if ( name.equals("soft-hyphen") )
                {
                    stil.add(r);
                    int i = sb.length()-1;
                    while ( i > 0 && !Character.isWhitespace(sb.charAt(i)) )
                        i--;
                    if ( i > 0 )
                        i++;
                    String prev = clean(sb.substring(i),true);
                    String next = clean(nextWord(span),false);  
                    if ( this.speller.isHardHyphen(prev,next) )
                        name = "hard-hyphen";
                    stil.updateLen(r,sb.length()-offset);
                }
                else
                {
                    if ( isLineFormat(name) )
                        ensure(1,false);
                    stil.add( r );
                    stil.updateLen(r,sb.length()-offset);
                }
            }
            else
            {
                stil.add(r);
                sb.append( span.text() );
                stil.updateLen(r,sb.length()-offset);
            }
        }
        // else strangely no text: ignore it
    }
    /**
     * Parse the body of the HTML fragment
     * @param body should be contents of the target div in the editor
     * @throws JSONException 
     */
    protected void parseBody( Element body ) throws MMLSaveException
    {
        try
        {
            this.speller = new AeseSpeller( this.langCode );
            this.sb = new StringBuilder();
            String style = ScratchVersionSet.getDefaultStyleName(this.docid);
            stil = new STILDocument(style);
            pages = new STILDocument(style);
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
                        else if ( nName.equals("pre") )
                            parsePre( (Element) child );
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
     * Add the archive to the database
     * @param archive the archive
     * @param db cortex or corcode
     * @throws MMLException 
     */
    protected void addToDBase( Archive archive, String db, StringBuilder log ) 
        throws MMLException
    {
        try
        {
            // now get the json docs and add them at the right docid
            if ( !archive.isEmpty() )
            {
                String path = new String(docid);
                if ( db.equals(Database.CORCODE) )
                    path += "/default";
                Connector.getConnection().putToDb( db, path, 
                    archive.toResource(db) );
                log.append( archive.getLog() );
            }
            else
                log.append("No "+db+" created (empty)\n");
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
    /**
     * Save the cortex to the scratch collection
     * @param log record message here
     */
    private void saveCortex( StringBuilder log ) throws DbException
    {
        try
        {
            // send the text,STIL and dialect to the database
            if ( description == null )
                description = "Version "+this.version1+" of "+docid;
            Archive cortex = new Archive(Formats.TEXT, description);
            cortex.addLongName( version1, description );
            if ( style != null )
                cortex.setStyle( style );
            cortex.put( version1, sb.toString().toCharArray() );
            Connection conn = Connector.getConnection();
            String res = conn.getFromDb(Database.SCRATCH,docid);
            if ( res != null )
                conn.removeFromDb(Database.SCRATCH,docid);
            addToDBase( cortex, Database.SCRATCH, log );
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
    }
    /**
     * Save the corcode to the temporary scratch collection
     * @throws DbException 
     */
    private void saveCorcode( StringBuilder log ) throws DbException
    {
        try
        {
            // repeat for corcode
            if ( description == null )
                description = "Version "+this.version1+" of "+docid;
            Archive corcode = new Archive(Formats.STIL,description);
            if ( description != null )
                corcode.addLongName( version1, description );
            corcode.setStyle( style );
            corcode.put( version1, stil.toString().toCharArray() );
            Connection conn = Connector.getConnection();
            String ccDocId = docid+"/default";
            String res = conn.getFromDb(Database.SCRATCH,ccDocId);
            if ( res != null )
                conn.removeFromDb(Database.SCRATCH,ccDocId);
            addToDBase( corcode, Database.SCRATCH, log );
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
    }
    /**
     * Write metadata to scratch space
     * @param log track log messages here
     * @throws MMLException 
     */
    void saveMetadata( StringBuilder log ) throws MMLException
    {
        try
        {
            Connection conn = Connector.getConnection();
            String md = conn.getFromDb(Database.SCRATCH, this.docid );
            if ( md == null )
            {
                JSONObject metadata = new JSONObject();
                metadata.put(JSONKeys.AUTHOR,this.author);
                metadata.put(JSONKeys.DOCID,this.docid);
                metadata.put(JSONKeys.ENCODING,this.encoding);
                metadata.put(JSONKeys.SECTION,this.section);
                metadata.put(JSONKeys.TITLE,this.title);
                metadata.put(JSONKeys.VERSION1,this.version1);
                md = metadata.toJSONString();
                log.append(conn.putToDb(Database.SCRATCH,this.docid,md));
            }
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
    /**
     * Handle the request by writing everything out to scratch space
     * @param request
     * @param response
     * @param urn
     * @throws MMLException 
     */
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            parseRequest( request );
            StringBuilder log = new StringBuilder();
            Document doc = Jsoup.parseBodyFragment(html);
            Element body = doc.body();  
            parseBody( body );
            int totalWait = 0;
            while ( Autosave.inProgress && totalWait < 100000 )
            {
                Thread.sleep(4000);
                totalWait += 400;
            }
            if ( totalWait >= 100000 )
                throw new DbException("Save timed out");
            if ( !Autosave.inProgress )
            {
                Autosave.lock = true;
                saveCortex(log);
                saveCorcode(log);
                saveMetadata(log);
                Autosave.lock = false;
            }
            System.out.println( log.toString() );
        }
        catch ( Exception e )
        {
            Autosave.lock = false;
            throw new MMLException(e);
        }
    }
}
