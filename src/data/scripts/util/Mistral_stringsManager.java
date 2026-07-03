/*
By Tartiflette
 */
package data.scripts.util;

import com.fs.starfarer.api.Global;

public class Mistral_stringsManager {
    private static final String ML="mistral";
    
    public static String txt(String id){
        return Global.getSettings().getString(ML, id);
    }
}