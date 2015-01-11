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
 *  You should have received a copy of the GNU General Pubfiles.get(i).namelic License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */

package mml.handler.mvd;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import mml.handler.json.JSONDocument;
import mml.handler.AeseResource;
import calliope.core.constants.JSONKeys;
import mml.constants.Formats;
import mml.exception.MMLException;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
import edu.luc.nmerge.mvd.Version;

/**
 * A set of CorCode or CorTex files, each a version of the same work, 
 * indexed by the group-path/shortname separated by "/"
 * @author desmond
 */
public class Archive extends HashMap<String,byte[]>
{
    String author;
    String title;
    String description;
    StringBuilder log;
    String style;
    String version1;
    String revid;
    String format;
    String encoding;
    HashMap<String,String> nameMap;
    /**
     * Create an archive
     * @param title the title of the work
     * @param author the full author's name
     * @param format the format (changes to MVD if more than 1 version)
     * @param encoding defaults to UTF-8
     */
    public Archive( String title, String author, String format, String encoding )
    {
        this.title = title;
        this.author = author;
        this.log = new StringBuilder();
        this.style = "default";
        this.nameMap = new HashMap<String,String>();
        this.format = format;
        this.encoding = (encoding==null)?"UTF-8":encoding;
        StringBuilder sb = new StringBuilder(title);
//        sb.append( " by ");
//        if ( author.length()>0 )
//            sb.append( Character.toUpperCase(author.charAt(0)) );
//        if ( author.length()>1 )
//            sb.append( author.substring(1) );
        description = sb.toString();
    }
    /**
     * Add a long name to our map for later use
     * @param key the groups+version short name key
     * @param longName its long name
     */
    public void addLongName( String key, String longName )
    {
        nameMap.put( key, longName );
        System.out.println("Setting long name for "+key+" to "+longName);
    }
    public void setStyle( String style )
    {
        this.style = style;
    }
    /**
     * Get the log report of this archive's merging activities
     * @return a string
     */
    public String getLog()
    {
        return log.toString();
    }
    /**
     * Split off the groups path if any
     * @param key the groups+short name separated by slashes
     * @return the groups name
     */
    private String getGroups( String key )
    {
        String[] parts = key.split("/");
        String groups = "Base";
        if ( parts.length > 1 )
        {
            groups = "";
            for ( int i=0;i<parts.length-1;i++ )
            {
                groups = parts[i];
                if ( i < parts.length-2 )
                    groups += "/";
            }
        }
        return groups;
    }
    /**
     * Get the short name from a compound groups+short name
     * @param key the compound key
     * @return the bare short name at the end
     */
    private String getKey( String key )
    {
        String[] parts = key.split("/");
        key = parts[parts.length-1];
        return key;
    }
    /**
     * Convert this archive to a resource, wrapped in JSON for storage
     * @param mvdName name of the MVD
     * @return a string representation of the MVD as a JSON document
     * @throws MMLException 
     */
    public String toResource( String mvdName ) throws MMLException
    {
        try
        {
            String body;
            JSONDocument doc = new JSONDocument();
            if ( size()==1 )
            {
                Set<String> keys = keySet();
                Iterator<String> iter = keys.iterator();
                String key = iter.next();
                byte[] data = get( key );
                if ( format.equals(Formats.MVD_STIL) )
                    format = Formats.STIL;
                else if ( format.equals(Formats.MVD_TEXT) )
                    format = Formats.TEXT;
                body = new String( data, encoding );
                if ( version1 == null )
                    version1 = key;
                if ( nameMap.containsKey(version1) )
                    description = nameMap.get(version1);
            }
            else
            {
                // more than 1 version: make an MVD 
                Set<String> keys = keySet();
                Iterator<String> iter = keys.iterator();
                MVD mvd = new MVD();
                mvd.setDescription( description );
                // go through the files, adding versions to the MVD
                short vId;
                long startTime = System.currentTimeMillis();
                while ( iter.hasNext() )
                {
                    String key = iter.next();
                    String groups = getGroups( key );
                    String shortKey = getKey( key );
                    if ( version1 == null )
                        version1 = "/"+groups+"/"+shortKey;
                    byte[] data = get( key );
                    String longName = nameMap.get(key);
                    if ( longName == null )
                    {
                        longName = "Version ";
                        longName += (groups.length()>0)?shortKey+" of "
                            +groups:shortKey;
                    }
                    vId = (short)mvd.newVersion( shortKey, longName, 
                        groups, Version.NO_BACKUP, false );
                    // tepmorary hack
                    mvd.setDirectAlign( true );
                    mvd.update( vId, data, true );
                }
                long diff = System.currentTimeMillis()-startTime;
                log.append( "merged " );
                log.append( title );
                log.append( ": " );
                log.append( mvdName );
                log.append( " in " );
                log.append( diff );
                log.append( " milliseconds\n" );
                body = mvd.toString();
                if ( body.length() == 0 )
                    throw new MMLException("failed to create MVD");
                if ( format.equals(Formats.TEXT)||format.equals(Formats.STIL) )
                format = "MVD/"+format;
            }
            doc.add( JSONKeys.TITLE, title, false );
            doc.add( JSONKeys.VERSION1, version1, false );
            doc.add( JSONKeys.DESCRIPTION, description, false );
            doc.add( JSONKeys.AUTHOR, author, false );
            doc.add( JSONKeys.STYLE, style, false );
            doc.add( JSONKeys.FORMAT, format, false );
            doc.add( JSONKeys.BODY, body, false );
            return doc.toString();
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
    /**
     * Convert a resource to an Archive, for updating
     * @param resource a string representation of the MVD as a JSON document
     */
    public void fromResource( AeseResource resource ) throws MMLException
    {
        if ( resource.getFormat().startsWith("MVD") )
        {
            MVD mvd = MVDFile.internalise( resource.getContent() );
            int nVersions = mvd.numVersions();
            this.nameMap = new HashMap<String,String>();
            for ( int vId=1;vId<=nVersions;vId++ )
            {
                byte[] data = mvd.getVersion(vId);
                String groupName = mvd.getGroupPath((short)vId);
                String shortName = mvd.getVersionShortName( vId );
                String versionID = groupName+"/"+shortName;
                nameMap.put(versionID,mvd.getVersionLongName(vId));
                put( versionID, data );
            }
            this.version1 = resource.getVersion1();
        }
        else
        {
            version1 = resource.getVersion1();
            try
            {
                put( version1, resource.getContent().getBytes(encoding) );
                nameMap.put( version1, resource.getDescription() );
            }
            catch ( Exception e )
            {
                throw new MMLException(e);
            }
        }
        this.format = resource.getFormat();
    }
}
