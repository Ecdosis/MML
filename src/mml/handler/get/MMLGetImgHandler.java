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

import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.constants.Database;
import mml.constants.Params;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.database.MimeType;
import calliope.core.database.ImgInfo;
import calliope.core.constants.JSONKeys;
import mml.exception.MMLException;
import html.*;

/**
 * Handle a request for editor options
 * @author desmond
 */
public class MMLGetImgHandler extends MMLGetHandler
{
    String docid;
    String version1;
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
     * Create the editor options
     * @param req the http request
     * @param map the page reference to dimensions map
     * @return the images as a sequence of IMGs inside divs
     */
    String createImgs( HttpServletRequest req, HashMap<String,ImgInfo> map )
    { 
        Element images = new Element("div");
        images.addAttribute("id","images");
        String url = "/mml/"+Database.CORPIX+"/"+docid+version1;
        Set<String> keys = map.keySet();
        String[] names = new String[keys.size()];
        keys.toArray(names);
        ImageComparator comp = new ImageComparator();
        Arrays.sort(names,comp);
        for ( String name: names )
        {
            ImgInfo info = map.get(name);
            Element wrap = new Element("div");
            wrap.addAttribute("class","image");
            Element img = new Element("img");
            String src = url+"/p"+name;
            img.addAttribute("src",src);
            img.addAttribute("id","image_"+name);
            img.addAttribute("style","width: 100%; max-width: "
                +map.get(name).width+"px");
            img.addAttribute("data-width",Integer.toString(info.width));
            img.addAttribute("data-height",Integer.toString(info.height));
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
        int index = fullID.lastIndexOf("p");
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
            Connection conn = Connector.getConnection();
            docid = request.getParameter(Params.DOCID);
            version1 = request.getParameter(Params.VERSION1);
            String[] imgs = conn.listDocuments( Database.CORPIX, 
                "^"+docid+version1, JSONKeys.FILENAME );
            HashMap<String,ImgInfo> imageMap = new HashMap<>();
            for ( String img: imgs )
            {
                MimeType mType = new MimeType();
                Rectangle r = conn.getImageDimensions(Database.CORPIX, 
                    img, mType);
                if ( r != null )
                {
                    ImgInfo iInfo = new ImgInfo( r.width, r.height, 
                        mType.mimeType );
                    imageMap.put( pageRef(img), iInfo );
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
