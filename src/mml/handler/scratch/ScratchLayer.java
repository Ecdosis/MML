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
 *  (c) copyright Desmond Schmidt 2016
 */
package mml.handler.scratch;
import org.json.simple.JSONObject;
import calliope.core.constants.JSONKeys;
/**
 *
 * @author desmond
 */
public class ScratchLayer {
    String mml;
    String name;
    public ScratchLayer( String mml, String layerName )
    {
        this.name = layerName;
        this.mml = mml;
    }
    public JSONObject toJSONObject()
    {
        JSONObject jObj = new JSONObject();
        jObj.put( JSONKeys.NAME, name );
        jObj.put(JSONKeys.BODY, mml);
        return jObj;
    }        
}
