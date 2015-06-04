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
 *  (c) copyright Desmond Schmidt 2015
 */

package mml;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import calliope.core.constants.Database;
import calliope.core.exception.DbException;
import mml.constants.Formats;
import mml.handler.mvd.Archive;
import calliope.core.constants.JSONKeys;
import java.util.ArrayList;
import java.util.Arrays;
import mml.handler.AeseResource;
import mml.handler.get.MMLGetHandler;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;
import org.bson.types.ObjectId;
import edu.luc.nmerge.mvd.MVD;
/**
 * Reap the SCRATCH collection waiting for stuff to appear for saving
 * @author desmond
 */
public class Reaper extends Thread
{
    DocType classifyObj( JSONObject jObj )
    {
        String format = (String)jObj.get(JSONKeys.FORMAT);
        boolean hasBody = jObj.containsKey(JSONKeys.BODY);
        if ( !jObj.containsKey(JSONKeys.DOCID) )
            return DocType.UNKNOWN;
        else
        {
            if ( format != null && (format.equals(Formats.TEXT) 
                || format.equals(Formats.MVD_TEXT))
                && hasBody )
                return DocType.CORTEX;
            if ( hasBody && format != null 
                && (format.equals(Formats.MVD_STIL)|| format.equals(Formats.STIL)) )
                return DocType.CORCODE;
            if ( jObj.containsKey(JSONKeys.OFFSET)
                && jObj.containsKey(JSONKeys.LEN) 
                && jObj.containsKey(JSONKeys.USER)
                && jObj.containsKey(JSONKeys.CONTENT) )
                return DocType.ANNOTATION;
            else
                return DocType.UNKNOWN;
        }
    }
    /**
     * Save cortex for a docid by first removing the old cortex
     * @param jObj the json object representing the version to save
     * @throws DbException 
     */
    private void saveCortex( JSONObject jObj ) throws DbException
    {
        try
        {
            // 1. fetch existing cortex and preserve fields
            String docid = (String)jObj.get(JSONKeys.DOCID);
            AeseResource res = MMLGetHandler.doGetResource( 
                Database.CORTEX, docid );
            if ( res != null )
            {
                Archive cortex = Archive.fromResource(res);
                String newContent = (String)jObj.get(JSONKeys.BODY);
                String vid = (String)jObj.get(JSONKeys.VERSION1);
                cortex.put(vid, newContent.getBytes(cortex.getEncoding()) );
                Connector.getConnection().putToDb(Database.CORTEX,docid,
                    cortex.toResource(docid));
            }
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
    }
    /**
     * Save corcode for a docid by first removing the old corcode
     * @param jObj the json object
     * @throws DbException 
     */
    private void saveCorcode( JSONObject jObj ) throws DbException
    {
        try
        {
            // 1. fetch existing cortex and preserve fields
            String docid = (String)jObj.get(JSONKeys.DOCID);
            AeseResource res = MMLGetHandler.doGetResource( Database.CORCODE, 
                docid );
            if ( res != null )
            {
                Archive corcode = Archive.fromResource(res);
                String newContent = (String)jObj.get(JSONKeys.BODY);
                String vid = (String)jObj.get(JSONKeys.VERSION1);
                corcode.put(vid, newContent.getBytes(corcode.getEncoding()) );
                Connector.getConnection().putToDb(Database.CORCODE,docid,
                    corcode.toResource(docid));
            }
        }
        catch ( Exception e )
        {
            throw new DbException(e);
        }
    }
    private MVD getCortexMVD( String docid )
    {
        return null;
    }
    /**
     * Split a vpath into its short name and group path components
     * @param vpath a ful vpath (vid)
     * @return an array of two strings
     */
    String[] splitVPath( String vpath )
    {
        String[] parts = new String[2];
        int index = vpath.lastIndexOf("/");
        if ( index != -1 )
        {
            parts[0] = vpath.substring(0,index);
            parts[1] = vpath.substring(index+1);
        }
        else
        {
            parts[0] = "";
            parts[1] = vpath;
        }
        return parts;
    }
    public void run()
    {
        try
        {
            Connection conn = Connector.getConnection();
            // do this while the thread runs
            while ( true )
            {
                //1. examine scratch collection to see if it contains any files. 
                // If not, sleep for 5 minutes
                String[] docids = conn.listCollection(Database.SCRATCH);
                if ( docids.length==0 )
                {
                    Autosave.inProgress = false;
                    Thread.sleep(300000);
                }
                else
                {
                    // stop simultaneous saves
                    Autosave.inProgress = true;
                    // 2.  classify all docids found into four collections:
                    // metadata, cortexs, corcodes and annotations
                    ArrayList<JSONObject> cortexs = new ArrayList<JSONObject>();
                    ArrayList<JSONObject> corcodes = new ArrayList<JSONObject>();
                    ArrayList<JSONObject> annotations = new ArrayList<JSONObject>();
                    ArrayList<JSONObject> unclassified = new ArrayList<JSONObject>();
                    for ( int i=0;i<docids.length;i++ )
                    {
                        String jDoc = conn.getFromDb(Database.SCRATCH,docids[i]);
                        if ( jDoc != null )
                        {
                            JSONObject jObj = (JSONObject)JSONValue.parse(jDoc);
                            DocType type = classifyObj( jObj );
                            switch ( type )
                            {
                                case CORTEX:
                                    cortexs.add( jObj );
                                    break;
                                case CORCODE:
                                    corcodes.add(jObj);
                                    break;
                                case ANNOTATION:
                                    annotations.add( jObj );
                                    break;
                                case UNKNOWN:
                                    unclassified.add( jObj );
                                    break;
                            }
                        }
                    }
                    // 3. Save the cortexs if they have matching corcodes
                    for ( int i=0;i<cortexs.size();i++ )
                    {
                        String ctDocId = (String)cortexs.get(i).get(JSONKeys.DOCID);
                        for ( int j=0;j<corcodes.size();j++ )
                        {
                            String ccDocId = (String)corcodes.get(i).get(JSONKeys.DOCID);
                            int lastSlashPos = ccDocId.lastIndexOf("/");
                            if ( lastSlashPos != -1 )
                            {
                                ccDocId = ccDocId.substring(0,lastSlashPos);
                                if ( ccDocId.equals(ctDocId) )
                                {
                                    saveCortex( cortexs.get(i) );
                                    saveCorcode( corcodes.get(j) );
                                }
                            }
                        }
                    }
                    // 4. Save annotations
                    if ( annotations.size()>0 )
                    {
                        JSONObject[] jobjs = new JSONObject[annotations.size()];
                        annotations.toArray( jobjs );
                        Arrays.sort(jobjs,new DocidComparator());
                        String current = null;
                        for ( int k=0;k<jobjs.length;k++ )
                        {
                            String docid = (String)jobjs[k].get(JSONKeys.DOCID);
                            if ( current != docid )
                            {
                                MVD mvd = getCortexMVD(docid);
                                String vpath = (String)jobjs[k].get(
                                    JSONKeys.VERSION1);
                                String[] parts = splitVPath(vpath);
                                int base = mvd.getVersionByNameAndGroup(
                                    parts[0],parts[1]);
                                int offset = ((Number)jobjs[k].get(
                                    JSONKeys.OFFSET)).intValue();
                                int len = ((Number)jobjs[k].get(
                                    JSONKeys.LEN)).intValue();
                                String[] vids = mvd.getVersionsOfRange(
                                    (short)base,offset,len);
                                ArrayList<String> list = new ArrayList<String>();
                                for ( int m=0;m<vids.length;m++ )
                                    list.add( vids[m] );
                                jobjs[k].put(JSONKeys.VERSIONS, vids);
                                current = docid;
                            }
                        }
                    }
                    // for each annotation load its MVD, find out the versions
                    // that share that annotation, and set it into the annotation
                    // then save it to the annotations database
                    // so when we request the annotaitons for one of those other 
                    // versions we will find it
                    // even though we created it for another version
                    
                    // 5. anything we couldn't classify shouldn't be there
                    // so remove them
                    for ( int m=0;m<unclassified.size();m++ )
                    {
                        JSONObject jObj = unclassified.get(m);
                        ObjectId id = (ObjectId)jObj.get(JSONKeys._ID);
                        conn.removeFromDbByField( Database.SCRATCH, 
                            JSONKeys._ID, id.toString() );
                    }
                    // finished! reset flag
                    Autosave.inProgress = false;
                }
            }
        }
        catch ( Exception e )
        {
            System.out.println(e.getMessage());
        }
    }
}
