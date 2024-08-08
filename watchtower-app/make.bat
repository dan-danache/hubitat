ECHO off
CLS

ECHO ===================================================================================================
ECHO This script uses esbuild to bundle all JavaScript files from ./src/* into the ./watchtower.js files
ECHO ===================================================================================================

del watchtower.html
del watchtower.js

copy src\watchtower.html .
tools\esbuild.exe src\watchtower.js --bundle --minify --tree-shaking=true --legal-comments=none --outdir=. "--external:chart.js" "--external:hammerjs" "--external:chart.js/helpers"
