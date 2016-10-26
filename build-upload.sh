#!/bin/bash
function build_file
{
    first=1
    if [ ! -z "$2" ]; then
        sed -f $2 $1 > temp.out
    else
        cp $1 temp.out
    fi
    IFS=''
    while read line
    do
        if [ $first == 1 ]; then
            first=2
            str="$str$line"
        else
            str="$str"\\n"$line"
        fi
        line=""
    done < temp.out
    if [ ! -z "$line" ]; then
        str="$str"\\n"$line"
    fi
#   rm temp.out
    echo "$str"
}
function wrap
{
    if [ -z "$4" ] && [ -z "$5" ]; then
        echo "db.$1.insert({docid:\"$2\",body:\"$3\"});"
    elif [ -z "$5" ]; then
        echo "db.$1.insert({docid:\"$2\",body:\"$3\",format:\"$4\"});"
    else
        echo "db.$1.insert({docid:\"$2\",body:\"$3\",format:\"$4\",style:\"$5\"});"
    fi
}
if [ -f upload.js ]; then
    rm upload.js
    touch upload.js
fi
# build dialect file and add to upload.js
dialect=`build_file dialect-harpur.json code.sed`
echo db.dialects.remove\({docid:\"english/harpur\"}\)\;>>upload.js
upload=`wrap dialects english/harpur "$dialect"`
echo "$upload">>upload.js


