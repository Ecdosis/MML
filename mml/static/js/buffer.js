/**
 * Hold the number of additional and/or deleted characters
 * and the position they start from
 * @param parent the mml editor object
 * @param sourceId the #+id of the source textarea
 */
function Buffer(parent,sourceId)
{
    this.start = 0;
    this.numDelLeftChars = 0;
    this.numDelRightChars = 0;
    this.numAddChars = 0;
    this.sourceId = sourceId;
    this.startPending = -1;
    this.parent = parent;
    /** 
     * Clone this object 
     * @return an exact copy of this
     */
    this.clone = function() {
        var clone = new Buffer(this.parent,this.sourceId);
        clone.start = this.start;
        clone.numDelLeftChars = this.numDelLeftChars;
        clone.numDelRightChars = this.numDelRightChars;
        clone.numAddChars = this.numAddChars;
        clone.startPending = this.startPending;
        return clone;
    };
    /** 
     * Add chars to the right of start
     * @param num the number of chars to add
     */
    this.addChars = function(num) {
        if ( this.selectionPending )
        {
            this.selection = $(this.sourceId).getSelection();
            this.selectionPending = false;
        }
        if ( this.selection != undefined )
        {
            this.start = this.selection.start;
            this.numDelRightChars = this.selection.end-this.selection.start;
            this.selection = undefined;
        }
        this.numAddChars += num;
        this.parent.changed = true;
    };
    /** 
     * Delete chars to the left of start
     * @param num the number of chars to remove
     */
    this.delLeftChars = function(num) {
        this.numDelLeftChars += num;
        this.parent.changed = true;
    };
    /** 
     * Delete chars to the right of start
     * @param num the number of chars to remove
     */
    this.delRightChars = function(num) {
        this.numDelRightChars += num;
        this.parent.changed = true;
    };
    /**
     * Clear this buffer after the preview and annotations have been updated
     */
    this.clear = function() {
        this.numDelLeftChars = this.numDelRightChars = this.numAddChars = 0;
        this.shiftDown = false;
        this.start -= (this.numDelLeftChars+this.numDelRightChars);
        this.start += this.numAddChars;
        if ( this.startPending != -1 )
        {
            this.start = this.startPending;
            this.startPending = -1;
        }
        this.selection = undefined;
        this.selectionPending = false;
    };
    /**
     * Set the start point after a mouse click, cursor movement or paste
     * @param start the new start point 
     */
    this.setStart = function( start ) {
        if ( this.numDelLeftChars == 0 && this.numDelRightChars ==0 && this.numAddChars == 0 )
            this.start = start;
        else
            this.startPending = start;
    };
    /**
     * Is there any pending data?
     * @return true if there are characters in the buffer else false
     */
    this.empty = function() {
        return this.numDelLeftChars == 0 && this.numDelRightChars==0 && this.numAddChars==0;
    };
    /**
     * Get the leftmost delete position
     * @return the left-margin of the deleted range
     */
    this.minDelPos = function() {
        return this.start-this.numDelLeftChars;
    };
    /**
     * Get the rightmost delete position
     * @return the right-margin of the deleted range
     */
    this.maxDelPos = function() {
        return this.start+this.numDelRightChars;
    };
    /**
     * A selection has just been made (say) on mouse-up
     * @param sel the rangy selection
     */
    this.setSelection = function( sel ) {
        this.selection = sel;
        this.start = sel.start;
    };
    /**
     * Set the selectionPending flag. When a selection is requested it will be computed
     */
    this.setSelectionPending = function() {
        this.selectionPending = true;
    };
    /**
     * Clear the current selection (user pressed a key etc)
     */
    this.clearSelection = function() {
        this.selection = undefined;
        this.selectionPending = undefined;
    };
    /**
     * Test if this buffer is holding a selection
     * @return true if text is currently selected else false
     */
    this.hasSelection = function() {
        if ( this.selectionPending )
        {
            this.selection = $(this.sourceId).getSelection();
            this.selectionPending = false;
        }
        return this.selection != undefined;
    };
    /**
     * The user pressed a delete key or another character when text was selected
     */
    this.deleteSelection = function() {
        if ( this.selectionPending )
        {
            this.selection = $(this.sourceId).getSelection();
            this.selectionPending = false;
        }
        this.start = this.selection.start;
        this.numDelRightChars = this.selection.end-this.selection.start;
        this.selection = undefined;
        this.parent.changed = true;
    };
    this.decStart = function() {
        if ( this.start >0 )
            this.start--;
    };
    this.incStart = function() {
        this.start++;
    };
    this.redo = function() {
        console.log("redo");
        this.parent.changed = true;
    };
    this.undo = function() {
        console.log("undo");
        this.parent.changed = true;
    };
    this.paste = function() {
        console.log("paste");
        this.parent.changed = true;
    };
    this.copy = function() {
        console.log("copy");
    };
}

