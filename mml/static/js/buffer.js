/**
 * Hold the number of additional and/or deleted characters
 * and the position they start from
 */
function Buffer()
{
    this.start = 0;
    this.numDelLeftChars = 0;
    this.numDelRightChars = 0;
    this.numAddChars = 0;
    /** 
     * Add chars to the right of start
     * @param num the number of chars to add
     */
    this.addChars = function(num) {
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
    this.minDelPos = function() {
        return this.start-this.numDelLeftChars;
    };
    this.maxDelPos = function() {
        return this.start+this.numDelRightChars;
    };
}

