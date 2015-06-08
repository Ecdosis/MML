Custom scripts
==============

All these scripts require jQuery. Except for editor.js they are all downloaded on demand by the Drupal client via the mml service, which looks for them in the static/js folder of the web application. The Drupal module only contains editor.js.

editor.js
---------
This is the master file, called from the mml_edit Drupal module. The only parameter passed to it is the docid. Using this value, editor.js retrieves from the MML service the list of versions, the first version, the dialect (to format it). It then calls the mml.js script, to create an MML object that manages the actual editing.

mml.js
------
This downloads all the images associated with the current docid and version. It draws the three panels and does the sync-scrolling. It also tracks all key-presses so that annotations can be updated in annotate.js. It uses rangy to track selections in the HTML preview, and creates annotations based on them.

info.js
-------
This creates a human-readable description of the current dialect file, referenced from mml.js.

refloc.js
---------
This is a simple object for remembering the start-point of each page in the textarea and the HTML preview that correspond to page-starts in the left-hand column. This enables sync scrolling.

styles.js
---------
The styles menu is created and managed by this script. It uses rangy-inputs to surround selected text in the textarea with appropriate MML codes.

buffer.js
---------
Buffer.js holds the current edit state so that any changes to the text are buffered 1/4 of a second at a time before being passed on to the HTML preview for update.

annotate.js
-----------
Handles creation and management of annotations. For this it uses the buffer object to get a readout of how many characters have to be deleted left or right of the cursor or added etc. It uses jquery-ui to draw the annotation dialog boxes. It updates and redraws the annotations after the HTML has been drawn by formatter.js. It calls tiny-editor.js if the user enters some rich annotation text in a dialog.

formatter.js
------------
This turns the MML text into HTML by following the rules in the dialect file. It also rebuilds the table of correspondences between the HTML, plain text and MML text, so that any change to the MML can be used to update the correct character positions in the HTML. It can format a 35 page novel chapter in around 1/50th of a second, but it only does that about once every 1/4 of a second, when the buffer has accumulated some characters.

External scripts
================

rangy-core.js
-------------
This is used by mml.js to make annotations.

rangyinputs-jquery.js
---------------------
This is used by styles.js to update the textarea when the user selects a format.

tiny.editor.js
--------------
This is used to handle rich-text editing in annotation boxes.

jquery-ui.js
------------
Required by annotate.js. Used to create dialog boxes.

jquery-1.11.1.js
----------------
Required by the mml test function in mml.war. Not needed by the Drupal module, since Drupal preloads a version of jquery anyhow.
