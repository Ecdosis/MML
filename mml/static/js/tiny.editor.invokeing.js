    /**
     * The user clicked on an editable div
     * @param the jQuery target for later restoration
     */
    this.install_editor = function( target ) {
        var content = target.html();
        target.replaceWith(function(){
            return '<textarea id="tinyeditor">'+content+'</textarea>';
        });
        var editor = new TINY.editor.edit('editor', {
            id: 'tinyeditor',
            width: 584,
            height: 175,
            cssclass: 'tinyeditor',
            controlclass: 'tinyeditor-control',
            rowclass: 'tinyeditor-header',
            dividerclass: 'tinyeditor-divider',
            controls: ['bold', 'italic', 'underline', 'strikethrough', '|', 'subscript', 'superscript', '|',
                    'orderedlist', 'unorderedlist', '|', 'outdent', 'indent', '|', 'leftalign',
                    'centeralign', 'rightalign', 'blockjustify', '|', 'unformat', '|', 'undo', 'redo', 'n',
                    'font', 'size', 'style', '|', 'image', 'hr', 'link', 'unlink'],
            footer: true,
            fonts: ['Verdana','Arial','Georgia','Trebuchet MS'],
            xhtml: true,
            bodyid: 'editor',
            footerclass: 'tinyeditor-footer',
            toggle: {text: 'source', activetext: 'wysiwyg', cssclass: 'toggle'},
            resize: {cssclass: 'resize'}
        });
        jQuery("div.tinyeditor").css("overflow-y","visible");
    };
    /**
     * Remove the editor and restore the old div with the new text
     */
    this.restore_div = function() {
        var iframe = jQuery("#tinyeditor").next();
        var html = iframe[0].contentDocument.documentElement;
        var content = html.lastChild.innerHTML;
        var class_name = "edit-region";
        var parent = iframe.closest("tr");
        if ( content=='<br>' )
        {
            if ( parent.next("tr").length!= 0)
                content = self.strs.empty_description;
            else
                content = self.strs.empty_references;
        }
        var id = 'description';
        if ( parent.next("tr").length== 0)
            id = 'references';
        jQuery("div.tinyeditor").replaceWith('<div id="'+id+'" class="'
            +class_name+'">'+content+'</div>');
        if ( self.pDoc.events[this.index].status != 'added' )
            self.pDoc.events[this.index].status = 'changed';
        jQuery("#"+id).click( function(e) {
            if ( jQuery("#tinyeditor").length>0 )
                self.restore_div();
            self.install_editor(jQuery(this));
        });
        jQuery("#"+id).css("overflow-y","auto");
    };
