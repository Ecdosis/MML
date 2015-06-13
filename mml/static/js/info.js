/*
 * This is the display that describes the dialect file
 */
function Info( helpId, dialect )
{
    this.helpId = helpId;
    this.dialect = dialect;
    /**
     * Called by makeInfo
     * @param name the name of the section/paragraph
     * @param prop the property it will have if any (undefined or empty)
     * @param by description of how it will be labelled
     * @return the string composed of this info
     */
    this.describeSimpleProp = function(name,prop,by) {
        var info = "<p><b>"+name+"</b> will be marked by "+by;
        if ( prop != undefined && prop.prop != undefined && prop.prop.length > 0 )
            info += ", and will be labelled '"+prop.prop+"'</p>\n";
        else
            info += ".</p>\n";
        return info;
    };
    /**
     * Build the info about the dialect from the dialect description
     */
    this.makeInfo = function() {
        var help = $(helpId);
        var i;
        var info = "";
        info += "<h2>Novel markup for De Roberto</h2>";
        if ( this.dialect.sections )
        {
            info += "<p><b>Sections:</b> are formed by two blank lines, "
            +"optionally followed on a new line by a name enclosed in braces:</p>";
            if ( this.dialect.sections.length>0 )
            {
                info += "<ul>";
                for ( var i=0;i<this.dialect.sections.length;i++ )
                {
                    if ( this.dialect.sections[i].prop.length>0 )
                    {
                        info += "<li>";
                        info += "{"+this.dialect.sections[i].prop+"}"
                        info += "</li>";
                    }
                }
                info += "</ul>";
            }
        }
        info += this.describeSimpleProp("Paragraphs",this.dialect.paragraphs,"one blank line");
        info += this.describeSimpleProp("Quotations",this.dialect.quotations,
            "initial '> ', which may be nested");
        if ( this.dialect.softhyphens )
        {
            info += "<p><b>Hyphens:</b> Lines ending in '-' will be joined up, "
             +"and the hyphen labelled 'soft-hyphen', which will be invisible but "
             +"still present.</p>";
            info += "<p>Lines ending in '--' will be joined to the next line but "
            +"one hyphen will remain. These wil be labelled as 'hard-hyphens' on save.</p>";
        }
        else
            info += "<p><b>Hyphens:</b> Lines ending in '-' followed by a new line "
                 +"will <em>not</em> be joined up.</p>";
        if ( this.dialect.smartquotes )
            info += "<p>Single and double plain <b>quotation marks</b> will be converted "
                 +"automatically into curly quotes.</p>";
        else
            info += "<p>Single and double plain <b>quotation marks</b> will be left unchanged.</p>";
        if ( this.dialect.codeblocks != undefined && this.dialect.codeblocks.length > 0 )
        {
            info += "<h3>Preformatted sections</h3><p>The following are defined:</p>";
            for ( i=0;i<this.dialect.codeblocks.length;i++ )
            {
                var h = this.dialect.codeblocks[i];
                var level = i+1;
                info += "<p>A line starting with "+(level*4)+" spaces will be treated"
                    "as preformatted and will be indented to tab-stop "+level;
                if ( h.prop != undefined && h.prop.length>0 )
                     info += ", and will be labelled '"+h.prop+"'.</p>";
                else
                     info += ".</p>";
            }
        }
        if ( this.dialect.headings != undefined && this.dialect.headings.length > 0 )
        {
            info += "<h3>Headings</h3><p>The following are defined:</p>";
            for ( i=0;i<this.dialect.headings.length;i++ )
            {
                var h = this.dialect.headings[i];
                var level = i+1;
                info += "<p>Text on a line followed by another line consisting entirely of "
                     +h.tag+" characters will be displayed as a heading level "+level;
                if ( h.prop != undefined && h.prop.length>0 )
                     info += ", and will be labelled '"+h.prop+"'.</p>";
                else
                     info += ".</p>";
            }
        }
        if ( this.dialect.dividers != undefined && this.dialect.dividers.length>0 )
        {
            info += "<p><h3>Dividers</h3>The following are defined:</p>";
            for ( i=0;i<this.dialect.dividers.length;i++ )
            {
                var d = this.dialect.dividers[i];
                if ( d.prop != undefined )
                    info += "<p>"+d.tag+" on a line by itself will be drawn in "
                         +"accordance with the stylesheet definition for '"
                         +d.prop+"', and will be labelled '"+d.prop+"'.</p>";
            }
        }
        if ( this.dialect.charformats != undefined && this.dialect.charformats.length>0 )
        {
            info += "<h3>Character formats</h3><p>The following are defined:</p>";
            for ( i=0;i<this.dialect.charformats.length;i++ )
            {
                var c = this.dialect.charformats[i];
                if ( c.prop != undefined )
                    info += "<p>Text within a paragraph that begins and ends with '"+c.tag
                         + "' will be drawn in accordance with the stylesheet definition for '"
                         + c.prop+"', and will be labelled '"+c.prop+"'.</p>";
            }
        }
        if ( this.dialect.paraformats != undefined && this.dialect.paraformats.length>0 )
        {
            info += "<h3>Paragraph formats</h3><p>The following are defined:</p>";
            for ( i=0;i<this.dialect.paraformats.length;i++ )
            {
                var p = this.dialect.paraformats[i];
                if ( p.prop != undefined && p.leftTag != undefined && p.rightTag != undefined )
                    info += "<p>Text separated by one blank line before and after, "
                         + "with '"+p.leftTag+"' at the start and '"+p.rightTag+"' at the end "
                         + "will be drawn in accordance with the stylesheet definition for "
                         + p.prop+", and will be labelled '"+p.prop+"'.</p>";
            }
        }
        if ( this.dialect.milestones != undefined && this.dialect.milestones.length>0 )
        {
            info += "<h3>Milestones</h3><p>The following are defined:</p>";
            for ( i=0;i<this.dialect.milestones.length;i++ )
            {
                var m = this.dialect.milestones[i];
                if ( m.prop != undefined && m.leftTag != undefined && m.rightTag != undefined )
                    info += "<p>A line preceded by '"+m.leftTag+"' and followed by '"+m.rightTag
                         +"' will mark an invisible dividing point that will be labelled '"
                         + m.prop+"', and will have the value of the textual content.";
                if ( m.prop=="page" )
                    info += " The page milestone will be used to align segments of the "
                        + "transcription to the preview, and to fetch page images with that name.</p>";
                else
                    info += "</p>";
            }
        }
        $(helpId).html(info);
    };
}
