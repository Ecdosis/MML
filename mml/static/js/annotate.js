/**
 * Create an annotation object
 * @param target the ID of the generated HTML
 * @param anchorPos the absolute offset for the annotation
 * @param anchorLen the length of the annotation's anchor
 * @param user the name of the current server-side user
 */
function Annotation( target, anchorPos, anchorLen, user )
{
    this.target = target;
    this.offset = anchorPos;
    this.len = anchorLen;
    this.toString = function() {
        return "offset:"+this.offset+" len="+this.len;
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
    var self = this;
    this.getFirstRange = function() {
        var sel = rangy.getSelection();
        return sel.rangeCount ? sel.getRangeAt(0) : null;
    };
    /**
     * Surround part of a single text element with <span> tags
     * @param elem the text element
     * @param start the start offset into the text
     * @param len the length of the portion to surround
     * @return the span node
     */
    this.surroundTextNode = function(elem,start,len){
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
            midNode.setAttribute("class","annotation");
            midNode.textContent = mid;
        }
        if ( preNode != null )
            elem.parentNode.insertBefore(preNode,elem);
        if ( midNode != null )
            elem.parentNode.insertBefore(midNode,elem);
        if ( postNode != null )
            elem.parentNode.insertBefore(postNode,elem);
        elem.parentNode.removeChild(elem);
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
     * @param elem the current text element
     * @return another text element or null
     */
    this.nextTextNode = function(elem) {
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
        for ( var i=0;i<annotations.length;i++ )
            console.log(annotations[i].toString());
    };
    /**
     * Execute this when you click on the comment button
     */
    $("#"+this.button).click( function() {
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
            first = last = self.surroundTextNode(start,
                range0.startOffset,
                range0.endOffset-range0.startOffset);
        }
        else 
        {
            var next = self.nextTextNode(start);
            // destroys start
            var mid = self.surroundTextNode(start,range0.startOffset, 
                start.nodeValue.length);
            if ( first == null )
                first = mid;
            while ( next != end && next != null )
            {
                var nextLen = next.nodeValue.length;
                var newNext = self.nextTextNode(next);
                if ( next != end )  // destroys next
                {
                    mid = self.surroundTextNode(next,0,nextLen);
                    if ( first == null )
                        first = mid;
                    else
                        last = mid;
                }
                next = newNext;    
            }
            if ( next != null )
            {
                last = self.surroundTextNode(next,0,range0.endOffset);
            }
        }
        //clean up selection
        sel.removeAllRanges();
        var textStart = self.firstTextNode(first);
        var textEnd = self.firstTextNode(last);
        var newRange = rangy.createRange();
        newRange.setStartAndEnd(textStart, 0, textEnd, textEnd.nodeValue.length);
        //3. create annotation object based on selection, current user
        var ann = new Annotation(editor.getTarget(),
            self.rangeOffset(newRange),self.rangeLength(newRange),
            editor.getUserName());
        self.installAnnotation( ann );
        //4. generate popup containing annotation
        // centre it near/over annotated text
    });
}
