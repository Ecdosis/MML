/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */
package mml.database;

import mml.exception.MMLDbException;
import mml.constants.Database;
import mml.constants.JSONKeys;
import java.util.Iterator;
import java.util.ArrayList;
import com.mongodb.MongoClient;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.util.JSON;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.WriteResult;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;
import com.mongodb.DBCursor;

import java.io.InputStream;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;
import java.util.List;
import java.util.HashSet;


/**
 * Database interface for MongoDB
 * @author desmond
 */
public class MongoConnection extends Connection 
{
    MongoClient client;
    static int MONGO_PORT = 27017;
    /** connection to database */
    DB  db;
    public MongoConnection( String user, String password, String host, 
        int dbPort, int wsPort )
    {
        super( user, password, host, dbPort, wsPort );
    }
    /**
     * Connect to the database
     * @throws Exception 
     */
    private void connect() throws Exception
    {
        if ( db == null )
        {
            MongoClient mongoClient = new MongoClient( host, MONGO_PORT );
            db = mongoClient.getDB("calliope");
            //boolean auth = db.authenticate( user, password.toCharArray() );
            //if ( !auth )
            //    throw new MMLDbException( "MongoDB authentication failed");
        }
    }
    /**
     * Get the Mongo db collection object from its name
     * @param collName the collection name
     * @return a DBCollection object
     * @throws MMLDbException 
     */
    private DBCollection getCollectionFromName( String collName )
        throws MMLDbException
    {
        DBCollection coll = db.getCollection( collName );
        if ( coll == null )
            coll = db.createCollection( collName, null );
        if ( coll != null )
            return coll;
        else
            throw new MMLDbException( "Unknown collection "+collName );
    }
    /**
     * Fetch a resource from the server, or try to.
     * @param collName the collection or database name
     * @param docID the path to the resource in the collection
     * @return the response as a string or null if not found
     */
    @Override
    public String getFromDb( String collName, String docID ) throws MMLDbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject query = new BasicDBObject( JSONKeys.DOCID, docID );
            DBObject obj = coll.findOne( query );
            if ( obj != null )
                return obj.toString();
            else
                throw new FileNotFoundException( "failed to find "
                    +collName+"/"+docID );
        }
        catch ( Exception e )
        {
            throw new MMLDbException( e );
        }
    }
    /**
     * PUT a json file to the database
     * @param collName the name of the collection
     * @param docID the docid of the resource 
     * @param json the json to put there
     * @return the server response
     */
    @Override
    public String putToDb( String collName, String docID, String json ) 
        throws MMLDbException
    {
        try
        {
            docIDCheck( collName, docID );
            DBObject doc = (DBObject) JSON.parse(json);
            doc.put( JSONKeys.DOCID, docID );
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject query = new BasicDBObject( JSONKeys.DOCID, docID );
            WriteResult result = coll.update( query, doc, true, false );
            //return removeFromDb( path );
            return result.toString();
        }
        catch ( Exception e )
        {
            throw new MMLDbException( e );
        }
    }
    /**
     * Remove a document from the database
     * @param collName name of the collection
     * @param docID the docid of the resource 
     * @param json the json to put there
     * @return the server response
     */
    @Override
    public String removeFromDb( String collName, String docID ) 
        throws MMLDbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            DBObject query = new BasicDBObject( JSONKeys.DOCID, docID );
            WriteResult result = coll.remove( query );
            return result.toString();
        }
        catch ( Exception e )
        {
            throw new MMLDbException( e );
        }
    }
    /**
     * Get a list of docIDs or file names corresponding to the regex expr
     * @param collName the collection to query
     * @param expr the regular expression to match against docid
     * @return an array of matching docids, which may be empty
     */
    @Override
    public String[] listDocuments( String collName, String expr )
        throws MMLDbException
    {
        try
        {
            connect();
            DBCollection coll = getCollectionFromName( collName );
            if ( coll != null )
            {
                BasicDBObject q = new BasicDBObject();
                q.put(JSONKeys.DOCID, Pattern.compile(expr) );
                DBCursor curs = coll.find( q );
                ArrayList<String> docids = new ArrayList<String>();
                Iterator<DBObject> iter = curs.iterator();
                int i = 0;
                while ( iter.hasNext() )
                {
                    String dId = (String)iter.next().get(JSONKeys.DOCID);
                    if ( dId.matches(expr) )
                        docids.add( dId );
                }
                String[] array = new String[docids.size()];
                docids.toArray( array );
                return array;
            }
            else
                throw new MMLDbException("collection "+collName+" not found");
        }
        catch ( Exception e )
        {
            throw new MMLDbException( e );
        }
    }
    /**
     * List all the documents in a Mongo collection
     * @param collName the name of the collection
     * @return a String array of document keys
     * @throws MMLDbException 
     */
    @Override
    public String[] listCollection( String collName ) throws MMLDbException
    {
        if ( !collName.equals(Database.CORPIX) )
        {
            try
            {
                connect();
            }
            catch ( Exception e )
            {
                throw new MMLDbException( e );
            }
            DBCollection coll = getCollectionFromName( collName );
            BasicDBObject keys = new BasicDBObject();
            keys.put( JSONKeys.DOCID, 1 );
            DBCursor cursor = coll.find( new BasicDBObject(), keys );
            if ( cursor.length() > 0 )
            {
                String[] docs = new String[cursor.length()];
                Iterator<DBObject> iter = cursor.iterator();
                int i = 0;
                while ( iter.hasNext() )
                    docs[i++] = (String)iter.next().get( JSONKeys.DOCID );
                return docs;
            }
            else
                throw new MMLDbException( "no docs in collection "+collName );
        }
        else
        {
            GridFS gfs = new GridFS( db, collName );
            DBCursor curs = gfs.getFileList();
            int i = 0;
            List<DBObject> list = curs.toArray();
            HashSet<String> set = new HashSet<String>();
            Iterator<DBObject> iter = list.iterator();
            while ( iter.hasNext() )
            {
                String name = (String)iter.next().get("filename");
                set.add(name);
            }
            String[] docs = new String[set.size()];
            set.toArray( docs );
            return docs;
        }
    }
    /**
     * Get an image from the database
     * @param collName the collection name
     * @param docID the docid of the corpix
     * @return the image data
     */
    @Override
    public byte[] getImageFromDb( String collName, String docID )
    {
        try
        {
            connect();
            GridFS gfs = new GridFS( db, collName );
            GridFSDBFile file = gfs.findOne( docID );
            if ( file != null )
            {
                InputStream ins = file.getInputStream();
                long dataLen = file.getLength();
                // this only happens if it is > 2 GB
                if ( dataLen > Integer.MAX_VALUE )
                    throw new MMLDbException( "file too big (size="+dataLen+")" );
                byte[] data = new byte[(int)dataLen];
                int offset = 0;
                while ( offset < dataLen )
                {
                    int len = ins.available();
                    offset += ins.read( data, offset, len );
                }
                return data;
            }
            else
                throw new FileNotFoundException(docID);
        }
        catch ( Exception e )
        {
            e.printStackTrace( System.out );
            return null;
        }
    }
    /**
     * Store an image in the database
     * @param collName name of the image collection
     * @param docID the docid of the resource
     * @param data the image data to store
     * @throws MMLDbException 
     */
    @Override
    public void putImageToDb( String collName, String docID, byte[] data ) 
        throws MMLDbException
    {
        docIDCheck( collName, docID );
        GridFS gfs = new GridFS( db, collName );
        GridFSInputFile	file = gfs.createFile( data );
        file.setFilename( docID );
        file.save();
    }
    /**
     * Delete an image from the database
     * @param collName the collection name e.g. "corpix"
     * @param docID the image's docid path
     * @throws MMLDbException 
     */
    @Override
    public void removeImageFromDb( String collName, String docID ) 
        throws MMLDbException
    {
        try
        {
            GridFS gfs = new GridFS( db, collName );
            GridFSDBFile file = gfs.findOne( docID );
            if ( file == null )
                throw new FileNotFoundException("file "+collName+"/"+docID
                    +" not found");
            gfs.remove( file );
        }
        catch ( Exception e )
        {
            throw new MMLDbException( e );
        }
    }
}