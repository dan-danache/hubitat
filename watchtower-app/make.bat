ECHO off
CLS

ECHO ===================================================================================================
ECHO This script uses esbuild to bundle all JavaScript files from ./src/* into the ./watchtower.js files
ECHO ===================================================================================================

del watchtower.html
del watchtower.js

tools\esbuild-0.23.0.exe src\watchtower.js --bundle --minify --tree-shaking=true --legal-comments=none "--reserve-props=^\$config.*$" --outdir=. "--external:chart.js"
tools\minhtml-0.15.0.exe --minify-css --do-not-minify-doctype --ensure-spec-compliant-unquoted-attribute-values --keep-closing-tags .\src\watchtower.html -o .\watchtower.html
