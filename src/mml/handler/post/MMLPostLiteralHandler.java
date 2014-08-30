/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.handler.post;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import mml.Utils;
import mml.constants.Database;
/**
 * Handle some kind of import
 * @author desmond
 */
public class MMLPostLiteralHandler extends MMLPostHandler
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        String first = Utils.first(urn);
        urn = Utils.pop(urn);
        if ( first.equals(Database.CORPIX) )
            new MMLPostImageHandler().handle(request,response,urn);
        else
            throw new MMLException("Unknown service "+first);
    }
}
