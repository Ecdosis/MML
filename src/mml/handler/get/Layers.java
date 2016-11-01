/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mml.handler.get;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

/**
 *
 * @author desmond
 */
public class Layers {
    static final HashMap<String,Integer> values;
    static {
        values = new HashMap<String,Integer>();
        values.put("layer-1",1);
        values.put("layer-2",2);
        values.put("layer-3",3);
        values.put("layer-4",4);
        values.put("layer-5",5);
        values.put("layer-6",6);
        values.put("layer-7",7);
        values.put("layer-8",8);
        values.put("layer-9",9);
        values.put("layer-10",10);
        values.put("layer-final",Integer.MAX_VALUE);
        values.put("add0",2);
        values.put("del1",2);
        values.put("rdg1",1);
        values.put("rdg2",2);
        values.put("rdg3",3);
        values.put("base",1);
    }
    public static boolean isNewStyleLayer( String layer )
    {
        int index = layer.lastIndexOf("/");
        String tail = layer.substring(index+1);
        return tail.equals("layer-final")||tail.matches("layer-[0-9]+");
    }
    /**
     * Ensure that the version named is a pure version, devoid of layer suffixes
     * @param version the raw version name
     * @return a version name without a layer or add0,de1 etc designation
     */
    public static String stripLayer( String version )
    {
        StringBuilder sb = new StringBuilder();
        if ( version.equals("/base") || version.equals("base") )
            sb.append( "/base" );
        else
        {
            String[] parts = version.split("/");
            // process all except last part
            for ( int i=0;i<parts.length-1;i++ )
            {
                if ( parts[i].length()> 0 )
                {
                    sb.append("/");
                    sb.append(parts[i]);
                }
            }
            // now examine the last part
            if ( !values.containsKey(parts[parts.length-1]) 
                && parts[parts.length-1].length()>0  )
            {
                sb.append("/");
                sb.append(parts[parts.length-1]);
            }
        }
        return sb.toString();
    }
    /**
     * COnvert an old-style name to a new layer-based one
     * @param all all the versions in the set originally
     * @param layer the vid possibly including the old-style layer name
     * @return the full vid including the new layer-name
     */
    private static String convertLayer( String[] all, String layer )
    {
        String stripped = stripLayer(layer);
        ArrayList<String> layers = new ArrayList<String>();
        for ( int i=0;i<all.length;i++ )
        {
            if ( all[i].indexOf(stripped)==0 )
                layers.add(all[i]);
        }
        if ( layers.size()==1 )
            return stripped+"/layer-final";
        else
        {
            HashMap<Integer,String> reduced = new HashMap<Integer,String>();
            int value = 1;
            for ( int i=0;i<layers.size();i++ )
            {
                String bare = stripLayer(layers.get(i));
                String lay = layers.get(i).substring(bare.length()+1);
                int val = values.get(lay);
                if ( layer.equals(layers.get(i)) )
                    value = val;
                reduced.put(val,lay);
            }
            if ( value == reduced.size() )
                return stripped+"/layer-final";
            else
                return stripped+"/layer-"+value;
        }
    }
    /**
     * Upgrade an old-style layer name to the new style
     * @param all all the MVD version names in full
     * @param layer the old or new-style layer name
     */
    public static String upgradeLayerName( String[] all, String layer )
    {
        if ( layer.matches(".*/layer-[0-9]+$")|| layer.endsWith("/layer-final") )
            return layer;
        else
            return convertLayer(all,layer);
    }
    class LayerComparator implements Comparator<String>
    {
        /**
         * Compare two full version names including layer suffixes
         * @param s1 the first full version name
         * @param s2 the second full version name
         * @return -1 if s1 < s2, 1 if s1 > s2 else 0
         */
        public int compare( String s1, String s2 )
        {
            String ss1 = stripLayer(s1);
            String ss2 = stripLayer(s2);
            if ( !ss1.equals(ss2) )
                return ss1.compareTo(ss2);
            else if ( ss1.equals(s1) )  // s1 is layer-final
                return 1;
            else if ( ss2.equals(s2) )  // s2 is layer-final
                return -1;
            else
            {
                String s1Old = s1.substring(ss1.length()+1);
                String s2Old = s2.substring(ss2.length()+1);
                if ( s1Old.length()==0 )
                    return 1;
                else if ( s2Old.length()==0 )
                    return -1;
                else if ( values.containsKey(s1Old) && values.containsKey(s2Old) )
                {
                    int num1 = values.get(s1Old);
                    int num2 = values.get(s2Old);
                    if ( num1 < num2 )
                        return -1;
                    else if ( num1 > num2 )
                        return 1;
                    else
                        return 0;
                }
                else if ( values.containsKey(s1Old) )
                    return -1;
                else if ( values.containsKey(s2Old) )
                    return 1;
                else
                    return 0;
            }
        }
        public boolean equals(Object obj)
        {
            return this.equals(obj);
        }
    }
    public static void main(String[] args )
    {
        String[] all = {"/h080a","/h080d","/h080f","/h080g/base","/h080g/add0",
            "/h080h","/h080i/base","/h080i/add0","/h080i/rdg1","/h080i/rdg2",
            "/h080k","/h080l","/h080m"};

        String[] tests = new String[6];
        tests[0] = "/h080a";
        tests[1] = "/h080g/add0";
        tests[2] = "/h080i/layer-final";
        tests[3] = "/h080i/base";
        tests[4] = "/h080i/rdg2";
        tests[5] = "/h080/layer-3";
        for ( int i=0;i<tests.length;i++ )
        {
            String res = upgradeLayerName(all,tests[i]);
            System.out.println(tests[i]+" upgraded to "+res);
        }
    }
}
