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

package mml.handler.json;

import mml.constants.JSONKeys;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.Set;
import java.util.Iterator;

/**
 * Dialect class just exists to compare two dialect files
 * @author desmond
 */
public class Dialect {
    private static boolean compareArrays( JSONArray a1, JSONArray a2 )
    {
        boolean res = true;
        if ( a1.size()==a2.size() )
        {
            for ( int i=0;i<a1.size();i++ )
            {
                Object o1 = a1.get(i);
                Object o2 = a2.get(i);
                if ( o1 != null && o2 != null 
                    && o1.getClass().equals(o2.getClass()) )
                {
                    if ( o1 instanceof JSONObject )
                        res = compareObjects((JSONObject)o1,(JSONObject)o2);
                    else if ( o1 instanceof Number )
                        res = o1.equals(o2);
                    else if ( o1 instanceof String )
                        res = o1.equals(o2);
                    else if ( o1 instanceof JSONArray)
                        res = compareArrays((JSONArray)o1,(JSONArray)o2);
                    else
                        res = false;
                }
                else
                    res = false;
            }
        }
        else
            res = false;
        return res;
    }
    private static boolean compareObjects( JSONObject o1, JSONObject o2)
    {
        boolean res = true;
        Set<String> keys = o1.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() && res )
        {
            String key = iter.next();
            Object obj1 = o1.get(key);
            Object obj2 = o2.get(key);
            if ( obj1 != null && obj2 != null 
                && obj1.getClass().equals(obj2.getClass()) )
            {
                if ( obj1 instanceof String )
                    res = obj1.equals(obj2);
                else if ( obj1 instanceof Number )
                    return obj1.equals(obj2);
                else if ( obj1 instanceof JSONArray )
                    res = compareArrays((JSONArray)obj1,(JSONArray)obj2);
                else if ( obj1 instanceof JSONObject )
                    res = compareObjects((JSONObject)obj1,(JSONObject)obj2);
                else
                    res = false;
            }
            else
                res = false;
        }
        return res;
    }
    /**
     * Is one dialect the same as another?
     * @param dialect1 the first dialect file
     * @param dialect2 the second dialect file
     * @return true if they are equal else false
     */
    public static boolean compare( JSONObject dialect1, JSONObject dialect2 )
    {
        JSONObject d1 = (JSONObject)dialect1.clone();
        JSONObject d2 = (JSONObject)dialect2.clone();
        // ignore generated ids
        d1.remove("_id");
        d2.remove("_id");
        return compareObjects(d1,d2);
    }
}
