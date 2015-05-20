function Styles(editor,target)
{
    /** the MML Editor object */
    this.editor = editor;
    /** the styles menu id */
    this.target = target;
    /** reference to ourselves */
    var self = this;
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
                lines[i] = lines[i].trim();
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
    /**
     * Adjust the start of the selection back to line-start
     * @param ta the textarea
     */
    this.adjustSelection = function( ta ) {
        var sel = ta.getSelection();
        var text = ta.val();
        var n = 0;
        for ( var i=sel.start-1;i>0;i-- )
        {
            if ( text[i] != ' ' && text[i] != '\t' )
                break;
            else if ( text[i] == '\n' )
                break;
            n++;
        }
        ta.setSelection(sel.start-n,sel.end);
    };
    // handle formatting menu actions
    $("#"+this.target).mouseup(function(){
        var json = $("#"+self.target).val().replace(/<q>/g,'"');
        var jobj = JSON.parse(json);
        var leftTag = "";
        var rightTag = "";
        var $ta = $("#"+editor.opts.source);
        if ( jobj.tag != undefined )
            leftTag = rightTag = jobj.tag;
        else if ( jobj.leftTag != undefined && jobj.rightTag != undefined )
        {
            leftTag = jobj.leftTag;
            rightTag = jobj.rightTag;
        }
        if ( jobj.type == 'charformats' )
        {
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                $ta.surroundSelectedText(leftTag, rightTag);
                self.editor.changed = true;
            }
        }
        else if ( jobj.type == 'paraformats' )
        {
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                self.wrapBlock($ta,leftTag,rightTag,2,2);
                self.editor.changed = true;
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
                self.wrapBlock($ta,"", "\n"+underlining,2,2);
                self.editor.changed = true;
            }
        }
        else if ( jobj.type == 'dividers' )
        {
            var sel = $ta.getSelection();
            if ( sel.end>sel.start )
                $ta.deleteSelectedText();
            self.wrapBlock($ta, "", jobj.tag,2,2);
            self.editor.changed = true;
        }  
        else if ( jobj.type == 'milestones' )
        {
            self.wrapBlock($ta, leftTag, rightTag,1,1);
            self.editor.changed = true;
        }  
        else if ( jobj.type == 'codeblocks' )
        {
            self.adjustSelection($ta);
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                var level = self.getLevel(jobj.prop);
                var verbatim = self.indentBlock($ta, "    ", level, 1, 1);
                $ta.replaceSelectedText(verbatim, "collapseToEnd");
                self.editor.changed = true;
            }
        }
        else if ( jobj.type == 'quotations' )
        {
            self.adjustSelection($ta);
            var sel = $ta.getSelection();
            var len = sel.end-sel.start;
            if ( len > 0 )
            {
                var level = self.getLevel(jobj.prop);
                var quoted = self.indentBlock($ta,">",level,2,2);
                $ta.replaceSelectedText(quoted, "collapseToEnd");
                self.editor.changed = true;
            }
        }
        if ( self.editor.changed )
        {
            if ( self.editor.saved )
            {
                self.editor.saved = false;
                self.editor.toggleSave();
            }
        }
    });
}
