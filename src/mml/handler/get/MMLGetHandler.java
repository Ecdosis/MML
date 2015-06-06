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

import calliope.core.constants.Database;
import calliope.core.constants.JSONKeys;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.*;
import calliope.core.database.*;
import calliope.core.Utils;
import calliope.core.URLEncoder;
import mml.exception.*;
import mml.constants.*;
import mml.handler.AeseVersion;
import mml.handler.AeseResource;
import mml.handler.MMLHandler;

/**
 * Handle GET request for the MML service
 * @author desmond
 */
public class MMLGetHandler extends MMLHandler {
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException {
        try {
            String service = Utils.first(urn);
            urn = Utils.pop(urn);
            if ( service.equals(Service.METADATA) )
                new MMLMetadataHandler().handle( request, response, urn );
            else if ( service.equals(Database.CORPIX) )
                new MMLCorpixHandler().handle( request, response, urn );
            else if (service.equals(Service.VERSIONS))
                new MMLGetVersionsHandler().handle(request,response,urn);
            else if ( service.equals(Database.CORFORM) )
                new MMLResourceHandler(Database.CORFORM).handle( request, response, urn );
            else if ( service.equals(Database.DIALECTS) )
                new MMLResourceHandler(Database.DIALECTS).handle( request, response, urn );
            else if ( service.equals(Service.DIALECT) )
                new MMLGetDialectHandler().handle( request, response, urn );
            else if ( service.equals(Service.VERSION1) )
                new MMLGetVersion1Handler().handle( request, response, urn );
            else if ( service.equals(Database.CORTEX) )
                new MMLResourceHandler(Database.CORTEX).handle( request, response, urn );
            else if ( service.equals(Database.CORCODE) )
                new MMLResourceHandler(Database.CORCODE).handle( request, response, urn );
            else if (service.equals(Service.TEST))
                new MMLGetTestHandler().handle(request,response,urn);
            else if ( service.equals(Service.MML) )
                new MMLGetMMLHandler().handle( request, response, urn );
            else if ( service.equals(Service.IMAGES))
                new MMLGetImgHandler().handle( request, response, urn );
            else if ( service.equals(Service.ANNOTATIONS) )
                new MMLGetAnnotationsHandler().handle(request,response, urn );
            else if ( service.equals(Service.STATIC) )
                new MMLFileHandler().handle(request,response, "mml/static/"+urn );
            else
                new MMLFileHandler().handle(request,response, urn );
        } catch (Exception e) {
            try
            {
                if ( encoding != null )
                    response.setCharacterEncoding(encoding);
                response.getWriter().println(e.getMessage());
            }
            catch ( Exception ex )
            {
                throw new MMLException(ex);
            }
        }
    }
    /**
     * Get a resource from the database if it already exists
     * @param db the collection name
     * @param docID the resource docID
     * @return the resource or null
     * @throws MMLException 
     */
    public static AeseResource doGetResource( String db, String docID ) 
        throws MMLException
    {
        String res = null;
        JSONObject doc = null;
        AeseResource resource = null;
        try
        {
            res = Connector.getConnection().getFromDb(db,docID);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
        if ( res != null )
            doc = (JSONObject)JSONValue.parse( res );
        if ( doc != null )
        {
            String format = (String)doc.get(JSONKeys.FORMAT);
            if ( format == null )
                throw new MMLException("doc missing format");
            String version1 = (String)doc.get(JSONKeys.VERSION1);
            resource = new AeseResource();
            if ( version1 != null )
                resource.setVersion1( version1 );
            if ( doc.containsKey(JSONKeys.DESCRIPTION)&&format.equals(Formats.TEXT) )
                resource.setDescription((String)doc.get(JSONKeys.DESCRIPTION) );
            resource.setFormat( format );
            resource.setContent( (String)doc.get(JSONKeys.BODY) );
        }
        return resource;
    }
    String getBestString( String key, JSONObject o1, JSONObject o2 )
    {
        String ans = null;
        if ( o1 != null )
            ans = (String)o1.get( key );
        if ( ans == null )
            ans = (String)o2.get( key );
        return ans;
    }
    /**
     * Try to retrieve the CorTex/CorCode version specified by the path
     * @param db the database to fetch from
     * @param docID the document ID
     * @param vPath the groups/version path to get
     * @return the CorTex/CorCode version contents or null if not found
     * @throws MMLException if the resource couldn't be found for some reason
     */
    protected AeseVersion doGetResourceVersion( String db, String docID, 
        String vPath ) throws MMLException
    {
        AeseVersion version = new AeseVersion();
        JSONObject doc = null;
        JSONObject md = null;
        byte[] data = null;
        String res = null;
        String metadata;
        //System.out.println("fetching version "+vPath );
        try
        {
            res = Connector.getConnection().getFromDb(db,docID);
            metadata = Connector.getConnection().getFromDb(Database.METADATA,docID);
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
        if ( res != null )
            doc = (JSONObject)JSONValue.parse( res );
        if ( metadata != null )
            md = (JSONObject)JSONValue.parse(metadata);
        if ( doc != null )
        {
            String format = (String)doc.get(JSONKeys.FORMAT);
            if ( format == null )
                throw new MMLException("doc missing format");
            version.setFormat( format );
            // first resolve the link, if any
            if ( version.getFormat().equals(Formats.MVD) )
            {
                MVD mvd = MVDFile.internalise( (String)doc.get(
                    JSONKeys.BODY) );
                //vPath = getBestString(JSONKeys.VERSION1,md,doc);
                version.setStyle(getBestString(JSONKeys.STYLE,md,doc));
                String sName = Utils.getShortName(vPath);
                String gName = Utils.getGroupName(vPath);
                int vId = mvd.getVersionByNameAndGroup(sName, gName );
                version.setMVD(mvd);
                if ( vId != 0 )
                {
                    data = mvd.getVersion( vId );
                    String desc = mvd.getDescription();
                    //System.out.println("description="+desc);
                    //int nversions = mvd.numVersions();
                    //System.out.println("nversions="+nversions);
                    //System.out.println("length of version "+vId+"="+data.length);
                    if ( data != null )
                        version.setVersion( data );
                    else
                        throw new MMLException("Version "+vPath+" not found");
                }
                else
                    throw new MMLException("Version "+vPath+" not found");
            }
            else
            {
                String body = (String)doc.get( JSONKeys.BODY );
                version.setStyle(getBestString(JSONKeys.STYLE,md,doc));
                version.setDescription(getBestString(JSONKeys.TITLE,md,doc)+" "
                    +getBestString(JSONKeys.SECTION,md,doc));
                version.setVersion1(getBestString(JSONKeys.VERSION1,md,doc));
                if ( body == null )
                    throw new MMLException("empty body");
                try
                {
                    data = body.getBytes("UTF-8");
                }
                catch ( Exception e )
                {
                    throw new MMLException( e );
                }
                version.setVersion( data );
            }
        }
        return version;
    }
    /**
     * Get the document body of the given urn or null
     * @param db the database where it is
     * @param docID the docID of the resource
     * @return the document body or null if not present
     */
    protected String getDocumentBody( String db, String docID ) 
        throws MMLException
    {
        try
        {
            String jStr = Connector.getConnection().getFromDb(db,docID);
            if ( jStr != null )
            {
                JSONObject jDoc = (JSONObject)JSONValue.parse( jStr );
                if ( jDoc != null )
                {
                    Object body = jDoc.get( JSONKeys.BODY );
                    if ( body != null )
                        return body.toString();
                }
            }
            throw new MMLException("document "+db+"/"+docID+" not found");
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
     /**
     * Fetch a single style text
     * @param style the path to the style in the corform database
     * @return the text of the style
     */
    public String fetchStyle( String style ) throws MMLException
    {
        // 1. try to get each literal style name
        String actual = getDocumentBody(Database.CORFORM,style);
        while ( actual == null )
        {
            // 2. add "default" to the end
            actual = getDocumentBody( Database.CORFORM,
                URLEncoder.append(style,Formats.DEFAULT) );
            if ( actual == null )
            {
                // 3. pop off last path component and try again
                if ( style.length()>0 )
                    style = Utils.chomp(style);
                else
                    throw new MMLException("no suitable format");
            }
        }
        return actual;
    }
}
