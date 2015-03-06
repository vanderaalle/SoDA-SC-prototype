"\n".postln;
"NEW SCLANG SESSION\n///////////----------------------------------------- // /-----------------------------------------/////////////----------------------------------------- // /-----------------------------------------////////////----------------------------------------- // /-----------------------------------------////////////--------------".postln;
Date.getDate.postln;
"\n".postln;

// read arguments
~args = thisProcess.argv;

("SoDA: Arguments givens are: " ++ ~args).postln;


~search = ~args[0].asString;
~duration = ~args[1].asInteger;
~decoder = ~args[2].asSymbol;
~density = ~args[3].asInteger;
~spread = ~args[4].asInteger;
~soundLibraryPath = ~args[5].asString;
~destination = ~args[6].asString;

~ssc = SoundScapeComposer.new(~search, ~duration, ~decoder, ~density,~spread, ~soundLibraryPath, ~destination);
// 0.exit;