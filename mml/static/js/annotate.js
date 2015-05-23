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
            var att = document.createAttribute("class"); 
            att.value = "annotation";  
            midNode.setAttributeNode(att);
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
    $("#"+this.button).click( function() {
        //1. find selection
        var sel = rangy.getSelection();
        var range0 = sel.getRangeAt(0);
        var start = range0.startContainer;
        var end = range0.endContainer;
        //2. highlight text at that location
        if ( start == end )
        {
            self.surroundTextNode(start,
                range0.startOffset,
                range0.endOffset-range0.startOffset);
        }
        else 
        {
            var next = self.nextTextNode(start);
            // destroys start
            self.surroundTextNode(start,range0.startOffset, 
                start.nodeValue.length);
            while ( next != end && next != null )
            {
                var nextLen = next.nodeValue.length;
                var newNext = self.nextTextNode(next);
                if ( next != end )  // destroys next
                {
                    self.surroundTextNode(next,0,nextLen);
                }
                next = newNext;    
            }
            if ( next != null )
            {
                self.surroundTextNode(next,0,range0.endOffset);
            }
        }
        //clean up selection
        sel.removeAllRanges();
        //3. create annotation object based on selection, current user
        //4. generate popup
    });
}
