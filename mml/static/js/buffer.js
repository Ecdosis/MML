/**
 * Hold the number of additional and/or deleted characters
 * and the position they start from
 * @param sourceId the #+id of the source textarea
 */
function Buffer(sourceId)
{
    this.start = 0;
    this.numDelLeftChars = 0;
    this.numDelRightChars = 0;
    this.numAddChars = 0;
    this.sourceId = sourceId;
    /** 
     * Clone this object 
     * @return an exact copy of this
     */
    this.clone = function() {
        var clone = new Buffer(sourceId);
        clone.start = this.start;
        clone.numDelLeftChars = this.numDelLeftChars;
        clone.numDelRightChars = this.numDelRightChars;
        clone.numAddChars = this.numAddChars;
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
    };
    /** 
     * Delete chars to the left of start
     * @param num the number of chars to remove
     */
    this.delLeftChars = function(num) {
        this.numDelLeftChars += num;
    };
    /** 
     * Delete chars to the right of start
     * @param num the number of chars to remove
     */
    this.delRightChars = function(num) {
        this.numDelRightChars += num;
    };
    /**
     * Clear this buffer after the preview and annotations have been updated
     */
    this.clear = function() {
        this.numDelLeftChars = this.numDelRightChars = this.numAddChars = 0;
        this.shiftDown = false;
        this.start -= (this.numDelLeftChars+this.numDelRightChars);
        this.start += this.numAddChars;
        this.selection = undefined;
        this.selectionPending = false;
    };
    /**
     * Set the start point after a mouse click, cursor movement or paste
     * @param start the new start point 
     * @return true if it was OK else false (not set)
     */
    this.setStart = function( start ) {
        if ( this.numDelLeftChars == 0 && this.numDelRightChars ==0 && this.numAddChars == 0 )
        {
            this.start = start;
            return true;
        }
        else
            return false;   // empty buffer first
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
    };
    /**
     * Set the selectionPending flag. When a selection is requested it will be computed
     */
    this.setSelectionPending = function() {
        this.shiftDown = false;
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
        this.start = this.selection.start;
        this.numDelRightChars = this.selection.end-this.selection.start;
        this.selection = undefined;
    };
    /**
     * The user pressed shift
     */
    this.setShiftDown = function(down) {
        this.shiftDown = down;
    };
    /**
     * Is the shift key being held down?
     * @return true if it was held down
     */
    this.shiftIsDown = function() {
        return this.shiftDown;
    };
}

