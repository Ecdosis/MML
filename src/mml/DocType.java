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
package mml;

import calliope.core.constants.JSONKeys;
import mml.constants.Formats;
import org.json.simple.JSONObject;

/**
 * Names for calliope document types
 * @author desmond
 */
public enum DocType {
    CORTEX,
    CORCODE,
    ANNOTATION,
    UNKNOWN;
    public static DocType classifyObj( JSONObject jObj )
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
}
