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
import calliope.core.constants.JSONKeys;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import mml.handler.scratch.ScratchVersion;
import mml.handler.scratch.ScratchVersionSet;
/**
 * Reap the SCRATCH collection waiting for stuff to appear for saving
 * @author desmond
 */
public class Reaper extends Thread
{
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
    /**
     * Run the reaper. Every 5 minutes we look for new entries in the
     * scratch collection. If we find them we classify them as annotation,
     * cortex or corcode. We then merge them into the proper databases 
     * and delete the temporary copies.
     */
    public void run()
    {
        try
        {
            Connection conn = Connector.getConnection();
            // do this while the thread runs
            while ( true )
            {
                // 1. examine scratch collection to see if it contains any files. 
                // or if files are being written elsewhere
                // If there are no files or we are locked, sleep for 1 minute
                String[] ids = conn.listCollectionByKey(Database.SCRATCH,JSONKeys._ID);
                if ( ids.length==0 || Autosave.inProgress )
                {
                    Thread.sleep(60000);
                    //System.out.println("Reper sleeping 1 minute");
                }
                else if ( !Autosave.inProgress )
                {
                    // stop simultaneous saves
                    Autosave.inProgress = true;
                    //System.out.println("COmmenced autosave");
                    // prepare map of documents to be saved
                    // keyed on docid
                    HashMap<String,ScratchVersion[]> versions = 
                        new HashMap<String,ScratchVersion[]>();
                    //System.out.println("Found "+versions+" versions in scratch");
                    for ( int i=0;i<ids.length;i++ )
                    {
                        String jDoc = conn.getFromDbByField(Database.SCRATCH,
                            ids[i],JSONKeys._ID);
                        if ( jDoc != null )
                        {
                            ScratchVersion sv = ScratchVersion.fromJSON(jDoc);
                            // records unique for cc-default, cc-pages and cortex
                            // but not for versions
                            String docid = sv.getDocid();
                            if ( versions.containsKey(docid) )
                            {
                                ScratchVersion[] list = versions.get(docid);
                                ScratchVersion[] newList = new ScratchVersion[list.length+1];
                                System.arraycopy(list,0,newList,0,list.length);
                                newList[list.length] = sv;
                                versions.put(docid, newList);
                            }
                            else
                            {
                                ScratchVersion[] list = new ScratchVersion[1];
                                list[0] = sv;
                                versions.put(docid,list);
                            }
                        }
                    }
                    // for each docid, retrieve the original resources 
                    // from their respective databases
                    Set<String> keys = versions.keySet();
                    Iterator<String> iter = keys.iterator();
                    while ( iter.hasNext() )
                    {
                        String key = iter.next();
                        ScratchVersionSet svs = new ScratchVersionSet(versions.get(key));
                        String docid = svs.getDocid();
                        String dbase = svs.getDbase();  
                        String jDoc = conn.getFromDb(dbase,docid);
                        if ( jDoc != null )
                        {
                            ScratchVersionSet dbaseSet = new ScratchVersionSet(jDoc,dbase);
                            dbaseSet.upsert( svs );
                            conn.putToDb(dbase, docid, dbaseSet.toResource());
                            //System.out.println("Put resource to database overwriting one already there");
                        }
                        else // not already present
                        {
                            String jStr = svs.toResource();
                            conn.putToDb(dbase, docid, jStr);
                            //System.out.println("Put resource to database not already there");
                        }
                    }
                    // we got here without exception: so it's safe to 
                    // remove cortexs and corcodes from the scratch database
                    for ( int m=0;m<ids.length;m++ )
                    {
                        String id = ids[m];
                        String res = conn.removeFromDbByField( Database.SCRATCH, 
                            JSONKeys._ID, ids[m] );
                        //System.out.println("removed sratch resource. res="+res);
                    }
                    // finished! reset flag
                    Autosave.inProgress = false;
                }
            }
        }
        catch ( Exception e )
        {
            Autosave.inProgress = false;
            System.out.println(e.getMessage());
        }
    }
}
