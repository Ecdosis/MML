function Formatter( dialect ) 
{
    /** dialect file of MML */
    this.dialect = dialect;
    /** quote chars for smartquotes */
    this.quotes = {"'":1,"‘":1,"’":1,'"':1,'”':1,'“':1};
    /** number of lines in textarea source */
    this.num_lines = 0;
    /** flag to indicate we are NOT busy */
    this.ready = true;
    
    /**
     * This should be a function of Array
     * @param the Array to test
     * @return the last element of Array, which is NOT modified
     */
    this.peek = function( stack )
    {
        return (stack.length==0)?undefined:stack[stack.length-1];
    };
    /**
     * Make a generic divider. It is a table with four cells.
     * @param prop the class name of the table and cell properties
     * @return the HTML table with class names suitable for CSS
     */
    this.makeDivider = function( prop )
    {
        var sb = "";
        sb += '<table class="';
        sb += prop;
        sb += '" title="';
        sb += prop;
        sb += '"><tr><td class="';
        sb += prop;
        sb += '-lefttop">';
        sb += "</td>";
        sb += '<td class="';
        sb += prop;
        sb += '-righttop">';
        sb += "</td></tr>";
        sb += '<tr><td class="';
        sb += prop;
        sb += '-leftbot">';
        sb += "</td>";
        sb += '<td class="';
        sb += prop;
        sb += '-rightbot">';
        sb += "</td></tr>";
        sb += "</table>";
        return sb;
    };
    /**
     * Process a paragraph for possible dividers
     * @param text the text to process
     * @return the possibly modified text
     */
    this.processDividers = function(text)
    {
        if ( this.dialect.dividers!=undefined )
        {
            var divs = this.dialect.dividers;
            for ( var i=0;i<divs.length;i++ )
            {
                var div = divs[i];
                if ( text.trim() == div.tag )
                {
                    text = this.makeDivider( div.prop );
                    this.formatted = true;
                }
            }
        }
        return text;
    };
    /**
     * Get a curly close quote character 
     * @param quote the quote to convert
     * @return the Unicode curly variant
    */
    this.closeQuote = function( quote )
    {
        if ( quote=="'" )
            return "’";
        else if ( quote == '"' )
            return '”';
        else
            return quote;
    };
    /**
     * Get a curly opening quote character 
     * @param quote the quote to convert
     * @return the Unicode curly variant
    */
    this.openQuote = function( quote )
    {
        if ( quote=="'" )
            return "‘";
        else if ( quote == '"' )
            return '“';
        else
            return quote;
    };
    /**
     * Is this a curly or straight quote char?
     * @param c the quote to test
     * @return true if it is
     */
    this.isQuote= function( c )
    {
        return c in this.quotes;
    };
    /**
     * Is this a plain space char?
     * @param c the char to test
     * @return true if it is
     */
    this.isSpace = function( c )
    {
        return c == '\t'||c==' ';
    };
    /**
     * Is this an opening bracket of some kind?
     * @param c the char to test
     * @return true if it is
     */
    this.isOpeningBracket = function(c)
    {
        return c=='['||c=='('||c=='{'||c=='<';
    };
    /**
     * Specifically test for an opening quote char
     * @param c the char to test
     * @return true if it is
     */
    this.isOpeningQuote = function(c)
    {
        return c=="‘"||c=='“';
    };
    /**
     * Convert smart quotes as fast as possible. Do this first.
     * @param text the text of a paragraph
     * @return the modified text
     */
    this.processSmartQuotes = function( text )
    {
        if ( this.dialect.smartquotes )
        {
            for ( var i=0;i<text.length;i++ )
            {
                var c = text[i];
                if ( this.isQuote(c) )
                {
                    var prev = text[i-1];
                    if ( i==0||(this.isSpace(prev)
                        ||this.isOpeningQuote(prev)||this.isOpeningBracket(prev)) )
                        text = text.slice(0,i)+this.openQuote(c)+text.slice(i+1);
                    else
                        text = text.slice(0,i)+this.closeQuote(c)+text.slice(i+1);
                }
            }
        }
        return text;         
    };
    /**
     * Search for and replace all character formats in the paragraph
     * @param text the text of the paragraph
     * @return the possibly modified text with spans inserted
     */ 
    this.processCfmts = function(text)
    {
        if ( this.dialect.charformats != undefined )
        {
            var cfmts = this.dialect.charformats;
            var tags = new Array();
            var stack = new Array();
            for ( var k=0;k<cfmts.length;k++ )
            {
                var cfmt = cfmts[k];
                if ( cfmt.tag != undefined )
                    tags[cfmt.tag] = (cfmt.prop!=undefined)?cfmt.prop:cfmt.tag;
            }
            // add default soft-hyphens
            tags["-\n"] = "soft-hyphen";
            var i = 0;
            while ( i<text.length )
            {
                var c = text[i];
                for ( var tag in tags )
                {
                    var j;
                    for ( j=0;j<tag.length;j++ )
                    {
                        if ( text[i+j]!=tag[j] )
                            break;
                    }
                    if ( j == tag.length )
                    {
                        if ( this.dialect.softhyphens != undefined 
                            && this.dialect.softhyphens && tag == "-\n" )
                        {
                            text = text.slice(0,i)
                                +'<span class="soft-hyphen">-</span>'
                                +text.slice(i+2);
                            i += 34;
                        }
                        else if ( stack.length>0&&this.peek(stack)==tag )
                        {
                            stack.pop();
                            text = text.slice(0,i)+"</span>"
                                +text.slice(i+tag.length);
                            i += 6 -tag.length;
                        }
                        else
                        {
                            stack.push(tag);
                            text = text.slice(0,i)+'<span class="'
                                +tags[tag]+'" title="'+tags[tag]+'">'
                            +text.slice(i+tag.length);
                            i += 14+(tags[tag].length-tag.length);
                        }
                    }
                }
                i++; 
            }
        }
        // else do nothing
        return text;
    };
    /**
     * Find start of tag after leading white space
     * @param text the text to search
     * @param tag the tag to find at the end
     * @return -1 on failure else index of tag-start at end of text
     */
    this.startPos = function( text, tag )
    {
        var i = 0;
        while ( i<text.length&&(text.charAt(i)=='\t'||text.charAt(i)==' ') )
            i++;
        if ( text.indexOf(tag)==i )
            return i;
        else
            return -1;
    };
    /**
     * Find the last instance of tag before trailing white space
     * @param text the text to search
     * @param tag the tag to find at the end
     * @return -1 on failure else index of tag-start at end of text
     */
    this.endPos = function( text, tag )
    {
        var i = text.length-1;
        while ( i>=0 )
        {
            if ( text[i]==' '||text[i]=='\n'||text[i]=='\t' )
                i--;
            else
                break;
        }
        var j = tag.length-1;
        while ( j >= 0 )
        {
            if ( tag[j] != text[i] )
                break;
            else
            {
                j--;
                i--;
            }
        }
        return (j==-1)?i+1:-1;
    };
    /**
     * Scan the start and end of the paragraph for defined para formats.
     * @param text the text to of the paragraph 
     * @return the possibly modified text with p-formats inserted
     */
    this.processPfmts = function( text )
    {
        if ( this.dialect.paraformats !=undefined )
        {
            var pfmts = this.dialect.paraformats;
            for ( var i=0;i<pfmts.length;i++ )
            {
                var pfmt = pfmts[i];
                if ( pfmt.leftTag != undefined && pfmt.rightTag != undefined )
                {
                    var ltag = pfmt.leftTag;
                    var rtag = pfmt.rightTag;
                    var lpos = this.startPos(text,ltag);
                    var rpos = this.endPos(text,rtag);
                    if (  lpos != -1 && rpos != -1 )
                    {
                        text = '<p class="'+pfmt.prop+'"'
                            +' title="'+pfmt.prop+'">'
                            +text.slice(lpos+ltag.length,rpos)+'</p>';
                        this.formatted = true;
                        break;
                    }
                }
            }
        }
        return text;
    };
    /**
     * Get the indent level of this line
     * @param line the line with some leading spaces
     * @return the level (4 spaces or a tab == 1 level)
     */
    this.getLevel = function( line )
    {
        var level = 0;
        var spaces = 0;
        var j;
        for ( j=0;j<line.length;j++ )
        {
            var token = line.charAt(j);
            if ( token =='\t' )
                level++;
            else if ( token==' ' )
            {
                spaces++;
                if ( spaces >= 4 )
                {
                    level++;
                    spaces = 0;
                }
            }
            else
                break;
        }
        // completely blank lines are NOT indented
        return (j==line.length)?0:level;
    };
    /**
     * Start a new level of preformatting
     * @param level the depth of the level (greater than 0)
     */
    this.startPre = function( level )
    {
        var text = "<pre";
        var prop = this.dialect.codeblocks[level-1].prop;
        if ( prop != undefined && prop.length > 0 )
            text += ' class="'+prop+'">';
        else
            text += '>';
        this.formatted = true;
        return text;
    };
    /**
     * Remove leading white space. If no such whitespace do nothing.
     * @param line the line to remove it from
     * @param level the level of the preformatting
     * @return the modified line
     */
    this.leadTrim = function(line,level)
    {
        for ( var i=0;i<level;i++ )
        {
            if ( line.indexOf("    ")==0 )
                line = line.substring(4);
            else if ( line.indexOf("\t")==0)
                line = line.substring(1);
        }
        return line;
    };
    /**
     * Look for four leading white spaces and format as pre
     * @param text the text of the paragraph
     */
    this.processCodeBlocks = function( input )
    {
        var text = "";
        if ( this.dialect.codeblocks!=undefined )
        {
            var lines = input.split("\n");
            var level = 0;
            var mss = (this.dialect.milestones!=undefined
                &&this.dialect.milestones.length>0)
                ?this.dialect.milestones:undefined;
            for ( var i=0;i<lines.length;i++ )
            {
                var currLevel = this.getLevel(lines[i]);
                if ( mss == undefined || this.isMilestone(lines[i],mss)==undefined )
                {
                    if ( currLevel > level )
                    {
                        if ( level > 0 )
                            text += '</pre>';
                        if ( currLevel <= this.dialect.codeblocks.length )
                            text += this.startPre(currLevel);
                        else // stay at current level
                            currLevel = level;
                    }
                    else if ( currLevel < level )
                    {
                        text += '</pre>';
                        if ( currLevel > 0 )
                            text += this.startPre(currLevel);
                    }
                    level = currLevel;
                }
                text += (lines[i].length>0)?this.leadTrim(lines[i],currLevel):"";
                if ( i < lines.length-1 )
                    text += '\n';
            }
            if ( level > 0 )
                text += "</pre><p></p>";
        }
        else
            text = input;
        return text;
    };
    /**
     * Get the quote depth of the current line
     * @paramline the line to test for leading >s
     * @return the number of leading >s followed by spaces
     */
    this.quoteDepth = function( line )
    {
        var state = 0;
        var depth = 0;
        for ( var i=0;i<line.length;i++ )
        {
            var c = line.charAt(i);
            switch ( state )
            {
                case 0: // looking for ">"
                    if ( c=='>' )
                    {
                        depth++;
                        state = 1;
                    }
                    else if ( c!=' '&&c!='\t' )
                        state = -1;
                    break;
                case 1: // looking for obligatory space
                    if ( c==' '||c=='\t' )
                        state = 0;
                    else
                        state = -1;
                    break;
        
            }
            if ( state == -1 )
                break;
        }
        return depth;
    };
    /**
     * Strip the leading quotations from a line
     * @param line
     * @return the line with leading quotations (>s) removed
     */
    this.stripQuotations = function( line )
    {
        var i = 0;
        var c = (line.length>0)?line.charAt(0):undefined;
        if ( this.startPos(line,">")==0 )
        {
            while ( i<line.length && (c=='>'||c=='\t'||c==' ') )
            {
                i++;
                if ( i < line.length )
                    c = line.charAt(i);
            }
        }
        return line.slice(i);
    };
    /**
     * Quotations are lines starting with "> "
     * @param text the text to scan for quotations and convert
     * @return the possibly formatted paragraph
     */
    this.processQuotations = function(text)
    {
        if ( this.dialect.quotations != undefined )
        {
            var old;
            var res = "";
            var attr = (this.dialect.quotations.prop!=undefined
                &&this.dialect.quotations.prop.length>0)
                ?' class="'+this.dialect.quotations.prop+'"':"";
            var stack = new Array();
            var lines = text.split("\n");
            for ( var i=0;i<lines.length;i++ )
            {
                var depth = this.quoteDepth(lines[i]);
                if ( depth > 0 )
                {
                    if ( this.peek(stack) != depth )
                    {
                        if ( stack.length==0||this.peek(stack)<depth )
                        {
                            for ( var j=stack.length;j<depth;j++ )
                                res += "<blockquote"+attr+'>';
                            stack.push(depth);
                        }
                        else if ( depth < this.peek(stack) )
                        {
                            old = stack.pop();
                            while ( old != undefined && old>depth )
                            {
                                res +="</blockquote>";
                                depth = old;
                            }
                        }
                    }
                }
                res += this.stripQuotations(lines[i])+"\n";
            }
            old = this.peek(stack);
            while ( old != undefined && old > 0 )
            {
                old = stack.pop();
                if ( old != undefined )
                    res +="</blockquote>";
            }
            text = res;
            if ( this.startPos(text,"<blockquote")==0 
                && this.endPos(text,"</blockquote>")==text.length-13 )
                this.formatted = true;
        }
        return text;
    };
    /**
     * Does the given line define a heading for the line above?
     * @param line the line to test - should be all the same character
     * @param c the character that should be uniform
     * @return true if it qualifies
     */
    this.isHeading = function( line, c )
    {
        var j = 0;
        for ( ;j<line.length;j++ )
        {
            if ( line.charAt(j) !=c )
                break;  
        }
        return j == line.length;
    };
    /**
     * Is the current line a milestone?
     * @para, line the line to test
     * @param mss an array of milestone defs
     * @return the relevant milestone
     */
    this.isMilestone = function( line, mss )
    {
        var line2 = line.trim();
        for ( var i=0;i<mss.length;i++ )
        {
            var ms = mss[i];
            if ( this.startPos(line2,ms.leftTag)==0 
                && this.endPos(line2,ms.rightTag)==line2.length-ms.rightTag.length )
                return ms;
        }
        return undefined;
    };
    /**
     * Process setext type headings (we don't do atx). Oh, and do milestones.
     * @param text the text to give headings to
     * @return the possibly modified text
     */
    this.processHeadings = function( text )
    {
        if ( this.dialect.headings !=undefined )
        {
            var i;
            var res = "";
            var mss = (this.dialect.milestones!=undefined&&this.dialect.milestones.length>0)
                ?this.dialect.milestones:undefined;
            var heads = new Array();
            var tags = new Array();
            for ( i=0;i<this.dialect.headings.length;i++ )
            {
                if ( this.dialect.headings[i].prop != undefined 
                    && this.dialect.headings[i].tag != undefined )
                {
                    heads[this.dialect.headings[i].tag] = this.dialect.headings[i].prop;    
                    tags[this.dialect.headings[i].prop] = 'h'+(i+1);
                }
            }
            var lines = text.split("\n");
            for ( i=0;i<lines.length;i++ )
            {
                var ms;
                var line = lines[i];
                this.num_lines++;
                if ( line.length > 0 )
                {
                    var c = line.charAt(0);
                    if ( c in heads && i>0 && this.isHeading(lines[i],c) )
                    {
                        var attr = ' class="'+heads[c]+'" title="'+heads[c]+'"';
                        res += '<'+tags[heads[c]]+attr+'>'+lines[i-1]
                            +'</'+tags[heads[c]]+'>\n';  
                        this.formatted = true; 
                    }
                    else if ( mss != undefined 
                        && (ms=this.isMilestone(line,mss))!=undefined )
                    {
                        var ref = line.slice(ms.leftTag.length,
                            this.endPos(line,ms.rightTag));
                        if ( ms.prop=="page" )
                        {
                            //console.log("ref="+ref+" num_lines="+this.num_lines);
                            this.text_lines.push(new RefLoc(ref,this.num_lines));
                        }
                        res += '<span class="'+ms.prop+'">'
                            +ref+'</span>';
                    }
                    else if ( i == lines.length-1 )
                         res += line+'\n';
                    else
                    {
                        var next = lines[i+1];
                        var d = next.charAt(0);
                        if ( !(d in heads && this.isHeading(next,d)) )
                            res += line+'\n';
                    }
                }
            }
            text = res;
        }
        return text;
    };
    /**
     * Process an entire paragraph
     * @param text the text to process
     * @return the possibly modified text with HTML codes inserted
     */
    this.processPara = function( text )
    {
        this.formatted = false;
        var attr = (this.dialect.paragraph!=undefined
            &&this.dialect.paragraph.prop!=undefined
            &&this.dialect.paragraph.prop.length>0)
            ?' class="'+this.dialect.paragraph.prop+'" title="'
            +this.dialect.paragraph.prop+'"':"";
        text = this.processSmartQuotes(text);
        text = this.processCodeBlocks(text);
        text = this.processHeadings(text);
        text = this.processQuotations(text);
        text = this.processPfmts(text);
        text = this.processDividers(text);
        text = this.processCfmts(text);
        if ( !this.formatted && text.length > 0 )
            text = '<p'+attr+'>'+text+'</p>';
        //console.log("num_lines="+this.num_lines);
        return text;
    };
    /**
     * Process all the paras in a section
     * @param section the text of the section
     * @return the modified content of the section
     */
    this.processSection = function( section )
    {
        var html = "";
        var paras = section.split("\n\n");
        for ( var i=0;i<paras.length;i++ )
        {
            if ( paras[i].length > 0 )
                html += this.processPara(paras[i]);
            this.num_lines++;
        }
        return html;
    };
    /** 
     * Does this section only contains white space?
     * @param section the text of the section
     * @return true
     */
    this.isEmptySection = function(section) {
        var empty = true;
        for ( var i=0;i<section.length;i++ )
        {
            var c = section.charAt(i);
            if ( c!='\t'||c!=' '||c!= '\n' )
                return false;
        }
        return true;
    };
    /**
     * Convert the MML text into HTML
     * @param text the text to convert
     * @return HTML
     */
    this.toHTML = function(text,text_lines)
    {
        var html = "";
        this.num_lines = 0;
        this.text_lines = text_lines;
        var sectionName = (this.dialect.section!=undefined
            &&this.dialect.section.prop!=undefined)
            ?this.dialect.section.prop:"section";
        var sections = text.split("\n\n\n");
        if ( this.isEmptySection(sections[sections.length-1]) )
            sections = sections.slice(0,sections.length-1);
        for ( var i=0;i<sections.length;i++ )
        {
            html+= '<div class="'+sectionName+'">'
                +this.processSection(sections[i]);
            html += '</div>';
            this.num_lines ++;
        }
        //console.log("num_lines="+this.num_lines);
        return html;
    };
}
