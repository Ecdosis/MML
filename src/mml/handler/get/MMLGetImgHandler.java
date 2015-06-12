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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.constants.JSONKeys;
import mml.constants.Params;
import mml.MMLWebApp;
import calliope.core.image.Corpix;
import mml.exception.MMLException;
import html.*;
import org.json.simple.*;

/**
 * Handle a request for editor options
 * @author desmond
 */
public class MMLGetImgHandler extends MMLGetHandler
{
    String[] pageRefs;
    class ImageComparator implements Comparator<String>
    {
        private int toNumber( String name )
        {
            int num = 0;
            for ( int i=0;i<name.length();i++ )
            {
                num*=10;
                if ( Character.isDigit(name.charAt(i)) )
                    num += name.charAt(i)-'0';
            }
            return num;
        }
        public int compare( String name1, String name2 )
        {
            int num1 = toNumber(name1);
            int num2 = toNumber(name2);
            return (num1>num2)?1:(num1<num2)?-1:0;
        }
    }
    /**
     * Compile a page ref list that is the intersection of pageRefs and names
     * @param names the image names found on disk
     * @return the sorted and available list of page-refs
     */
    private String[] sortByPageRefs( String[] names )
    {
        ArrayList<String> available = new ArrayList<String>();
        HashSet<String> set = new HashSet<String>();
        for ( String name : names )
            set.add( name );
        for ( String ref : pageRefs )
        {
            if ( set.contains(ref) )
                available.add(ref);
        }
        String [] array = new String[available.size()];
        available.toArray(array);
        return array;
    }
    /**
     * Create the list of images
     * @param req the http request
     * @param map the page reference to dimensions map
     * @return the images as a sequence of IMGs inside divs
     */
    String createImgs( HttpServletRequest req, HashMap<String,String> map )
    { 
        Element images = new Element("div");
        images.addAttribute("id","images");
        Set<String> keys = map.keySet();
        String[] names = new String[keys.size()];
        keys.toArray(names);
        if ( pageRefs == null )
        {
            ImageComparator comp = new ImageComparator();
            Arrays.sort(names,comp);
        }
        else
        {
            names = sortByPageRefs( names );
        }
        for ( String name: names )
        {
            String jDoc = map.get(name);
            JSONObject info = (JSONObject)JSONValue.parse( jDoc );
            Element wrap = new Element("div");
            wrap.addAttribute("class","image");
            Element img = new Element("img");
            String jDocId = (String)info.get(JSONKeys.DOCID);
            String src = "/"+jDocId;
            img.addAttribute("src",src);
            img.addAttribute("id","image_"+name);
            //String mimetype = (String)info.get("mimetype");
            int width = ((Number)info.get("width")).intValue();
            int height = ((Number)info.get("height")).intValue();
            img.addAttribute("style","width: 100%; max-width: "
                +width+"px");
            img.addAttribute("data-width",Integer.toString(width));
            img.addAttribute("data-height",Integer.toString(height));
            img.addAttribute("data-ref",name);
            wrap.addElement( img );
            images.addElement( wrap );
        }
        return images.toString();
    }
    /**
     * Extract the page reference from the full docid (including versionID)
     * @param fullID the docid of the image
     * @return the short image ref, such as "4" for page 4
     */
    String pageRef( String fullID )
    {
        int index = fullID.lastIndexOf("/");
        if ( index != -1 )
            return fullID.substring(index+1);
        else
            return fullID;
    }
    /**
     * Handle a request for options
     * @param request the http request
     * @param response the http response
     * @param urn the urn (ignored)
     * @throws MMLException 
     */
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            docid = request.getParameter(Params.DOCID);
            version1 = request.getParameter(Params.VERSION1);
            String pageRefParam = request.getParameter(Params.PAGEREFS);
            if ( pageRefParam != null && pageRefParam.length() > 0 )
                pageRefs = pageRefParam.split(",");
            String longDocID = docid+version1;
            String[] imgs = Corpix.listImages( MMLWebApp.webRoot, longDocID );
            HashMap<String,String> imageMap = new HashMap<String,String>();
            for ( String img: imgs )
            {
                String jDoc = Corpix.getMetaData( MMLWebApp.webRoot, img );
                if ( jDoc != null )
                {
                    imageMap.put( pageRef(img), jDoc );
                }
            }
            // write out html
            String html = createImgs(request,imageMap);
            response.getWriter().println(html);
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
