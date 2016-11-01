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

import calliope.core.constants.JSONKeys;
import calliope.core.database.Connector;
import calliope.core.database.Connection;
import calliope.core.constants.Database;
import calliope.core.exception.DbException;
import mml.exception.MMLException;
import mml.Autosave;
import calliope.core.handler.EcdosisMVD;
import mml.handler.get.Layers;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import java.util.HashMap;
import java.util.Set;
import java.util.Arrays;


/**
 * Get "versions" composed of layers
 * @author desmond
 */
public class Scratch 
{
    /**
     * Save a single scrath version
     * @param sv the scratch version object
     * @param docid its docid
     * @throws MMLException 
     */
    public static void save( ScratchVersion sv ) throws MMLException
    {
        try
        {
            System.out.println("Saving "+sv.docid+","+sv.version);
            Autosave.inProgress = true;
            // 1. check if scratch version already exists
            Connection conn = Connector.getConnection();
            String docid = sv.getDocid();
            String res = conn.getFromDb( Database.SCRATCH, sv.dbase, docid, sv.version );
            if ( res != null )
            {
                conn.removeFromDb( Database.SCRATCH, sv.dbase, docid, sv.version );
                System.out.println("Removed "+sv.docid+","+sv.version+" from scratch");
            }
            // 2. write the record and record its time
            String json = sv.toJSON();
            conn.putToDb(Database.SCRATCH, sv.dbase, docid, sv.version, json);
            System.out.println("Saved "+sv.docid+","+sv.version);
            Autosave.inProgress = false;
        }
        catch ( DbException e )
        {
            Autosave.inProgress = false;
            System.out.println("Error "+e.getMessage());
            throw new MMLException(e);
        }
    }
    protected static EcdosisMVD doGetMVD( String db, String docid ) 
        throws DbException
    {
        String res = null;
        JSONObject jDoc = null;
        res = Connector.getConnection().getFromDb(db,docid);
        if ( res != null )
            jDoc = (JSONObject)JSONValue.parse( res );
        if ( jDoc != null )
        {
            String format = (String)jDoc.get(JSONKeys.FORMAT);
            if ( format != null )
            {
                return new EcdosisMVD(jDoc);                    
            }
        }
        return null;
    }
    /**
     * Get a scratch version you know is in the scratch database
     * @param docid its docid
     * @param version its version
     * @param dbase the database it belongs to
     * @return the ScratchVersion built from the db entry
     * @throws DbException 
     */
    private static ScratchVersion getScratchVersion( String docid, 
        String version, String dbase ) throws DbException
    {
        Connection conn = Connector.getConnection();
        // base + docid + version should be unique
        String bson = conn.getFromDb(Database.SCRATCH, dbase, docid, version);
        if ( bson != null )
        {
            return ScratchVersion.fromJSON(bson);
        }
        return null;
    }
    /**
     * Get a version that may or may not be in scratch. If not put it there.
     * @param docid the docid 
     * @param version the desired single version or null if default
     * @param dbase the database it is in
     * @return a ScratchVersion object or null
     * @throws MMLException 
     */
    public static ScratchVersion getVersion( String docid, 
        String version, String dbase ) throws MMLException
    {
        try
        {
            ScratchVersion sv = null;
            if ( version != null )
                sv = getScratchVersion(docid,version,dbase);
            if ( sv == null )
            {
                EcdosisMVD mvd = doGetMVD( dbase, docid );
                if ( mvd != null )
                {
                    if ( version == null )
                        version = mvd.getVersion1();
                    String base = Layers.stripLayer(version);
                    HashMap<String,char[]> layers = new HashMap<String,char[]>();
                    int numVersions = mvd.numVersions();
                    for ( int i=1;i<=numVersions;i++ )
                    {
                        String vName = mvd.getVersionId((short)i);
                        if ( vName.lastIndexOf(base) == 0 )
                            layers.put( vName, mvd.getVersion(i) );
                    }
                    if ( !layers.isEmpty() )
                    {
                        Set<String> keys = layers.keySet();
                        String[] arr = new String[keys.size()];
                        keys.toArray( arr );
                        Arrays.sort( arr );
                        String[] all = mvd.getAllVersions();
                        short id = mvd.getVersionId(version);
                        String longName = mvd.getVersionLongName(id);
                        sv = new ScratchVersion(base,longName,
                            docid,dbase,null,false);
                        for ( int i=0;i<arr.length;i++ )
                        {
                            String updatedName = Layers.upgradeLayerName(all,arr[i]);
                            sv.addLayer( layers.get(arr[i]), 
                                ScratchVersion.layerNumber(updatedName) );
                        }
                        // save it for next time
                        Connection conn = Connector.getConnection();
                        conn.putToDb(Database.SCRATCH, docid,sv.toJSON());
                        return sv;
                    }
                }
                return null;
            }
            else
                return sv;
         }
        catch ( DbException e )
        {
            throw new MMLException(e);
        }
    }
}
