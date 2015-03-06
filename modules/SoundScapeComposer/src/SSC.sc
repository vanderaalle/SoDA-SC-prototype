// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013-14

// this file is part of the SoundScape Composer, which has been developed as part of the Sound Design Accelerator (SoDA) project


SoundScapeComposer {


	// member variables

	// organicDb
	var < organicDb;          // (SSC_OrganicDb) the processed organicDb
	var search;             // (String) the search string
	var < interpreter;        // (SSC_Interpreter)

	// paths
	var organicPath;
	var parentFolderPath;
	var soundLibraryPath;
	var destination; // the final name of the bounce (no .aif in the end)

	var duration;          // (Number) total duration
	var decoder;           // (FoaDecoderMatrix) the decoder

	var ready;              // (Boolean) is ready?
	var readyCond;          // (Condition) is ready ?

	var < sonicSpace;
	var < runner;


	// ===================== new/init =======================

	*initClass {
		// init all SSC related herein
		// Class.initClassTree(SSG_Runner);
		Class.initClassTree(SSC_OrganicDb);
		Class.initClassTree(SSC_Interpreter);
		Class.initClassTree(FoaDecoderMatrix);
	}

	// ===================== new/init =======================

	*new{ arg
		search = "", // (String) Should be the search string
		duration = 100,     // (Number) the final duration
		decoder = \stereo, // stereo, 5.1, etc
		// maxEntries = 100, maxSimilarEntries = 10, maxAtomsPerSequence = 10, maxAtmos = 1
		density = 2,         // (Integer) how many simultaneous different events (approximation)
		spread = 200,       // (Number) spatial spread of soundscape
		// correlation = false; // (Boolean) true will result in semantically relevant events to be temporally grouped together (if possible)
		soundLibraryPath, // (The folder with the completeLibrary)
		destination; // the destination file name

		^super.new.pr_init(search, duration, decoder, density, spread, soundLibraryPath, destination);
	}

	pr_init{
		arg search_, duration_, decoder_, density, spread, library, destination_;

		"------------------------- // ---------------------".postln;
		"SSC: New SoundScapeComposition Initiated.".postln;
		Date.getDate.postln;
		"\n".postln;

		// perform type check
		case
		{ search_.isKindOf(String).not} {
			Error("SSC: 'search' argument should be an instance of String").throw }
		{duration_.isKindOf(Number).not} {
			Error("SSC: argument 'duration' should be an instance of Number").throw;}
		{density.isKindOf(Number).not} {
			Error("SSC: argument 'density' should be an instance of Integer").throw;}
		{spread.isKindOf(Number).not} {
			Error("SSC: argument 'spread' should be an instance of Number").throw;} 
		{library.isKindOf(String).not} {
				Error("SSC: argument 'library' should be an instance of String pointing to an actual Folder in the Hard Drive").throw;}
		{PathName(library).isFolder.not} {
			Error("SSC: argument 'library' points to an non existent folder").throw;}	
		{destination_.isKindOf(String).not} {
			Error("SSC: argument 'Destination' should be an instance of String with a name of a file").throw;};

		// type check decoder
		if (decoder_.class==FoaDecoderMatrix) {
			decoder = decoder_;
		} {
			if (decoder_==\stereo) {
				decoder = FoaDecoderMatrix.newStereo;
			} {
				if (decoder_==\mono) {
					decoder = FoaDecoderMatrix.newMono;
				} {
					
					if (decoder_==\50) {
						decoder = FoaDecoderMatrix.new5_0;	
					} {
						Error("SSC: argument 'decoder' should be an instance of FoaDecoderMatrix or one of the following symbols: \stereo, \mono, \50").throw;
					}
				}
			}
		};

		search = search_;
		duration = duration_;
		destination = destination_;
		
		soundLibraryPath = library;
			

		ready = false;
		readyCond = Condition(false);

		fork{
			var maxEntries;  // used to decide how many entries should be acquired
			var condition = Condition(false); // Condition to control flow

			// create temp folder - always the same
			parentFolderPath = PathName(thisProcess.nowExecutingPath).pathOnly.asString ++ "temporary_files"; 
			// parentFolderPath.postln;
			if (PathName(parentFolderPath).isFolder.not) {
				("mkdir " ++ parentFolderPath.asUnixPath).unixCmd({ 
					arg exitCode; 
					if (exitCode != 0) {
						Error("SSC: temporary_folder could not be created")
						.throw
					}
				});
			};

			// calculate how many entries are needed based on the assumption that each event will approximately last 25 seconds
			maxEntries = ((duration/25) * density).trunc.asInteger;

			// choose a fileName
			organicPath = parentFolderPath +/+ Date.getDate.asSortableString ++ "_" ++ UniqueID.next.asString ++ ".json";
			// organicPath.postln;
			
			// init organicDb
			organicDb = SSC_OrganicDb(parentFolderPath, soundLibraryPath); // THE SECOND PATH SHOULD POINT TO THE ACTUAL LOCATION OF THE FILES... 

			// populate with entries
			"SSC: Populating Dbase".postln;
			SSC_Acquire.acquireDataFile(search,nil, maxEntries, path:organicPath, doneFunction: {
				"SSC: Organic library retrieved".postln;
				("SSC: Ogranic library path is: " ++ organicPath.asString).postln;
				organicDb.addData(organicPath);
				condition.test_(true);
				condition.signal;
			});

			condition.test_(false);
			condition.signal;
			condition.wait;

			// first parse all data
			"SSC: Parsing and preprocessing database".postln;
			organicDb.prepare(maxEntries,10,1).wait;


			// "SSC: Downloading audiofiles".postln;
			// download audiofiles
			// organicDb.downloadFiles.wait;

			// analyze
			"SSC: analyzing entries and constructing behavior lists".postln;
			organicDb.analyze.wait;

			// interpet
			interpreter = SSC_Interpreter(organicDb);
			sonicSpace = [
				interpreter.interpret(duration, density, spread);
			];

			// create Runner
			runner = SSG_Runner(
				sonicSpace,  // sonicSpace
				SSG_FixedListener(0@0@2,spread*1.5), // a new static listener
				decoder  // the decoder
			);

			"ok".postln;

			ready = true;
			readyCond.test_(true);
			readyCond.signal;

			// this.bounce(); // bounce directly (Server version only)
		};
		^this;
	}

	// ===================== public methods =======================

	// ========================== bounce =============================

	bounce{
		var filePath = (organicDb.auxilaryFolderPath +/+ "Audio_Bounce_" ++ Date.getDate.asSortableString ++ "_" ++ UniqueID.next.asString).asString ++ ".aiff";
		filePath.postln;
		fork {
			readyCond.wait;
			"SSC: Now bouncing audio.".postln;
			runner.bounce(duration,filePath);
			"SSC: Renaming File!".postln;
			("mv " ++ filePath.asUnixPath ++ "  " ++ (organicDb.auxilaryFolderPath++"/../bounces/") ++ destination ++ ".aif").unixCmd;
			"SSC: Done!".postln;
			readyCond.test_(true);
			readyCond.signal;
		}
	}

	clean{

		// delete auxilary folder
		("rm -r " ++ organicDb.auxilaryFolderPath.asString).unixCmd;
		
	}
	// ===================== load/retrieve data =======================

}
