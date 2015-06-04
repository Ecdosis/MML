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
import org.json.simple.JSONObject;
import java.util.Comparator;
import calliope.core.constants.JSONKeys;
/**
 * Comparator for annotations based on docid then version id
 * @author desmond
 */
public class DocidComparator implements Comparator<JSONObject> 
{
    public int compare( JSONObject obj1, JSONObject obj2 )
    {
        String docid1 = (String) obj1.get(JSONKeys.DOCID);
        String docid2 = (String) obj2.get(JSONKeys.DOCID);
        if ( docid1 != null && docid2 != null )
            return docid1.compareTo(docid2);
        else
            return 0;
    }
    public boolean equals( Object obj ) 
    {
        return this.equals(obj);
    }
}
