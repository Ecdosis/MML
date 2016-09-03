/* This file is part of MML.
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
 */
package mml.handler.json;

import calliope.core.constants.JSONKeys;
import mml.constants.Formats;
import mml.exception.JSONException;
import java.util.HashMap;
import java.util.ArrayList;
import org.json.simple.*;
/**
 *
 * @author desmond
 */
public class STILDocument extends JSONObject
{
    ArrayList<JSONObject> ranges;
    HashMap<Range,JSONObject> map;
    /** array of actual ranges with absolute offsets */
    ArrayList<Range> rangeArray;
    int lastOffset;
    
    public STILDocument( String style )
    {
        super();
        put( JSONKeys.STYLE, style );
        ranges = new ArrayList<JSONObject>();
        put( JSONKeys.RANGES, ranges );
        put( JSONKeys.FORMAT, Formats.STIL );
        lastOffset = 0;
        map = new HashMap<Range,JSONObject>();
    }
    /**
     * Add a range to the STIL Document. Must be added in sequence
     * @param r the actual range to add (NOT relative)
     * @return the added document
     */
    public JSONObject add( Range r ) throws JSONException
    {
        JSONObject doc = new JSONObject();
        int reloff = r.offset - lastOffset;
        lastOffset = r.offset;
        doc.put( JSONKeys.NAME, r.name );
        doc.put( JSONKeys.RELOFF, reloff );
        doc.put( JSONKeys.LEN, r.len );
        if ( r.removed )
            doc.put( JSONKeys.REMOVED, true );
        if ( r.annotations != null && r.annotations.size() > 0 )
        {
            ArrayList<Object> attrs = new ArrayList<Object>();
            for ( int i=0;i<r.annotations.size();i++ )
            {
                Annotation a = r.annotations.get( i );
                attrs.add( a.toJSONObject() );
            }
            doc.put( JSONKeys.ANNOTATIONS, attrs );
        }
        ranges.add( doc );
        // remember for later update
        map.put( r, doc );
        return doc;
    }
    public void updateLen( Range r, int len )
    {
        JSONObject doc = map.get(r);
        doc.put(JSONKeys.LEN, len );
    }
    /**
     * Get the range information from a loaded document
     * @param key the property name desired
     * @param offset the absolute offset
     * @param length the length of the range
     * @return a range object
     */
    public Range get( String key, int offset, int length ) throws Exception
    {
        if ( rangeArray != null )
        {
            int top,bottom,mid;
            top = 0;
            bottom = rangeArray.size()-1;
            while ( top <= bottom )
            {
                mid = (bottom+top)/2;
                Range r = rangeArray.get(mid);
                if ( r.offset+r.len<offset )
                    top = mid+1;
                else if ( r.offset >= offset+length )
                    bottom = mid-1;
                else
                {
                    // overlap
                    // 1. move start to first overlapping range
                    int start = mid;
                    while ( start > 0 && r.offset+r.len>offset )
                    {
                        start--;
                        r = rangeArray.get(start);
                    }
                    if ( start<rangeArray.size()-1&& r.offset+r.len<=offset )
                        start++;
                    // 2. move end to last overlapping range
                    int end = mid;
                    do
                    {
                        r = rangeArray.get( end );
                        if ( r.offset<offset+length )
                            end++;
                    }
                    while ( end < rangeArray.size()-1 
                        && r.offset<offset+length );
                    if ( end>0&&r.offset>=offset+length )
                        end--;
                    // look in the range between start and end
                    for ( int k=start;k<=end;k++ )
                    {
                        r = rangeArray.get( k );
                        if ( r.name.equals(key) )
                            return r;
                    }
                    break;
                }
            }
        }
        else
            throw new Exception("STILDocument not loaded");
        return null;
    }
}
