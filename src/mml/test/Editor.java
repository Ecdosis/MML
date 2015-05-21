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

package mml.test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.*;
import mml.handler.get.MMLGetMMLHandler;
import html.*;
import java.io.File;
import java.io.FileInputStream;
import calliope.core.Utils;
import calliope.core.URLEncoder;
import mml.constants.Params;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

/**
 * Test interface for editor
 * @author desmond
 */
public class Editor extends Test
{
    String service;
    String host;
    String version1;
    String docid;
    String requestURL;
    String style;
    String title;
    String author;
    static String EDITOR_START_JS = 
        "jQuery( document ).ready(function() {";
    static String EDITOR_END_JS =
    "var editor = new MMLEditor(opts, dialect);\njQuery(window).load(fu"
    +"nction() {\n\t\teditor.recomputeImageHeights()\n}); \njQuery(\"#i"
    +"nfo\").click( function() {\n\t\teditor.toggleHelp();\n});\njQuery"
    +"(\"#save\").click( function() {\n\t\teditor.save();\n});\njQuery("
    +"\"#dropdown\").change( function() {\n\t\tvar parts = jQuery(\"#dr"
    +"opdown\").val().split(\"&\");\n\t\tfor ( var i=0;i<parts.len"
    +"gth;i++ ) {\n\t\t\tvar value = parts[i].split(\"=\");\n\t\t\t"
    +"if ( value.length== 2 )\n\t\t\t\tjQuery(\"#\"+value[0]).val(valu"
    +"e[1]);\n\t\t}\n\t\tjQuery(\"form\").submit();\n});\n}); \n";
    static String DEROBERTO_1920 = "docid=italian/deroberto/ivicere/cap1&version1="
        +"/Base/1920&title=I Vicerè&author=De Roberto";
    static String DEROBERTO_1894 = "docid=italian/deroberto/ivicere/cap1&version1="
        +"/Base/1894&title=I Vicerè&author=De Roberto";
    static String HARPUR_1883 = "docid=english/harpur/h642&version1=/h642j"
        +"&title=Poems&author=Harpur";
    public Editor()
    {
        super();
        this.encoding = "UTF-8";
    }
    /**
     * Ensure that an expected parameter has a sensible default
     * @param request the http request
     * @param param the parameter name
     * @param dflt its default value
     * @return the value assigned
     */
    String ensureParam( HttpServletRequest request, String param, String dflt )
    {
        String value = request.getParameter(param);
        if ( value == null || value.length()==0 )
            value = dflt;
        return value;
    }
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            this.requestURL = request.getRequestURL().toString();
            this.service = Utils.first(request.getRequestURI());
            this.host = request.getServerName();
            this.docid = ensureParam( request,Params.DOCID,
                "italian/deroberto/ivicere/cap1");
            this.version1 = ensureParam(request,Params.VERSION1,"/Base/1920");
            this.style = ensureParam(request,Params.STYLE,"italian/deroberto");
            this.title = ensureParam(request,Params.TITLE,"I Vicerè");
            this.author = ensureParam(request,Params.AUTHOR,"De Roberto");
            composePage();
            response.setContentType("text/html;charset=UTF-8");
            response.getWriter().println(doc);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
    /**
     * Read a utf-8 file and convert it to a string
     * @param name the local name of the file or path
     * @return a String being the file contents
     */
    String readFile( String name ) throws MMLTestException
    {
        try
        {
            File f = new File(name);
            byte[] data = new byte[(int)f.length()];
            FileInputStream fis = new FileInputStream( f );
            fis.read( data );
            fis.close();
            return new String( data, "UTF-8");
        }
        catch ( Exception e )
        {
            throw new MMLTestException(e);
        }
    }
    /**
     * Escape characters if the string was set to the value of an option
     * @param value the value of a select option
     * @return the value with <, >, " escaped (<=&lt;,>=&gt;,"=<q>)
     */
    private String escape( String value )
    {
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<value.length();i++ )
        {
            char c = value.charAt(i);
            if ( c == '"' )
                sb.append("<q>");
            else if ( c == '<' )
                sb.append("&lt;");
            else if ( c == '>')
                sb.append("&gt;");
            else
                sb.append(c);
        }
        return sb.toString();
    }
    /**
     * Add a single option-group to the styles menu
     * @param select the selct element
     * @param dObj the dialect JSON object
     * @param name the name of the format collection in dObj
     * @param label the name of the optgroup to appear in menu
     */
    private void addOptGroup( Element select, JSONObject dObj, String name, 
        String label )
    {
        JSONArray items = (JSONArray)dObj.get(name);
        if ( items != null )
        {
            Element group = new Element("optgroup");
            group.addAttribute("label",label);
            for ( int i=0;i<items.size();i++ )
            {
                JSONObject item = (JSONObject)items.get(i);
                Element option = new Element("option");
                item.put("type",name);
                String value = item.toJSONString();
                value = escape(value);
                option.addAttribute("value", value);
                String text = (String)item.get("prop");
                //System.out.println(text);
                option.addText(text);
                group.addElement(option);
            }
            select.addElement( group );
        }
    }
    /**
     * Create a select dropdown with the dialect's styles
     * @param dialect the dialect file
     * @return a select list with groups for para headings and charfmts
     */
    private Element getStyles( String dialect )
    {
        JSONObject dObj = (JSONObject)JSONValue.parse(dialect);
        Element select = new Element("select");
        select.addAttribute("id","styles");
        addOptGroup(select,dObj,"headings","headings");
        addOptGroup(select,dObj,"paraformats","paragraph formats");
        addOptGroup(select,dObj,"charformats","character formats");
        addOptGroup(select,dObj,"dividers","dividers");
        addOptGroup(select,dObj,"milestones","milestones");
        addOptGroup(select,dObj,"codeblocks","verbatim");
        addOptGroup(select,dObj,"quotations","quotations");
        return select;
    }
    /**
     * Create a temporary toolbar at the top.
     * @param dialect the dialect describing the formats
     * @return a div element with some buttons
     */
    Element createToolbar( String dialect )
    {
        Element wrapper = new Element("div");
        wrapper.addAttribute("id","toolbar-wrapper");
        Element toolbar = new Element("div");
        toolbar.addAttribute("id","toolbar");
        Element dropdown = new Element("select");
        dropdown.addAttribute("class","dropdown");
        dropdown.addAttribute("id","dropdown");
        Element option1 = new Element("option");
        String currentValue = "docid="+docid+"&version1="+version1;
        option1.addAttribute("value",DEROBERTO_1920);
        if ( DEROBERTO_1920.startsWith(currentValue) )
            option1.addAttribute("selected","");
        option1.addText("De Roberto I Vicerè 1920 Chapter 1");
        dropdown.addElement(option1);
        Element option2 = new Element("option");
        option2.addAttribute("value",HARPUR_1883);
        if ( HARPUR_1883.startsWith(currentValue) )
            option2.addAttribute("selected","");
        option2.addText("Harpur Tower of the Dream 1883");
        dropdown.addElement(option2);
        Element option3 = new Element("option");
        option3.addAttribute("value",DEROBERTO_1894);
        option3.addText("De Roberto I Vicerè 1894 Chapter 1");
        if ( DEROBERTO_1894.startsWith(currentValue) )
            option3.addAttribute("selected","");
        dropdown.addElement(option3);
        wrapper.addElement(dropdown);
        Element save = new Element("button");
        save.addAttribute("title","saved");
        save.addAttribute("class","saved-button");
        save.addAttribute("disabled","disabled");
        save.addAttribute("id","save");
        wrapper.addElement(save);
        Element info = new Element("button");
        info.addAttribute("title","about the markup");
        info.addAttribute("class","info-button");
        info.addAttribute("id","info");
        wrapper.addElement(info);
        Element annotate = new Element("button");
        annotate.addAttribute("title","add a note");
        annotate.addAttribute("class","annotate-button");
        annotate.addAttribute("id","annotate");
        wrapper.addElement(annotate);
        Element styles = getStyles(dialect);
        wrapper.addElement(styles);
        toolbar.addElement( wrapper );
        return toolbar;
    }
    void writeHiddenTag( Element parent, String name, String value  )
    {
        Element hidden = new Element("input");
        hidden.addAttribute("type","hidden");
        hidden.addAttribute("name",name);
        hidden.addAttribute("id",name);
        hidden.addAttribute("value",value);
        parent.addElement(hidden);
    }
    String section()
    {
        String[] parts = docid.split("/");
        StringBuilder sb = new StringBuilder();
        for ( int i=3;i<parts.length;i++ )
        {
            if ( sb.length()> 0 )
                sb.append("/");
            sb.append(parts[i]);
        }
        return sb.toString();
    }
    String author()
    {
        String[] parts = docid.split("/");
        if ( parts.length >= 2 )
            return parts[1];
        else
            return "";
    }
    /**
     * Write the hidden metadata needed back by the server
     */
    void writeHiddenTags()
    {
        Element form = new Element("form");
        form.addAttribute("action",this.requestURL);
        // if visible it will take up space
        form.addAttribute("style","display:none");
        writeHiddenTag( form, Params.DOCID,docid);
        writeHiddenTag( form, Params.ENCODING,"UTF-8");
        writeHiddenTag( form, Params.STYLE, baseID() ); 
        writeHiddenTag( form, Params.FORMAT, "MVD/TEXT" );
        writeHiddenTag( form, Params.SECTION, section() );
        // these are set inthe javascript
        writeHiddenTag( form, Params.AUTHOR, author );
        writeHiddenTag( form, Params.TITLE, title );
        writeHiddenTag( form, Params.VERSION1, version1 );
        doc.addElement( form );
    }
    /**
     * Get the opts for this editor
     * @return an Element (div) containing the content
     */
    private String getOpts( String docid, String version1 ) throws MMLTestException
    {
        JSONObject opts = new JSONObject();
        opts.put("source","source");
        opts.put("target","target");
        opts.put("images","images");
        opts.put("formid","tostil");
        return opts.toJSONString();
    }
    /**
     * Get the opts for this editor
     * @return an Element (div) containing the content
     */
    private String getMml( String docid, String version1 ) throws MMLTestException
    {
        try
        {
            String url = "http://localhost/mml/mml?docid="+docid+"&version1="+version1;
            String res = URLEncoder.getResponseForUrl(url);
            //System.out.println(url+res);
            return res;
        }
        catch ( Exception e )
        {
            throw new MMLTestException(e);
        }
    }
     /**
     * Get the images for this document
     * @return an Element (div) containing the content
     */
    private String getImages() throws MMLTestException
    {
        try
        {
            String url = "http://localhost/mml/images?docid="+docid
                +"&version1="+version1;
            return URLEncoder.getResponseForUrl(url).trim();
        }
        catch ( Exception e )
        {
            throw new MMLTestException(e);
        }
    }
    /**
     * Get the css for this document
     * @return an Element (div) containing the content
     */
    private String getCss() throws MMLTestException
    {
        try
        {
            String url = "http://localhost/mml/corform/"+shortID()+"/default";
            return URLEncoder.getResponseForUrl(url).trim();
        }
        catch ( Exception e )
        {
            throw new MMLTestException(e);
        }
    }
    /**
     * Get the short version of the docid (language/author/work)
     * @return a shortened docid for dialect etc
     */
    String shortID()
    {
        String[] parts = docid.split("/");
        if ( parts.length>= 3 )
        {
            StringBuilder sb = new StringBuilder();
            sb.append(parts[0]);
            sb.append("/");
            sb.append(parts[1]);
            sb.append("/");
            sb.append(parts[2]);
            return sb.toString();
        }
        else
            return docid;
    }
    /**
     * Get the basic ID from docid (just language/author)
     * @return a String
     */
    String baseID()
    {
        String[] parts = docid.split("/");
        if ( parts.length>= 2 )
        {
            StringBuilder sb = new StringBuilder();
            sb.append(parts[0]);
            sb.append("/");
            sb.append(parts[1]);
            return sb.toString();
        }
        else
            return docid;
    }
    /**
     * Build the test age for the editor
     * @throws MMLTestException 
     */
    void composePage()throws MMLTestException
    {
        doc.getHead().addEncoding( encoding );
        doc.getHead().addCssFile("/mml/static/css/mml.css");
        String css = getCss();
        doc.getHead().addCss(css);
        doc.getHead().addScriptFile( "/mml/static/js/jquery-1.11.1.js" );
        doc.getHead().addScriptFile( "/mml/static/js/refloc.js" );
        doc.getHead().addScriptFile( "/mml/static/js/formatter.js" );
        doc.getHead().addScriptFile( "/mml/static/js/info.js" );
        doc.getHead().addScriptFile( "/mml/static/js/rangyinputs-jquery.js");
        doc.getHead().addScriptFile( "/mml/static/js/styles.js");
        doc.getHead().addScriptFile( "/mml/static/js/mml.js" );
        String dialect = MMLGetMMLHandler.getDialect(shortID(),version1);
        String opts = getOpts(docid,version1);
        StringBuilder js = new StringBuilder();
        js.append(EDITOR_START_JS);
        js.append("var dialect = ");
        js.append(dialect);
        js.append(";\n");
        js.append("var opts = ");
        js.append(opts);
        js.append(";\n");
        js.append(EDITOR_END_JS);
        doc.getHead().addScript( js.toString() );
        Element toolbar = createToolbar(dialect);
        doc.addElement( toolbar );
        Element wrapper = new Element("div");
        wrapper.addAttribute("id","wrapper");
        wrapper.addText( getImages() );
        Element help = new Element("div");
        help.addAttribute("id", "help");
        wrapper.addElement( help );
        Element textarea = new Element( "textarea" );
        textarea.addAttribute("id", "source" );
        String mml = getMml(docid,version1);
        textarea.addText( mml );
        wrapper.addElement( textarea );
        Element target = new Element("div");
        target.addAttribute("id","target");
        wrapper.addElement( target );
        doc.addElement( wrapper );
        writeHiddenTags();
    }
}
