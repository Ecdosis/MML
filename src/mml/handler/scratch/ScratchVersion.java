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
import calliope.core.constants.JSONKeys;
import java.util.HashMap;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.Set;
import java.util.Arrays;
import java.util.Iterator;
import org.json.simple.JSONValue;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * A scratch version will be stored in the scratch database
 * @author desmond
 */
public class ScratchVersion {
    /** name of version minus the "-layer-* stuff */
    String version;
    /** The database it came from originally */
    String dbase;
    /** docid if built from BSON */
    String docid;
    /** map to prevent same layer being added twice */
    HashMap<String,String> layers;
    /**
     * Create a Scratch version from SCRATCH
     * @param name the name or vid of this version
     * @param docid the docid of the resource
     * @param dbase the dbase it belongs to (e.g. CORTEX)
     */
    public ScratchVersion( String name, String docid, String dbase )
    {
        this.version = cleanVersionName(name);
        this.dbase = dbase;
        this.docid = docid;
    }
    /**
     * Remove -layer-x suffix
     * @param name the full version name
     * @return the shortened version name
     */
    private String cleanVersionName( String name )
    {
        if ( name.matches(".*-layer-.*" ))
        {
            int pos = name.lastIndexOf("-layer-");
            if ( pos > 0 )
                name = name.substring(0,pos);
        }
        return name;
    }
    public int size()
    {
        return (layers==null)?0:layers.size();
    }
    public boolean isSimple()
    {
        return layers != null && layers.size()==1;
    }
    /**
     * Get the docid 
     * @return the docid 
     */
    public String getDocid()
    {
        return this.docid;
    }
    /**
     * Get the dbase we belong to, ultimately, dude 
     * @return the dbase 
     */
    public String getDbase()
    {
        return this.dbase;
    }
    /**
     * Get the name of a layer corresponding to its number
     * @param num layer number starting at 1 (Integer.MAX_VALUE is "final")
     * @return the layer NAME
     */
    public static String layerName( int num )
    {
        if ( num == Integer.MAX_VALUE )
            return "layer-final";
        else
            return "layer-"+Integer.toString(num);
    }
    /**
     * Get the layer number from its NAME
     * @param shortName the name e.g. layer-1 or layer-final
     * @return the numeric representaiton of the layer name
     */
    public static int layerNumber( String shortName )
    {
        String[] parts = shortName.split("-");
        if ( parts[parts.length-1].equals("final") )
            return Integer.MAX_VALUE;
        else
            return Integer.parseInt(parts[parts.length-1]);
    }
    /**
     * Add a new layer or obliterate the same one already there
     * @param vdata the raw character data from the MVD
     * @param num the number of the layer (Integer.MAX_VALUE = "-layer-final")
     */
    public void addLayer( char[] vdata, int num )
    {
        if ( layers == null )
            layers = new HashMap<String,String>();
        layers.put( layerName(num), new String(vdata) );
    }
    /**
     * Get the true name of the default version
     * @return a real vpath
     */
    public String getDefaultVersion()
    {
        Set<String> keys = layers.keySet();
        Iterator<String> iter = keys.iterator();
        int lay = 0;
        String dfltVersion = "";
        while ( iter.hasNext() )
        {
            String key = iter.next();
            int num = layerNumber(key);
            if ( num > lay )
            {
                lay = num;
                dfltVersion = key;
            }
        }
        return this.version+"-"+dfltVersion;
    }
    /**
     * Extract a spcific layer
     * @param layer
     * @return the layer contents or null
     */
    public String getLayerString( int layer )
    {
        Set<String> keys = layers.keySet();
        Iterator<String> iter = keys.iterator();
        while ( iter.hasNext() )
        {
            String layerName = iter.next();
            if ( ScratchVersion.layerNumber(layerName) == layer )
            {
                String body = layers.get(layerName);
                return body;
            }
        }
        return null;
    }
    /**
     * Get an array of layer numbers
     * @return an int array
     */
    public int[] getLayerNumbers()
    {
        int[] arr = new int[layers.size()];
        Set<String> keys = layers.keySet();
        Iterator<String> iter = keys.iterator();
        int i = 0;
        while ( iter.hasNext() )
        {
            String key = iter.next();
            arr[i++] = ScratchVersion.layerNumber(key);
        }
        return arr;
    }
    static String simpleName( String name )
    {
        String[] parts = name.split("-");
        return parts[parts.length-1];
    }
    public static String simpleLayerName( int num )
    {
        return simpleName(layerName(num));
    }
    /**
     * Convert this version to JSON so it can stored in the database
     * @return the entire version as a string
     */
    public String toJSON()
    {
        JSONObject jObj = new JSONObject();
        if ( layers != null )
        {
            Set<String> keys = layers.keySet();
            String[] arr = new String[keys.size()];
            keys.toArray(arr);
            Arrays.sort(arr);
            jObj.put( JSONKeys.VERSION1, version );
            Calendar cal = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            jObj.put(JSONKeys.TIME, sdf.format(cal.getTime()) );
            jObj.put("dbase", dbase);
            JSONArray jArr = new JSONArray();
            jObj.put( "layers", jArr );
            for ( int i=0;i<arr.length;i++ )
            {
                JSONObject jLayer = new JSONObject();
                jLayer.put(JSONKeys.NAME,arr[i]);
                jLayer.put(JSONKeys.BODY,layers.get(arr[i]));
                jArr.add(jLayer);
            }
        }
        return jObj.toJSONString();
    }
    /**
     * Convert a BSON object to a Scratch version internal format
     * @param json the bson object from the database
     * @return a ScratchVersion with layers
     */
    public static ScratchVersion fromJSON( String json )
    {
        JSONObject jObj = (JSONObject)JSONValue.parse(json);
        ScratchVersion sv = new ScratchVersion(
            (String)jObj.get(JSONKeys.VERSION1),
            (String)jObj.get(JSONKeys.DOCID),
            (String)jObj.get(JSONKeys.DBASE));
        JSONArray jArr = (JSONArray)jObj.get("layers");
        if ( jArr != null )
        {
            for ( int i=0;i<jArr.size();i++ )
            {
                JSONObject jLayer = (JSONObject)jArr.get(i);
                String layerName = (String)jLayer.get(JSONKeys.NAME);
                String body = (String)jLayer.get(JSONKeys.BODY);
                int layerNum = sv.layerNumber(sv.version+"-"+layerName);
                sv.addLayer(body.toCharArray(),layerNum);
            }
        }
        return sv;
    }
}
