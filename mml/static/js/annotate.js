function Position()
{
    this.position = 0;
    this.setPos = function(pos)
    {
        this.position = pos;
    }
    this.getPos = function()
    {
        return this.position;
    }
}
/**
 * Create an annotation object
 * @param id the identifier (integer)
 * @param anchorPos the absolute offset for the annotation
 * @param anchorLen the length of the annotation's anchor
 * @param user the name of the current server-side user
 */
function Annotation( id, anchorPos, anchorLen, user, content )
{
    this.offset = anchorPos;
    this.len = anchorLen;
    this.content = "";
    this.user = user;
    this.id = id;
    this.toString = function() {
        return "offset:"+this.offset+" len="+this.len
        +" user:"+this.user+" content: "+this.content;
    };
    this.setContent = function( content ) {
        this.content = content;
    };
    this.setUser = function( user ) {
        this.user = user;
    };
}
var annotations = new Array();
/**
 * Handle annotation actions
 * @param editor the MMLEditor object
 * @param button the annotate button ID
 */
function Annotator( editor, button )
{
    this.editor = editor;
    this.button = button;
    this.nextId = 1;
    var self = this;
    this.getFirstRange = function() {
        var sel = rangy.getSelection();
        return sel.rangeCount ? sel.getRangeAt(0) : null;
    };
    /**
     * Surround part of a single text element with <span> tags
     * @param elem the text element
     * @param id the id of the comment
     * @param start the start offset into the text
     * @param len the length of the portion to surround
     * @return the span node
     */
    this.surroundTextNode = function(elem,id,start,len){
        var text = elem.nodeValue;
        var pre = (start==0)?"":text.slice(0,start);
        var mid = text.slice(start,start+len);
        var post = (start+len<text.length)?text.slice(start+len,text.length):"";
        var preNode = null;
        var postNode = null;
        var midNode = null;
        if ( pre.length > 0 )
            preNode = document.createTextNode(pre);
        if ( post.length > 0 )
            postNode = document.createTextNode(post);
        if ( mid.length > 0 )
        {
            midNode = document.createElement("span");
            midNode.setAttribute("class","anchor");
            midNode.setAttribute("data-id",id);
            midNode.textContent = mid;
        }
        var parent = elem.parentNode;
        if ( midNode != null )
            parent.replaceChild(midNode,elem);
        if ( preNode != null )
            parent.insertBefore(preNode,midNode);
        if ( postNode != null )
        {
            if ( midNode.nextSibling != null )
                parent.insertBefore(postNode,midNode.nextSibling);
            else
                parent.appendChild(postNode);
        }
        // verify
        if ( midNode.parentNode != parent )
            console.log("midNode.parentNode != parent");
        if ( postNode != null && postNode.parentNode != parent )
            console.log("postNode.parentNode != parent");
        if ( preNode != null && preNode.parentNode != parent )
            console.log("preNode.parentNode != parent");
        $(midNode).click( function() {
            var id = this.getAttribute("data-id");
            var commentId = "#comment-"+id;
            if ( $(commentId).length == 0 )
                self.initDialog(commentId);
            $("#comment-"+id).dialog("open");
        });
        return midNode;
    };
    /**
     * Get the first text-node of any DOM element
     * @param elem the element to search for text
     * @return a text node or null
     */
    this.firstTextNode = function( elem ) {
        if ( elem.nodeType==3 && elem.nodeValue.length!=0 )
            return elem;
        else if ( elem.firstChild != null 
            && this.firstTextNode(elem.firstChild) != null )
            return this.firstTextNode(elem.firstChild);
        else if ( elem.nextSibling != null )
            return this.firstTextNode(elem.nextSibling);
        else
            return null;
    }
    /**
     * Find the next text element
     * @param elem the current text node
     * @return another text element or null
     */
    this.nextTextNode = function(elem) {
        if ( elem.nodeType != 3 )
            console.log("nodeType="+elem.nodeType);
        if ( elem.nextSibling != null 
            && this.firstTextNode(elem.nextSibling) != null )
            return this.firstTextNode(elem.nextSibling);
        else
        {
            var parent = elem.parentNode;
            while ( parent != null )
            {
                var sibling = parent.nextSibling;
                while ( sibling != null )
                {
                    if ( this.firstTextNode(sibling) != null )
                        return this.firstTextNode(sibling);
                    else
                        sibling = sibling.nextSibling;
                }
                // no more text nodes on this level
                parent = parent.parentNode;
            }
        }
        console.log("no next text node for "+elem.nodeValue);
        return null;
    }
    /**
     * Get the length of a range (not provided by rangy!)
     * @param range the rangy range object
     * @return the length of the selection
     */
    this.rangeLength = function( range ) {
        var start = range.startContainer;
        var end = range.endContainer;
        var len = 0;
        var offset = range.startOffset;
        while ( start != end && start != null )
        {
            len += start.length-offset;
            offset = 0;
            start = this.nextTextNode(start);
        }
        len += range.endOffset-offset;
        return (start==null)?0:len;
    };
    /**
     * Find the absolute offset in the base text
     * @param range the rangy range object
     * @return the absolute text-only (no markup) offset
     */
    this.rangeOffset = function( range ) {
        var target = $("#"+this.editor.getTarget());
        var node = this.firstTextNode(target[0]);
        var end = range.startContainer;
        var endPos = range.startOffset;
        var len = 0;
        while ( node != end && node != null )
        {
            if ( node.nodeType==3 )
                len += node.nodeValue.length;
            node = this.nextTextNode(node);
        }
        if ( node != null )
            len += endPos;
        return len;
    };
    /**
     * Add an annotation to the global list
     * @param ann the Annotation object
     */
    this.installAnnotation = function( ann ) {
        var i=0;
        var offset=0;
        for ( ;i<annotations.length;i++ )
        {
            var nextOffset = offset+annotations[i].offset;
            if ( nextOffset == ann.offset )
            {
                if ( annotations[i].len < ann.len )
                {
                    annotations[i].offset = nextOffset-ann.offset;
                    ann.offset -= offset;
                    annotations.splice(i, 0, ann);
                    break;
                }
            }
            else if ( nextOffset > ann.offset )
            {
                annotations[i].offset = nextOffset-ann.offset;
                ann.offset -= offset;
                annotations.splice(i, 0, ann);
                break;
            }
            offset += annotations[i].offset;
        }
        if ( i == annotations.length )
        {
            ann.offset = ann.offset-offset;
            annotations.push(ann);
        }
        /*for ( var i=0;i<annotations.length;i++ )
            console.log(annotations[i].toString());*/
        this.editor.setSaved(false);
    };
    /**
     * Get a simple unique id for the next annotation
     * @return an int starting at 1
     */
    this.id = function() {
        var id = this.nextId;
        this.nextId++;
        return id;
    };
    /**
     * Get the last used user name
     * @return the annotator's name last entered OR the server user's name
     */
    this.getUserName = function() {
        if ( this.userName == undefined )
            this.userName = "Anonymous";
        return this.userName;
    };
    /**
     * Build the body of the comment
     * @param ann the annotation object
     * @return the HTML of the comment body
     */
    this.composeContent = function(ann) {
        this.user = ann.user;
        var text = '<div id="comment-'+ann.id+'" class="annotation">';
        text += '<p>Name: <input class="user-name" type="text" value="'
            +ann.user+'"></input></p>';
        text += '<textarea class="comment" '
            +'placeholder="Enter comment here..."></textarea>';
        text += '</div>';
        return text;
    }
    /**
     * Find an annotation by brute-force.
     * @param id the numeric id to look for
     * @return an Annotation object
     */
    this.findAnnotation = function(id) {
        for ( var i=0;i<annotations.length;i++ )
            if ( annotations[i].id == id )
                return annotations[i];
        return null;
    };
    /**
     * Initialise a jquery ui dialog widget
     * @param commentId the id of the comment div
     */
    this.initDialog = function(commentId) {
        var parts = commentId.split("-");
        var id = parts[1];
        var ann = this.findAnnotation(id);
        if ( ann != null )
        {
            var annDiv = self.composeContent(ann);
            if ( $("#annotations").length == 0 )
                $("body").append('<div id="annotations"></div>');
            $("#annotations").append(annDiv);
            $(commentId).dialog({ autoOpen: false, title: "Annotation", 
                position: { my: "bottom center", of:"[data-id='"+id+"']"}, 
                close: function closeComment( event, ui ) {
                    ann.setContent($(this).find(".comment").first().val());
                    ann.setUser($(this).find(".user-name").first().val());
                    self.userName = ann.user; 
                }  
            });
        }
    };
    /**
     * Delete a range and update the annotations
     * @param delLeft the position of the first char to be deleted
     * @param delRight the position after the last char to be deleted
     */
    this.updateDeletion = function(delLeft,delRight) {
        var i = 0;
        var offset = 0;
        var lastOffset = 0;
        while ( i<annotations.length )
        {
            lastOffset = offset;
            offset += annotations[i].offset;
            var end = offset + annotations[i].len;
            // 1. deletion before annotation
            if ( delLeft >= lastOffset && delRight < offset )
            {
                annotations[i].offset -= (delRight-delLeft);
                break;
            }
            // 2. annotation-range inside deletion
            else if ( delLeft <= offset && delRight >= end )
            {
                annotations.splice(i,1);
                offset = lastOffset;  // reset offset
                i--;// balance out i++ at end
            }
            // 3. deletion inside annotation-range
            else if ( delLeft >=offset && delRight <= end )
            {
                annotations[i].len -= (delRight-delLeft);
            }
            // 4. deletion overlaps on left of annotation
            else if ( delLeft < offset && delRight > offset )
            {
                annotation[i].len -= (delRight-offset);
                annotation[i].offset -= (offset-delLeft);
                break;
            }
            // 5. deletion overlaps on right of annotation
            else if ( delRight >= end && delLeft < end )
            {
                annotation[i].len -= end-delLeft;
            }
            i++;
        }
    };
    /**
     * Update the annotations when some new text is added
     * @param pos the position in the base text of the addition
     * @param numChars the numer of new characters at pos
     */
    this.updateAddition = function(pos,numChars) {
        var i = 0;
        var offset = 0;
        var lastOffset = 0;
        var lastEnd = 0;
        var end = 0;
        while ( i<annotations.length )
        {
            lastOffset = offset;
            lastEnd = end;
            offset += annotations[i].offset;
            end = offset + annotations[i].len;
            // 1. addition before annotation
            if ( pos >= lastEnd && pos < offset )
            {
                annotations[i].offset += numChars;
                break;
            }
            // 2. addition inside annotation-range
            else if ( pos >=offset && pos <= end )
            {
                annotations[i].len += numChars;
                if ( i < annotations.length-1 )
                    annotations[i+1].offset+=numChars;
                break;
            }
            i++;
        }
    };
    /**
     * Update the relevant annotations' offsets
     * @param buffer the Buffer object 
     * @param formatter the object with the getOffset convert routine
     */
    this.update = function( buffer, formatter ) {
        if ( !buffer.empty() && annotations != undefined )
        {
            var delLeft = formatter.getOffset(buffer.minDelPos(),"mml","text");
            var delRight = formatter.getOffset(buffer.maxDelPos(),"mml","text");
            delRight -= buffer.numAddChars;
            if ( delLeft < delRight )
                this.updateDeletion(delLeft,delRight);
            else if ( buffer.numAddChars > 0 )
            {
                var pos = formatter.getOffset(buffer.start,"mml","text");
                var numChars = buffer.numAddChars;
                numChars -= (buffer.maxDelPos()-buffer.minDelPos());
                if ( numChars > 0 )
                    this.updateAddition(pos,numChars);
            }
            /*for ( var i=0;i<annotations.length;i++ )
                console.log(annotations[i].toString());*/
        }
    };
    /**
     * Get the text-node at a given offset
     * @param offset the sought-after position in the text
     * @param tnode the text node to start from
     * @param pos on input global offset, on return local offset in text node
     * @return a text-node
     */
    this.textNodeAt = function( offset, tnode, pos ) {
        var loc = 0;
        var tLen = tnode.nodeValue.length-pos.getPos();
        while ( tnode != null && tLen+loc <= offset )
        {
            loc += tLen;
            tnode = this.nextTextNode(tnode);
            if ( tnode != null )
                tLen = tnode.nodeValue.length;
        }
        pos.setPos(offset-loc);
        return tnode;
    };
    /**
     * Redraw the annotations after a text update
     */
    this.redraw = function() {
        var elem = $("#"+this.editor.getTarget());
        var tnode = this.firstTextNode(elem[0]);
        if ( tnode != null )
        {
            var pos = new Position();
            for ( var i=0;i<annotations.length;i++ )
            {
                var ann = annotations[i];
                var annLen = ann.len;
                var offset = ann.offset;// relative!
                var first = null;
                while ( annLen > 0 )
                {
                    var old_tnode = tnode;
                    tnode = this.textNodeAt( offset, tnode, pos );
                    var len = Math.min(tnode.nodeValue.length-pos.getPos(),annLen);
                    var span = this.surroundTextNode(tnode,ann.id,pos.getPos(),len);
                    // the old tnode is now detached
                    tnode = this.firstTextNode(span);
                    if ( first == null )
                        first = span;
                    annLen -= len;
                    offset = len;
                    pos.setPos(0);
                }
                tnode = this.firstTextNode(first);
            }
        }
    };
    /**
     * Execute this when you click on the comment button
     */
    $("#"+this.button).click( function() {
        // the id of the new comment
        var id = self.id();
        //1. find selection
        var sel = rangy.getSelection();
        var range0 = sel.getRangeAt(0);
        var start = range0.startContainer;
        var end = range0.endContainer;
        //2. highlight text at that location
        var first=null;
        var last =null;
        if ( start == end )
        {
            first = last = self.surroundTextNode(start,id,
                range0.startOffset,
                range0.endOffset-range0.startOffset);
        }
        else 
        {
            var next = self.nextTextNode(start);
            // destroys start
            var mid = self.surroundTextNode(start,id,range0.startOffset, 
                start.nodeValue.length);
            if ( first == null )
                first = mid;
            while ( next != end && next != null )
            {
                var nextLen = next.nodeValue.length;
                var newNext = self.nextTextNode(next);
                if ( next != end )  // destroys next
                {
                    mid = self.surroundTextNode(next,id,0,nextLen);
                    if ( first == null )
                        first = mid;
                    else
                        last = mid;
                }
                next = newNext;    
            }
            if ( next != null )
            {
                last = self.surroundTextNode(next,id,0,range0.endOffset);
            }
        }
        //clean up selection
        sel.removeAllRanges();
        var textStart = self.firstTextNode(first);
        var textEnd = self.firstTextNode(last);
        var newRange = rangy.createRange();
        newRange.setStartAndEnd(textStart, 0, textEnd, textEnd.nodeValue.length);
        //3. create annotation object based on selection, current user
        var rangeOffset = self.rangeOffset(newRange);
        var rangeLen = self.rangeLength(newRange);
        var ann = new Annotation( id, rangeOffset, rangeLen, self.getUserName());
        console.log(rangeOffset+","+rangeLen);
        self.installAnnotation( ann );
        //4. generate popup containing annotation
        var commentId = "#comment-"+id;
        self.initDialog(commentId);
        // centre it near/over annotated text
        $(commentId).dialog("open");
    });
}
