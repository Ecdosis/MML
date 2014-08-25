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
import html.*;
import java.io.File;
import java.io.FileInputStream;
import mml.Utils;

/**
 * Test interface for editor
 * @author desmond
 */
public class Editor extends Test
{
    String service;
    String host;
    public Editor()
    {
        super();
        this.encoding = "UTF-8";
    }
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            this.service = Utils.first(request.getRequestURI());
            this.host = request.getServerName();
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
     * Create a temporary toolbar at the top.
     * @return a div element with some buttons
     */
    Element createToolbar()
    {
        Element wrapper = new Element("div");
        wrapper.addAttribute("id","toolbar-wrapper");
        Element toolbar = new Element("div");
        toolbar.addAttribute("id","toolbar");
        Element save = new Element("button");
        save.addAttribute("class", "btn btn-default");
        save.addAttribute("title","save");
        Element saveIcon = new Element("span");
        saveIcon.addAttribute("class","glyphicon glyphicon-floppy-saved");
        saveIcon.addAttribute("id","saveicon");
        save.addAttribute("disabled","disabled");
        save.addElement(saveIcon);
        save.addAttribute("id","save");
        wrapper.addElement(save);
        Element info = new Element("button");
        info.addAttribute("class", "btn btn-default");
        info.addAttribute("title","about the markup");
        Element infoIcon = new Element("span");
        infoIcon.addAttribute("class","glyphicon glyphicon-info-sign");
        infoIcon.addAttribute("id","infoicon");
        info.addElement(infoIcon);
        info.addAttribute("id","info");
        wrapper.addElement(info);
        toolbar.addElement( wrapper );
        return toolbar;
    }
    /**
     * Build the test age for the editor
     * @throws MMLTestException 
     */
    void composePage()throws MMLTestException
    {
        doc.getHead().addEncoding( encoding );
        doc.getHead().addCssFile("css/bootstrap-glyphicons.css");
        doc.getHead().addCssFile("css/deroberto.css");
        doc.getHead().addCssFile("css/bootstrap.css");
        doc.getHead().addScriptFile( "js/jquery-1.11.1.js" );
        doc.getHead().addScriptFile( "js/mml.js" );
        String editor = readFile( "js/editor.js" );
        doc.getHead().addScript( editor );
        Element toolbar = createToolbar();
        doc.addElement( toolbar );
        Element wrapper = new Element("div");
        wrapper.addAttribute("id","wrapper");
        Element images = new Element("div");
        images.addAttribute("id", "images");
        wrapper.addElement( images );
        Element help = new Element("div");
        help.addAttribute("id", "help");
        wrapper.addElement( help );
        Element textarea = new Element( "textarea" );
        textarea.addAttribute("id", "source" );
        String mml = readFile("test/DeRoberto-1920.mml");
        textarea.addText( mml );
        wrapper.addElement( textarea );
        Element target = new Element("div");
        target.addAttribute("id","target");
        wrapper.addElement( target );
        doc.addElement( wrapper );
    }
}
