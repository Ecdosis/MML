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
 *  Copyright Desmond Schmidt 2015
 */

package mml.handler;
import edu.luc.nmerge.mvd.MVD;
import edu.luc.nmerge.mvd.MVDFile;
/**
 * An MVD or plain text file wrapped up as a resource
 * @author desmond
 */
public class AeseResource 
{
    String format;
    String content;
    String description;
    String version1;
    MVD mvd;
    public void setFormat( String format )
    {
        this.format = format;
    }
    public void setContent( String content )
    {
        this.content = content;
    }
    public void setDescription( String description )
    {
        this.description = description;
    }
    public void setVersion1( String version1 )
    {
        this.version1 = version1;
    }
    public String getVersion1( )
    {
        return this.version1;
    }
    public String getFormat()
    {
        return this.format;
    }
    public String getContent( )
    {
        return this.content;
    }
    public String getDescription( )
    {
        return this.description;
    }
    /**
     * Get the full versionIDs of all the versions
     * @return an array of versionIDs
     */
    public String[] listVersions()
    {
        if ( !format.startsWith("MVD") )
        {
            String[] array = new String[1];
            array[0] = (this.version1==null)?"/base":this.version1;
            return array;
        }
        else
        {
            this.mvd = MVDFile.internalise( this.content );
            int nversions = mvd.numVersions();
            String[] array = new String[nversions];
            for ( int i=0;i<nversions;i++ )
            {
                short id = (short)(i+1);
                String groupPath = mvd.getGroupPath(id);
                String shortName = mvd.getVersionShortName(id);
                array[i] = groupPath+"/"+shortName;
            }
            return array;
        }
    }
    /**
     * Get the long name for the given version int id
     * @param id the identifier as an index+1 into the version table
     * @return a String
     */
    public String getVersionLongName( int id )
    {
        if ( mvd != null )
        {
            String longName = mvd.getVersionLongName(id);
            if ( longName.contains("/layer") )
                longName = longName.substring(0,longName.indexOf("/layer"));
            return longName;
        }
        else if ( version1.contains("/layer") )
            return "Version "+version1.substring(0,version1.lastIndexOf("/layer"));
        else
            return "Version "+version1;
    }
}
