$( document ).ready(function() {
  var dialect = {
        "description": "Novel markup for De Roberto",
        "language": "it",
        "section": {"prop": "section"},
        "paragraph": {"prop": ""},
        "codeblocks": {"prop": ""},
        "quotations": {"prop": ""},
        "smartquotes": true,
        "softhyphens": true,
        "headings": [{"tag":"=","prop":"h1"},{"tag":"-","prop":"h2"},{"tag":"_","prop":"h3"}],
        "dividers": [{"tag":"-----","prop":"dash"}, {"tag":"--+--","prop":"plus"}],
        "charformats": [{"tag":"*","prop":"italics"},{"tag":"`","prop":"letter-spacing"},{"tag":"@","prop":"small-caps"}],
        "paraformats": [{"leftTag":"->","rightTag":"<-","prop":"centered"}],
        "milestones": [{"leftTag":"[","rightTag":"]","prop":"page"}]
    };
    var opts = {
        "source": "source",
        "target": "target",
        "images": "images",
        "formid": "tostil",
        "data": {
            "prefix": "p",
            "suffix": ".png",
            "url": "images",
            "desc": [ 

            { "ref": "1",
            "width": 1324,
            "height": 2212 },
            { "ref": "2",
            "width": 1318,
            "height": 2328 },
            { "ref": "3",
            "width": 1388,
            "height": 2301 },
            { "ref": "4",
            "width": 1387,
            "height": 2301 },
            { "ref": "5",
            "width": 1416,
            "height": 2301 },
            { "ref": "6",
            "width": 1416,
            "height": 2301 },
            { "ref": "7",
            "width": 1359,
            "height": 2313 },
            { "ref": "8",
            "width": 1359,
            "height": 2313 },
            { "ref": "9",
            "width": 1419,
            "height": 2313 },
            { "ref": "10",
            "width": 1419,
            "height": 2313 },
            { "ref": "11",
            "width": 1431,
            "height": 2313 },
            { "ref": "12",
            "width": 1417,
            "height": 2313 },
            { "ref": "13",
            "width": 1418,
            "height": 2313 } 
            ]
        }
   };
   var editor = new MMLEditor(opts, dialect);
   $("#info").click( function() {
        editor.toggleHelp();
   });
   $("#save").click( function() {
        editor.save();
   });
}); 
