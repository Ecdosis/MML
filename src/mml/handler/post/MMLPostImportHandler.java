/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.handler.post;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import calliope.core.Utils;
import mml.constants.Service;
/**
 * Handle some kind of import
 * @author desmond
 */
public class MMLPostImportHandler extends MMLPostHandler
{
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        String first = Utils.first(urn);
        urn = Utils.pop(urn);
        if ( first.equals(Service.LITERAL) )
            new MMLPostLiteralHandler().handle(request,response,urn);
        else
            throw new MMLException("Unknown service "+first);
    }
}
