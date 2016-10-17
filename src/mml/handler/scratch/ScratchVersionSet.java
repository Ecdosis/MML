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
 *  (c) copyright Desmond Schmidt 2016
 */
package mml.handler.scratch;
import calliope.core.Utils;
import calliope.core.constants.Database;
import calliope.core.constants.Formats;
import calliope.core.database.*;
import calliope.core.exception.DbException;
import mml.exception.MMLException;
import java.util.HashMap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import org.json.simple.*;
import calliope.core.constants.JSONKeys;
import calliope.core.database.Connector;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;

/**
 * A set of ScratchVersions with all the same docid and dbase
 * @author desmond
 */
public class ScratchVersionSet {
    ScratchVersion[] list;
    HashMap<String,Object> otherFields;
    String docid;
    String version1;
    String body;
    String format;
    /**
     * Create a version set
     * @param list the list of scratch versions
     * @param format their format
     */
    public ScratchVersionSet( ScratchVersion[] list )
    {
        this.list = list;
        this.otherFields = new HashMap<String,Object>();
        this.format = "TEXT";
        this.docid = list[0].docid;
        this.version1 = list[0].version;
    }
    public int size()
    {
        int num = 0;
        for ( int i=0;i<list.length;i++ )
        {
            num += list[i].size();
        }
        return num;
    }
    public void setFormat() throws MMLException
    {
        String dbase = getDbase();
        if ( this.size() > 1 )
        {
            if ( dbase.equals(Database.CORTEX) )
                this.format = "MVD/TEXT";
            else if ( dbase.equals(Database.CORCODE) )
                this.format = "MVD/STIL";
            else
                throw new MMLException("Invalid database "+dbase);
        }
        else if ( dbase.equals(Database.CORTEX) )
            this.format = Formats.TEXT;
        else if ( dbase.equals(Database.CORCODE) )
            this.format = Formats.STIL;
        else
            throw new MMLException("Invalid database "+dbase);
    }
    private void parseResource( String resource )
    {
        JSONObject jObj = (JSONObject)JSONValue.parse(resource);
        this.otherFields = new HashMap<String,Object>();
        Set<String> keys = jObj.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String key = iter.next();
            if ( key.equals(JSONKeys._ID) )
                continue;
            else if ( key.equals(JSONKeys.DOCID) )
                docid = (String)jObj.get(JSONKeys.DOCID);
            else if ( key.equals(JSONKeys.BODY) )
                body = (String)jObj.get(JSONKeys.BODY);
            else if ( key.equals(JSONKeys.VERSION1) )
                version1 = (String)jObj.get(JSONKeys.VERSION1);
            else if ( key.equals(JSONKeys.FORMAT) )
                format = (String)jObj.get(JSONKeys.FORMAT);
            else
                otherFields.put( key, jObj.get(key) );
        }
    }
    /**
     * Is this version a layer?
     * @param vid the full version id
     * @return true if it ends in "/layer-something
     */
    private boolean isLayerName( String vid )
    {
        String[] parts = vid.split("-");
        if ( parts.length>1 && parts[parts.length-2].equals("layer") )
        {
            String name = parts[parts.length-1];
            if ( name.equals("final") )
                return true;
            else
            {
                try
                {
                    Integer.parseInt(name);
                    return true;
                }
                catch ( Exception e )
                {
                    return false;
                }
            }
        }
        else
            return false;
    }
    /**
     * Strip off the full version name of the layer name
     * @param vid the full verison name
     * @return the lesser version name, no "/layer-*" crap
     */
    private String splitName( String vid )
    {
        String[] parts = vid.split("/");
        StringBuilder sb = new StringBuilder();
        for ( int i=0;i<parts.length;i++ )
        {
            if ( parts[i].startsWith("layer-") )
                break;
            else 
            {
                if ( sb.length()>0 )
                    sb.append("/");
                sb.append(parts[i]);
            }
        }
        return sb.toString();
    }
    /**
     * Create a scratchversionset from a database record
     * @param resource the dbase resource fetched from the database
     */
    public ScratchVersionSet( String resource, String dbase )
    {
        parseResource( resource );
        if ( format.contains("MVD") )
        {
            MVD mvd = MVDFile.internalise(body);
            HashMap<String,ScratchVersion> map = new HashMap<String,ScratchVersion>();
            for ( short i=1;i<=mvd.numVersions();i++ )
            {
                char[] data = mvd.getVersion(i);
                String vid = mvd.getVersionId(i);
                if ( isLayerName(vid) )
                {
                    int num = ScratchVersion.layerNumber(vid);
                    String shortName = splitName(vid);
                    ScratchVersion sv = map.get(shortName);
                    if ( sv == null )
                    {
                        sv = new ScratchVersion(shortName,docid,dbase,null,false);
                        sv.addLayer(data, num);
                        map.put(shortName,sv);
                    }
                    else
                        sv.addLayer(data,num);
                }
                else
                {
                    ScratchVersion sv = new ScratchVersion(vid+"/layer-final", 
                        docid, dbase,null,false);
                    sv.addLayer( data, 1 );
                    map.put( vid, sv );
                }
            }
            // convert to list
            list = new ScratchVersion[map.size()];
            Collection<ScratchVersion> coll = map.values();
            coll.toArray(list);
        }
        else    // single version/layer
        {
            if ( version1 == null )
                version1 = "/base";
            if ( docid != null && body != null )
            {
                ScratchVersion sv = new ScratchVersion(version1, docid, dbase,null,false);
                sv.addLayer( body.toCharArray(), Integer.MAX_VALUE );
                appendToList( sv );
            }
        }        
    }
    private void appendToList( ScratchVersion sv )
    {
        if ( list == null )
        {
            list = new ScratchVersion[1];
            list[0] = sv;
        }
        else
        {
            ScratchVersion[] newList = new ScratchVersion[list.length+1];
            System.arraycopy(list,0,newList,0,list.length);
            newList[newList.length-1] = sv;
            this.list = newList;
        }
    }
    public String getDocid() throws ArrayIndexOutOfBoundsException
    {
        if ( this.list == null || this.list.length==0 )
            throw new ArrayIndexOutOfBoundsException("List is empty");
        return this.list[0].getDocid();
    }
    /**
     * Get the databse name this resource is assigned to
     * @return the name of a calliope database
     * @throws ArrayIndexOutOfBoundsException 
     */
    public String getDbase() throws ArrayIndexOutOfBoundsException
    {
        if ( this.list == null || this.list.length==0 )
            throw new ArrayIndexOutOfBoundsException("List is empty");
        return this.list[0].getDbase();
    }   
    /**
     * Get the default style for the docid
     * @param styleName the initial style name
     * @return an appropriate style name, or at least "default"
     */
    public static String getDefaultStyleName( String styleName ) throws MMLException
    {
        try
        {
            Connection conn = Connector.getConnection();
            while ( styleName != null && styleName.length()> 0 )
            {
                String jStr = conn.getFromDb(Database.CORFORM,styleName);
                if ( jStr != null )
                    break;
                else
                    styleName = Utils.chomp((styleName));
            }
        }
        catch ( DbException e )
        {
            throw new MMLException(e);
        }
        if ( styleName.length()==0 )
            styleName = "default";
        return styleName;
    }
    /**
     * Create a database resource without docid, _id fields
     * @return a JSON file
     * @throws MMLException 
     */
    public String toResource() throws MMLException
    {
        this.setFormat();
        //System.out.println(getDefaultStyleName());
        // create a JSONObject
        JSONObject jObj = new JSONObject();
        jObj.put( JSONKeys.FORMAT, format );
        if ( list.length > 0 )
        {
            // populate it with otherFields, version1, format
            version1 = list[0].getDefaultVersion();
            jObj.put( JSONKeys.VERSION1, version1 );
            Set<String> keys = otherFields.keySet();
            Iterator<String> iter = keys.iterator();
            while ( iter.hasNext() )
            {
                String key = iter.next();
                jObj.put( key, otherFields.get(key) );
            }
            // create a new body field by merging all the versions into one MVD
        }
        else
            throw new MMLException("ScratchVersion list is empty");
        // check style is set optimally
        String styleName = (String)jObj.get(JSONKeys.STYLE);
        if ( styleName == null || styleName.equals("default") )
            jObj.put(JSONKeys.STYLE, getDefaultStyleName(docid));
        // now build body
        if ( list.length== 1 && list[0].isSimple() )
            jObj.put( JSONKeys.BODY, list[0].getLayerString(Integer.MAX_VALUE) );
        else
        {
            try
            {
                MVD mvd = new MVD();
                short vid = 1;
                if ( otherFields.containsKey(JSONKeys.DESCRIPTION) )
                    mvd.setDescription((String)otherFields.get(JSONKeys.DESCRIPTION));
                else if ( otherFields.containsKey(JSONKeys.TITLE) )
                    mvd.setDescription((String)otherFields.get(JSONKeys.TITLE));
                for ( int i=0;i<list.length;i++ )
                {
                    int[] layers = list[i].getLayerNumbers();
                    for ( int j=0;j<layers.length;j++ )
                    {
                        String str = list[i].getLayerString(layers[j]);
                        String vPath = list[i].version+"/"+ScratchVersion.layerName(layers[j]);
                        String shortName = Utils.getShortName(vPath);
                        String groupName = Utils.getGroupName(vPath);
                        if ( groupName.startsWith("/"))
                            groupName = groupName.substring(1);
                        String longName = "Version "+shortName;
                        if ( vid <= mvd.numVersions() )
                            longName = mvd.getLongNameForVersion(vid);
                        mvd.newVersion( shortName, longName, groupName, 
                            (short)0, false );
                        System.out.println("vid="+vid);
                        mvd.update( vid, str.toCharArray(), true );
                        vid++;
                    }
                }
                body = MVDFile.externalise(mvd);
                jObj.put(JSONKeys.BODY,body);
            }
            catch ( Exception e )
            {
                throw new MMLException(e);
            }
        }
        return jObj.toJSONString();
    }
    /**
     * Replace our versions with those in other
     * @param other the other ScratchVersionSet
     */
    public void upsert( ScratchVersionSet other )
    {
        HashMap<String,ScratchVersion> map = new HashMap<String,ScratchVersion>();
        for ( int i=0;i<this.list.length;i++ )
        {
            ScratchVersion sv = this.list[i];
            map.put( sv.version, sv );
        }
        for ( int i=0;i<other.list.length;i++ )
        {
            map.put( other.list[i].version, other.list[i] );
        }
        ScratchVersion[] newList = new ScratchVersion[map.size()];
        Collection coll = map.values();
        int i = 0;
        Iterator iter = coll.iterator();
        while ( iter.hasNext() )
        {
            ScratchVersion sv = (ScratchVersion)iter.next();
            newList[i++] = sv;
        }
        this.list = newList;
    }
}
