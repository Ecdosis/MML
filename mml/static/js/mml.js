/**
 * An MML Editor provides 3 panels which are sync-scrolled.
 * In the first panel there is a succession of page-images.
 * In the second an editable text in a minimal markup language (MML).
 * In the third a HTML preview generated from the editable text.
 * The MML dialect is defined via a JSON object.
 * To use it just create one:
 * var editor = new MMLEditor(opts,dialect);
 * @param opts the options neede to run MMLEditor:
 * source: the ID of a textarea on the page (no leading "#")
 * target: the ID of an empty div element (ditto)
 * images: the ID of the div to receive the images (ditto)
 * data: contains the keys 
 *      prefix - the prefix before each image name
 *      suffix: the suffix for each image name e.g. ".png"
 *      url: the url to fetch the images from
        desc: an array of ref, width and height keys for each image
 * @param dialect an MML dialect description in JSON format, see README.md
 */
function MMLEditor(opts, dialect) {
    /** set to true when source altered, controls updating */
    this.changed = true;
    /** set to false whenever use edits and does not save*/
    this.saved = true;
    /** flag to indicate if current para was formatted */
    this.formatted = false;
    /** flag to indicate when images have been loaded */
    this.imagesLoaded = false;
    /** flag for info displayed */
    this.infoDisplayed = false;
    /** page break RefLocs for html target */
    this.html_lines = new Array();
    /** page-breaks for images */
    this.image_lines = new Array();
    /** copy of options for MMLEditor */
    this.opts = opts;
    /** formatter converts MML to HTML */
    this.formatter = new Formatter(dialect);
    /** help info for dialect */
    this.info = new Info( "#help", dialect );
    /**
     * Check if we need to update the HTML. Gets called repeatedly.
     */
    this.updateHTML = function()
    {
        if ( this.changed )
        {
            this.text_lines = new Array();
            this.html_lines = new Array();
            var text = $("#"+this.opts.source).val();
            $("#"+this.opts.target).html(this.formatter.toHTML(text,this.text_lines));
            this.changed = false;
            $(".page").css("display","inline");
            var base = 0;
            var self = this;
            $(".page").each( function(i) {
                var pos = $(this).position().top;
                if ( base==0 && pos < 0 )
                    base = Math.abs(pos);
                self.html_lines.push(new RefLoc($(this).text(),pos+base));
                // inefficient but the only way
                $(this).css("display","none");
            });
            this.recomputeImageHeights();
        }
    };
    /**
     * Find the index of the highest value in the refarray 
     * less than or equal to the given value
     * @param list a sorted array of RefLocs
     * @param value the value of loc to search for
     * @return -1 if no element is less than or equal to, or the index  
     * of the highest element in refarray that is
     */
    this.findHighestIndex = function( list, value )
    {
        var top = 0;
        var bot = list.length-1;
        var mid=0;
        while ( top <= bot )
        {
            mid = Math.floor((top+bot)/2);
            if ( value < list[mid].loc )
            {
                if ( mid == 0 )
                    // value < than first item
                    return -1;  
                else
                    bot = mid-1;
            }
            else    // value >= list[mid].loc
            {
                if ( mid == list.length-1 )
                    // value is >= last item
                    break;
                else if ( value >= list[mid+1].loc )
                    top = mid+1;
                else // list[mid] must be biggest <= value
                    break;
            }
        }
        //console.log("value="+value+" mid="+mid);
        return mid;
    }
    /**
     * Find the index of the RefLoc in an array
     * @param array the array to look in
     * @param ref the reference value
     * @return the index of that value in the array or -1
     */
    this.findRefIndex = function( array, ref ) {
        for ( var i=0;i<array.length;i++ )
        {
            if ( array[i].ref == ref )
                return i;
        }
        return -1;
    };
    /**
     * Get the page number currently in view and the proportion 
     * of the page visible.
     * @param div the jQuery div object to get scroll info from
     * @param lines the lines array e.g. html_lines
     * @return a string being the ref of the page, comma, and 
     * fraction of page in view
     */
    this.getPixelPage = function( div, lines )
    {
        if ( this.formatter.num_lines > 0 && lines.length > 0 )
        {
            var scrollPos = div.scrollTop();
            var scrollHt = div[0].scrollHeight;
            var maximum;
            if ( div[0].scrollTopMax != undefined )
                maximum = div[0].scrollTopMax;
            else 
                maximum = scrollHt - div.outerHeight(true);
            if ( scrollPos == 0 )
                return lines[0].ref+",0.0";
            else if ( scrollPos == maximum )
                return lines[lines.length-1].ref+",1.0";
            else
            {
                // align on middle of target window
                scrollPos += div.height()/2;
                var index = this.findHighestIndex( lines, scrollPos ); 
                var pageHeight;
                if ( index == lines.length-1)
                {
                    pageHeight = scrollHt-lines[index].loc;
                }  
                else if ( index != -1 )
                {
                    pageHeight = lines[index+1].loc-lines[index].loc;
                }              
                else
                    return lines[0].ref+",0.0";
                var pageFraction = (scrollPos-lines[index].loc)/pageHeight;
                return lines[index].ref+","+pageFraction;
            }
        }
        else if (lines !=undefined&&lines.length>0)
            return lines[0].ref+",0.0";
        else 
            return ",0.0";
    };
    /**
     * Get the source page number currently in view in the textarea, 
     * and the line-number of the central line.
     * @param src the jQuery textarea element
     * @return a string being the ref of the page, comma, and 
     * fraction of page in view
     */
    this.getSourcePage = function( src )
    {
        if ( this.formatter.num_lines > 0 && this.text_lines.length > 0 )
        {
            var scrollPos = src.scrollTop();
            var maximum;
            var scrollHt = src[0].scrollHeight;
            if ( src[0].scrollTopMax != undefined )
                maximum = src[0].scrollTopMax;
            else 
                maximum = scrollHt - src.outerHeight(true);
            if ( scrollPos == 0 )
                return this.text_lines[0].ref+",0.0";
            else if ( scrollPos >= maximum )
                return this.text_lines[this.text_lines.length-1].ref+",1.0";
            else
            {
                scrollPos += src.height()/2;
                // convert scrollPos to lines
                var lineHeight = src.prop("scrollHeight")/this.formatter.num_lines;
                var linePos = Math.round(scrollPos/lineHeight);
                //console.log("linePos="+linePos+" scrollPos="+scrollPos);
                // find page after which linePos occurs
                var index = this.findHighestIndex(this.text_lines,linePos);
                var linesOnPage;
                if ( index == this.text_lines.length-1)
                {
                    linesOnPage = this.formatter.num_lines-this.text_lines[index].loc;
                }  
                else if ( index != -1 )
                {
                    var nextPageStart = this.text_lines[index+1].loc;
                    linesOnPage = nextPageStart-this.text_lines[index].loc;
                }              
                else
                    return this.text_lines.ref+",0.0";
                var fraction = (linePos-this.text_lines[index].loc)/linesOnPage;
                return this.text_lines[index].ref+","+fraction;
            }
        }
        else
            return this.text_lines.ref+",0.0";
    };
    /**
     * Recompute the height of the images after the window is fully loaded
     */
    this.recomputeImageHeights = function()
    {
        var currHt = 0;
        this.image_lines = [];
        var editor = this;
        $(".image").each( function(index) {
            var img = $(this).children().first();
            var ref = img.attr("data-ref");
            var imgHeight = img.height();
            editor.image_lines.push( new RefLoc(ref,currHt) );    
            currHt += imgHeight;
        });
    };
    /**
     * Scroll to the specified location
     * @param loc the location to scroll to, as {page ref},{fraction}
     * @param lines an array of RefLocs defining page-break positions
     * @param elemToScroll the jQuery element to scroll
     * scale the scale to apply to locs from the lines array to target
     */
    this.scrollTo = function( loc, lines, elemToScroll, scale ) {
        var parts = loc.split(",");
        var pos;
        var index = this.findRefIndex(lines,parts[0]);
        if ( index >= 0 )
            pos = lines[index].loc*scale;
        else
            pos = 0;
        //console.log(loc);
        var pageHeight;
        if ( index == -1 )
            pageHeight = 0;
        else if ( index < lines.length-1)
            pageHeight = (lines[index+1].loc*scale)-pos;
        else
            pageHeight = elemToScroll.prop("scrollHeight")-(lines[index].loc*scale);
        pos += Math.round(parseFloat(parts[1])*pageHeight);
        // scrolldown one half-pagescp js mml.js
        pos -= Math.round(elemToScroll.height()/2);
        if ( pos < 0 )
        {
            //console.log("pos="+pos);
            pos = 0;
        }
        if ( elemToScroll[0].scrollTopMax !=undefined && pos > elemToScroll[0].scrollTopMax )
            pos = elemToScroll[0].scrollTopMax;
        else if ( pos > elemToScroll[0].scrollHeight-elemToScroll[0].clientHeight )
        {
            pos = elemToScroll[0].scrollHeight-elemToScroll[0].clientHeight;
            //console.log(pos);
        }
        elemToScroll[0].scrollTop = pos; 
    };
    /**
     * Get the sum of the horizontal border, padding and optionally margin
     * @param jqObj the jQuery object to measure
     * @param marg if true add the horizontal margin values
     * @return the sum of the horizontal adjustments
     */
    this.hiAdjust = function( jqObj, marg )
    {
        var padLeft = parseInt(jqObj.css("padding-left"),10);
        var padRight = parseInt(jqObj.css("padding-right"),10);  
        var bordLeft = parseInt(jqObj.css("border-left-width"),10);
        var bordRight = parseInt(jqObj.css("border-right-width"),10);
        var margLeft = parseInt(jqObj.css("margin-left"),10);
        var margRight = parseInt(jqObj.css("margin-right"),10);
        var adjust = padLeft+padRight+bordLeft+bordRight;
        if ( marg )
            adjust += margLeft+margRight;
        return adjust;
    };
    /**
     * Get the sum of the vertical border, padding and optionally margin
     * @param jqObj the jQuery object to measure
     * @param marg if true add the vertical margin values
     * @return the sum of the vertical adjustments
     */
    this.viAdjust = function( jqObj, marg )
    {
        var padTop = parseInt(jqObj.css("padding-top"),10);
        var padBot = parseInt(jqObj.css("padding-bottom"),10);  
        var bordTop = parseInt(jqObj.css("border-top-width"),10);
        var bordBot = parseInt(jqObj.css("border-bottom-width"),10);
        var margTop = parseInt(jqObj.css("margin-top"),10);
        var margBot = parseInt(jqObj.css("margin-bottom"),10);
        var adjust = padTop+padBot+bordTop+bordBot;
        if ( marg )
            adjust += margTop+margBot;
        return adjust;
    };
    /**
     * Resize manually to parent element width, height to bottom of screen. 
     */
    this.resize = function() {
        var imgObj = $("#"+this.opts.images);
        var srcObj = $("#"+this.opts.source);
        var helpObj = $("#help");
        var tgtObj = $("#"+this.opts.target);
        var topOffset = imgObj.parent().position().top;
        var wHeight = $(window).height()-topOffset;
        var wWidth = imgObj.parent().outerWidth();
        // compute width
        imgObj.width(Math.floor(wWidth/3));
        tgtObj.width(Math.floor(wWidth/3)-this.hiAdjust(tgtObj));
        helpObj.width(Math.floor(wWidth/3)-this.hiAdjust(helpObj));
        srcObj.width(Math.floor(wWidth/3)-this.hiAdjust(srcObj));
        // compute height
        imgObj.height(wHeight);
        tgtObj.height(wHeight-this.viAdjust(tgtObj));
        helpObj.height(wHeight-this.viAdjust(helpObj));
        srcObj.height(wHeight-this.viAdjust(srcObj,true));
    };
    /**
     * Switch the display of help on or off. This replaces the textarea.
     */
    this.toggleHelp = function() {
        if ( !this.infoDisplayed )
        {
            this.infoDisplayed = true;
            $("#"+this.opts.source).css("display","none");
            $("#help").css("display","inline-block");
            $("#info").val("edit");
            $("#info").attr("title","back to editing");
            this.toggleInfo();
        }
        else
        {
            this.infoDisplayed = false;
            $("#help").css("display","none");
            $("#"+this.opts.source).css("display","inline-block");
            $("#info").val("info");
            $("#info").attr("title","about the markup");
            this.toggleInfo();
        }
        this.resize();
    };
    /**
     * Save the current state of the preview to the server
     */
    this.save = function() {
        var jsonStr = JSON.stringify(this.formatter.dialect);
        var html = $("#"+this.opts.target).html();
        var obj = {
            dialect: jsonStr,
            html: html, 
        };
        $("form").children().each( (function(obj) {
            return function() {
                obj[this.name] = $(this).val();
            }
        })(obj));
        var url = window.location.protocol
            +"//"+window.location.host
            +"/"+window.location.pathname.split("/")[1]
            +"/html";
        $.ajax( url, 
            {
                type: "POST",
                data: obj,
                success: $.proxy(function(data, textStatus, jqXHR) {
                        this.saved = true;
                        this.toggleSave();
                    },this),
                error: function(jqXHR, textStatus, errorThrown ) {
                    alert("Save failed. Error: "+textStatus+" ("+errorThrown+")");
                }
            }
        );
    };
    /**
     * Do whatever is needed to indicate that the document has/has not been saved
     */
    this.toggleSave = function() {
        if ( !this.saved  )
        {
            $("#save").removeAttr("disabled");
            $("#save").attr("title","save");
            $("#save").attr("class","save-button");
        }
        else
        {
            $("#save").attr("disabled","disabled");
            $("#save").attr("title","saved");
            $("#save").attr("class","saved-button");
            
        }
    };
    /**
     * Do whatever is needed to indicate the information status
     */
    this.toggleInfo = function() {
        if ( !this.infoDisplayed  )
            $("#info").attr("class","info-button");
        else
            $("#info").attr("class","edit-button");
    };
    // this sets up the timer for updating
    window.setInterval(
        (function(self) {
            return function() {
                self.updateHTML();
            }
        // this should really reset the interval based on how long it took
        })(this),300
    );
    // force update when user modifies the source
    $("#"+opts.source).keyup( 
        (function(self) {
            return function() {
                self.changed = true;
                if ( self.saved )
                {
                    self.saved = false;
                    self.toggleSave();
                }
            }
        })(this)
    );
    // scroll the textarea
    $("#"+opts.source).scroll( 
        (function(self) {
            return function(e) {
                // prevent feedback
                if ( e.originalEvent )
                {
                    var loc = self.getSourcePage($(this));
                    // console.log("loc sent to other scrollbars:"+loc);
                    self.scrollTo(loc,self.html_lines,$("#"+self.opts.target),1.0);
                    self.scrollTo(loc,self.image_lines,$("#"+self.opts.images),1.0);
                    //console.log($("#images")[0].scrollHeight);
                    //var height = 0;
                    //var images = $(".image");
                    //for ( var i=0;i<images.length;i++ )
                    //    height += images[i].clientHeight;
                    //console.log("overall height="+height);
                }
            }
        })(this)
    );
    // scroll the preview
    $("#"+opts.target).scroll(
        (function(self) {
            return function(e) {
                if ( e.originalEvent )
                {
                    var lineHeight = $("#"+self.opts.source).prop("scrollHeight")
                        /self.formatter.num_lines;
                    var loc = self.getPixelPage($(this),self.html_lines);
                    self.scrollTo(loc,self.text_lines,$("#"+self.opts.source),lineHeight);
                    // for some reason this causes feedback, but it works without!!
                    if ( self.infoDisplayed )
                        self.scrollTo(loc,self.image_lines,$("#"+self.opts.images),1.0);
                }
            }
        })(this)
    );
    // scroll the images
    $("#"+opts.images).scroll(
        (function(self) {
            return function(e) {
                if ( e.originalEvent )
                {
                    var lineHeight = $("#"+self.opts.source).prop("scrollHeight")
                        /self.formatter.num_lines;
                    var loc = self.getPixelPage($(this),self.image_lines);
                    self.scrollTo(loc,self.text_lines,$("#"+self.opts.source),lineHeight);
                    self.scrollTo(loc,self.html_lines,$("#"+self.opts.target),1.0);
                }
            }
        })(this)
    );
    /**
     * Find the correct number of LFs to prepend at the position given 
     * @param ta the textarea to examine
     * @param pos the start position within the textarea
     * @param nLFs number of line-feeds to prepend
     * @return a string containing the correct number of line-feeds to insert
     */
    this.ensureStart = function( ta, pos, nLFs ) {
        var text = ta.val();
        var n = 0;
        // subtract the number of existing LFs from nLFs
        for ( var i=pos-1;i>0;i-- )
            if ( text[i] == '\n' )
                n++;
            else if ( text[i] != '\t' && text[i] != ' ' )
                break;
        var str = "";
        for ( var i=0;i<nLFs-n;i++ )
            str += '\n';
        return str;
    };
    /**
     * Find the correct number of LFs to append to the position given 
     * @param ta the textarea to examine
     * @param pos the end position within the textarea
     * @param nLFs number of line-feeds to append
     * @return a string containing the correct number of line-feeds to add
     */
    this.ensureEnd = function( ta, pos, nLFs ) {
        var text = ta.val();
        var n = 0;
        // subtract the number of existing LFs from nLFs
        for ( var i=pos;i<text.length;i++ )
            if ( text[i] == '\n' )
                n++;
            else if ( text[i] != '\t' && text[i] != ' ' )
                break;
        var str = "";
        for ( var i=0;i<nLFs-n;i++ )
            str += '\n';
        return str;
    };
    /**
     * Find the number of non-LF whitespeaces preceding the selection
     * @param ta the textarea object
     * @param pos the start-position of the selection
     */
    this.leadingWSnotLF = function( ta, pos ) {
        var text = ta.val();
        var n = 0;
        for ( var i=pos-1;i>0;i-- )
        {
            if ( text[i] != ' ' && text[i] != '\t' )
                break;
            else
                n++;
        }
        return n;
    };
    /**
     * Find the number of non-LF whitespeaces following the selection
     * @param ta the textarea object
     * @param pos the end-position of the selection (1 after end)
     */
    this.trailingWSnotLF = function( ta, pos ) {
        var text = ta.val();
        var n = 0;
        for ( var i=pos;i<text.length;i++ )
        {
            if ( text[i] != ' ' && text[i] != '\t' )
                break;
            else
                n++;
        }
        return n;
    };
    /**
     * Wrap a block with prefixes and suffix, preserving correct WS
     * @param ta the textarea object
     * @param before the string to prepend to the selected text
     * @param after the string to append after the selected text
     * @param startLFs number of LFs to ensure precede
     * @param endLFs number of end LFs to ensure follow
     */
    this.wrapBlock = function( ta, before, after, startLFs, endLFs ) {
        var sel = ta.getSelection();
        var selText = sel.text.trim();
        var startLFs = this.ensureStart(ta,sel.start,startLFs);
        var endLFs = this.ensureEnd(ta,sel.end,endLFs);
        var leadingWS = this.leadingWSnotLF(ta,sel.start);
        var trailingWS = this.trailingWSnotLF(ta,sel.end);
        ta.setSelection(sel.start-leadingWS,sel.end+trailingWS);
        var prefix = "";
        sel = ta.getSelection();
        for ( var i=0;i<sel.text.length;i++ )
            if ( sel.text[i]==' '||sel.text[i]=='\t' )
                prefix += sel.text[i];
            else
                break;
        ta.replaceSelectedText(startLFs+before+selText+after+endLFs+prefix, 
            "collapseToEnd");
    };
    /**
     * Indent a block of text as a quotation or verbatim section
     * @param ta the textare containing the text
     * @param prefix prefix each line with this
     * @param level the number of times to apply the prefix
     * @param beforeLFs the number of LFs to ensure before
     * @param afterLFs the nmber of LFs to ensure after
     * @return the formatted text previously selected for replacement
     */
    this.indentBlock = function( ta, prefix, level, beforeLFs, afterLFs ) {
        var sel = ta.getSelection();
        var lines = sel.text.split("\n");
        if ( lines.length > 0 )
        {
            for ( var i=0;i<lines.length;i++ )
            {
                for ( var j=0;j<level;j++ )
                    lines[i] = prefix+lines[i];
            }
            var startLFs = this.ensureStart(ta,sel.start,beforeLFs);
            var endLFs = this.ensureEnd(ta,sel.end,afterLFs);
            lines[0] = startLFs+lines[0];
            lines[lines.length-1] = lines[lines.length-1]+endLFs;
        }
        return lines.join("\n");
    };
    this.getLevel = function( levelStr ) {
        levelStr = (levelStr!=undefined)?levelStr:"level1";
        var level = 0;
        for ( var i=0;i<levelStr.length;i++ )
        {
            if ( levelStr[i] >='0'&&levelStr[i]<='9' )
            {
                level *= 10;
                level += levelStr[i]-'0';
            }
        }
        return level;
    };
    // handle formatting menu actions
    var editor = this;
    $("#styles").mouseup(function(){
        var json = $("#styles").val().replace(/<q>/g,'"');
        var jobj = JSON.parse(json);
        var leftTag = "";
        var rightTag = "";
        var $ta = $("#"+opts.source);
        if ( jobj.tag != undefined )
            leftTag = rightTag = jobj.tag;
        else if ( jobj.leftTag != undefined && jobj.rightTag != undefined )
        {
            leftTag = jobj.leftTag;
            rightTag = jobj.rightTag;
        }
        if ( jobj.type == 'charformats' )
        {
            $ta.surroundSelectedText(leftTag, rightTag);
            editor.changed = true;
        }
        else if ( jobj.type == 'paraformats' )
        {
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                editor.wrapBlock($ta,leftTag,rightTag,2,2);
                editor.changed = true;
            }
        }
        else if ( jobj.type == 'headings' )
        {
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                var underlining = "";
                for ( var i=0;i<len;i++ )
                    underlining += jobj.tag;
                editor.wrapBlock($ta,"", "\n"+underlining,2,2);
                editor.changed = true;
            }
        }
        else if ( jobj.type == 'dividers' )
        {
            var sel = $ta.getSelection();
            if ( sel.end>sel.start )
                $ta.deleteSelectedText();
            editor.wrapBlock($ta, "", jobj.tag,2,2);
            editor.changed = true;
        }  
        else if ( jobj.type == 'milestones' )
        {
            editor.wrapBlock($ta, leftTag, rightTag,1,1);
            editor.changed = true;
        }  
        else if ( jobj.type == 'codeblocks' )
        {
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                var level = editor.getLevel(jobj.prop);
                var verbatim = editor.indentBlock($ta, "    ", level, 1, 2);
                $ta.replaceSelectedText(verbatim, "collapseToEnd");
                editor.changed = true;
            }
        }
        else if ( jobj.type == 'quotations' )
        {
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                var level = editor.getLevel(jobj.prop);
                var quoted = editor.indentBlock($ta,">",level,1,2);
                $ta.replaceSelectedText(quoted, "collapseToEnd");
                editor.changed = true;
            }
        }
        if ( editor.changed )
        {
            if ( editor.saved )
            {
                editor.saved = false;
                editor.toggleSave();
            }
        }
    });
    // This will execute whenever the window is resized
    $(window).resize(
        (function(self) {
            self.resize();
        })(this)
    );
    // generate help but keep it hidden for now
    this.info.makeInfo();
    /* setup window */
    this.resize();
}