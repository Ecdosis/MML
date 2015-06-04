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
}
