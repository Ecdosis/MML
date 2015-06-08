/**
 * An MML Editor provides 3 panels which are sync-scrolled.
 * In the first panel there is a succession of page-images.
 * In the second an editable text in a minimal markup language (MML).
 * In the third a HTML preview generated from the editable text.
 * The MML dialect is defined via a JSON object.
 * To use it just create one:
 * var editor = new MML(opts,dialect);
 * @param opts the options neede to run MML:
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
function MML(opts, dialect) {
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
    /** copy of options for MML */
    this.opts = opts;
    /** formatter converts MML to HTML */
    this.formatter = new Formatter(dialect);
    /** help info for dialect */
    this.info = new Info( "#help", dialect );
    /** annotator for notes */
    this.annotator = new Annotator(this,"annotate");
    /** buffer to hold added or deleted chars */
    this.buffer = new Buffer(this,"#"+this.opts.source);
    /** reference to ourselves when this is redefined */
    var self = this;
    /** records which keycodes are characters to be typed */
    this.keycodes = 
        [0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        1,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        1,1,1,1,1,1,1,1,
        1,1,0,0,0,0,0,0,//63
        0,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,
        1,1,1,1,1,1,1,1,
        1,1,1,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,//127
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,1,1,1,1,1,1,//191
        1,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,1,1,1,1,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0,
        0,0,0,0,0,0,0,0//255
        ];
            
    /**
     * Check if we need to update the HTML. Gets called repeatedly.
     * @param time the Timer object: update on exit
     */
    this.updateHTML = function( time )
    {
        if ( this.changed && this.formatter.ready )
        {
            this.changed = false;
            this.formatter.ready = false;
            this.text_lines = new Array();
            this.html_lines = new Array();
            var text = $("#"+this.opts.source).val();
            $("#"+this.opts.target).html(this.formatter.toHTML(text,this.text_lines));
            $(".page").css("display","inline");
            var base = 0;
            $(".page").each( function(i) {
                var pos = $(this).position().top;
                if ( base==0 && pos < 0 )
                    base = Math.abs(pos);
                self.html_lines.push(new RefLoc($(this).text(),pos+base));
                // inefficient but the only way
                $(this).css("display","none");
            });
            this.recomputeImageHeights();
            var clonedBuffer = this.buffer.clone();
            this.buffer.clear();
            this.annotator.update(clonedBuffer,this.formatter);
            this.annotator.redraw();
            this.formatter.ready = true;
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
        if ( array == undefined )
            console.log("undefined");
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
            return /*this.text_lines.ref+*/",0.0";
    };
    /**
     * Recompute the height of the images after the window is fully loaded
     */
    this.recomputeImageHeights = function()
    {
        var currHt = 0;
        this.image_lines = [];
        $(".image").each( function(index) {
            var img = $(this).children().first();
            var ref = img.attr("data-ref");
            var imgHeight = img.height();
            self.image_lines.push( new RefLoc(ref,currHt) );    
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
        // oh this is an ugly hack but Opera 
        // ignores any kind of height on textarea
        // except rows - what else can you do?
        if ( navigator.appName=="Opera" )
        {
            var src = $("#source");
            var parentHt = src.offsetParent().height();
            var rows = Math.round(parentHt/16);// best guess
            src.attr("rows",rows);
        }
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
     * Explicitly set the saved property to some value
     * @param value
     */
    this.setSaved = function( value ) {
        this.saved = value;
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
    this.getTarget = function() {
        return this.opts.target;
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
    // this should really reset the interval based on how long it took
    // force update when user modifies the source
    window.setInterval(
        function() { self.updateHTML(); }, 200
    );
    /**
     * On keydown we test to see if shift or crtl etc was pressed
     */
    $("#"+opts.source).keydown(function(event) {
        if ( event.ctrlKey )
        {
            if ( event.which == 90 )
            {
                // undo/redo
                if ( event.shiftKey )
                    self.buffer.redo();
                else
                    self.buffer.undo();
            }
            else if ( event.which == 88 )
            {
                // cut
                self.buffer.deleteSelection();
            }
            else if ( event.which == 86 )
            {
                // paste
                self.buffer.paste();
            }
            else if ( event.which == 67 )
            {
                // copy
                self.buffer.copy();
            }
        }
        // ordinary keys
        else if ( self.keycodes[event.which]==1 )
        {
            self.buffer.addChars(1);
        }
        // cursor keys
        else if ( event.which >= 37 && event.which <= 40 )
        {
            if ( event.metaKey )
                self.buffer.setSelectionPending();
            else if ( event.which == 37 )    // left
                self.buffer.decStart();
            else if ( event.which == 39 )   // right
                self.buffer.incStart();
            else    // up or down
            {
                var $ta = $("#"+opts.source);
                var sel = $ta.getSelection();
                self.buffer.setStart(sel.start);
            }
        }
        else if ( event.which == 8 ) //DEL
        {
            if ( self.buffer.hasSelection() )
                self.buffer.deleteSelection();
            else
                self.buffer.delLeftChars(1);
            /*console.log("left:"+self.buffer.numDelLeftChars
                +" right:"+self.buffer.numDelRightChars);*/
        }
        else if (event.which == 46)// forward del
        {
            if ( self.buffer.hasSelection() )
                self.buffer.deleteSelection();
            else
                self.buffer.delRightChars(1);
            /*console.log("left:"+self.buffer.numDelLeftChars
                +" right:"+self.buffer.numDelRightChars);*/
        }
        /*else
            console.log("ignored key "+event.which);*/
        return true;
    });
    $("#"+opts.source).mouseup(function(event) {
        $ta = $("#"+opts.source);
        var sel = $ta.getSelection();
        if ( sel.end-sel.start > 0 )
            self.buffer.setSelection(sel);
        else
        {
            self.buffer.clearSelection();
            self.buffer.setStart(sel.start);
        }
    });
    $("#"+opts.source).bind('paste', function(e) {
        var pastedData = e.originalEvent.clipboardData.getData('text');
        self.buffer.addChars(pastedData.length);
    });
    // scroll the textarea
    $("#"+opts.source).scroll(function(e) {
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
    });
    // scroll the preview
    $("#"+opts.target).scroll(function(e) {
        if ( e.originalEvent )
        {
            var lineHeight = $("#"+self.opts.source).prop("scrollHeight")
                /self.formatter.num_lines;
            var loc = self.getPixelPage($(this),self.html_lines);
            self.scrollTo(loc,self.text_lines,
                $("#"+self.opts.source),lineHeight);
            // for some reason this causes feedback, but it works without!!
            if ( self.infoDisplayed )
                self.scrollTo(loc,self.image_lines,
                $("#"+self.opts.images),1.0);
        }
    });
    // scroll the images
    $("#"+opts.images).scroll(function(e) {
        if ( e.originalEvent )
        {
            var lineHeight = $("#"+self.opts.source).prop("scrollHeight")
                /self.formatter.num_lines;
            var loc = self.getPixelPage($(this),self.image_lines);
            self.scrollTo(loc,self.text_lines,
                $("#"+self.opts.source),lineHeight);
            self.scrollTo(loc,self.html_lines,$("#"+self.opts.target),1.0);
        }
    });
    $(window).load(function() {
        self.recomputeImageHeights()
    }); 
    $("#info").click( function() {
        self.toggleHelp();
    });
    $("#save").click( function() {
        self.save();
    });
    $("#dropdown").change( function() {
        var parts = $("#dropdown").val().split("&");
        for ( var i=0;i<parts.length;i++ ) {
            var value = parts[i].split("=");
            if ( value.length== 2 )
                $("#"+value[0]).val(value[1]);
        }
        $("form").submit();
    });
    this.styles = new Styles(this,"styles");
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

