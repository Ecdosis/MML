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
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.constants.Database;
import mml.constants.Params;
import mml.database.Connection;
import mml.database.Connector;
import mml.database.MimeType;
import mml.database.ImgInfo;
import mml.exception.MMLException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 * Handle a request for editor options
 * @author desmond
 */
public class MMLGetOptsHandler extends MMLGetHandler
{
    String docid;
    String version1;
    /**
     * Create the editor options
     * @param req the http request
     * @param map the page reference to dimensions map
     * @return the finished opts
     */
    JSONObject createOpts( HttpServletRequest req, HashMap<String,ImgInfo> map )
    {
        JSONObject opts = new JSONObject();
        opts.put("source","source");
        opts.put("target","target");
        opts.put("images","images");
        opts.put("formid","tostil");
        JSONObject data = new JSONObject();
        opts.put("data",data);
        data.put("prefix","p");
        data.put("suffix","");
        data.put("url",req.getScheme()+"://"+req.getServerName()
            +"/"+Database.CORPIX+"/"+docid+version1+"/");
        JSONArray desc = new JSONArray();
        Set<String> keys = map.keySet();
        String[] names = new String[keys.size()];
        keys.toArray(names);
        Arrays.sort(names);
        for ( String name: names )
        {
            JSONObject obj = new JSONObject();
            obj.put("ref",name);
            obj.put("width",map.get(name).width);
            obj.put("height",map.get(name).height);
            obj.put("type",map.get(name).mimeType);
            desc.add( obj );
        }
        data.put("desc",desc);
        return opts;
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
                "^"+docid+version1 );
            HashMap<String,ImgInfo> imageMap = new HashMap<>();
            for ( int i=0;i<imgs.length;i++ )
            {
                MimeType mType = new MimeType();
                Rectangle r = conn.getImageDimensions(Database.CORPIX, 
                    imgs[i], mType);
                if ( r != null )
                {
                    ImgInfo iInfo = new ImgInfo( r.width,r.height, 
                        mType.mimeType );
                    imageMap.put(pageRef(imgs[i]), iInfo );
                }
            }
            JSONObject opts = createOpts(request,imageMap);
            response.getWriter().println(opts.toJSONString());
        }
        catch ( Exception e )
        {
            throw new MMLException(e);
        }
    }
}
