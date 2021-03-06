// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013-14

// this file is part of the SoundScape Composer, which has been developed as part of the Sound Design Accelerator (SoDA) project

// This class parses, analyses and preprocesses an Organic Database and exports a behavior list


SSC_OrganicDb {

	// member variables

	var < auxilaryFolderPath; // the Path to the auxilaryFolder
	var < soundLibraryPath; // the Path to the audio Library

	var < rawData;         // (Array of Dictionary) the raw Data
	var < rawAtoms;         // (Array of Dictionary) the raw atoms
	var < entries;         // (Array of Dictionary) processed Data

	var < sequences;     // (Array of Dictionary) SonicSequences
	var < atoms;         // (Array of Dictionary) Atoms grouped upon relevance
	var < atmos;         // (Array) Atmospheres
	var < air;           // (Array) Background Air

	var < completions;       // (Array of String) a list of all completions made to construct the behaviorlist (used mostly for debugging)


	// ===================== initClass =======================

	*initClass {
		Class.initClassTree(JSONFileReader);
	}


	// ===================== new/init =======================

	*new{ arg path, soundLibraryPath; // Auxilary Folder path

		^super.new.pr_init(path, soundLibraryPath);
	}

	pr_init{ 

		arg path_, // Auxilary Folder Path
		soundLibraryPath_; // Auxilary Folder Path

		case 
		{ path_.isKindOf(String).not && PathName(path_).isFolder.not}
		{ Error("SSC_OrganicDb: 'path' argument should be an instance of String pointing at an existent folder").throw}
		{ soundLibraryPath_.isKindOf(String).not && PathName(path_).isFolder.not}
		{ Error("SSC_OrganicDb: 'soundLibraryPath' argument should be an instance of String pointing at an existent folder").throw};

		// all auxilary files should go here
		auxilaryFolderPath = path_;
		soundLibraryPath = soundLibraryPath_;

		// init raw Data;
		rawData = Array.new;

		^this;
	}

	// ===================== add entries =======================
	addData{
		arg filePath; 

		// type checking
		case 
		{ filePath.isKindOf(String).not && (PathName(filePath).extension != "json") && PathName(filePath).isFile.not}
		{ Error("SSC_OrganicDb: 'filePath' argument should be an instance of String pointing at a json file").throw};

		rawData = rawData.add(JSONFileReader.read(filePath)["result"]["documents"]);
		rawAtoms = rawAtoms.add(JSONFileReader.read(filePath)["result"]["atoms"]);
		rawData = rawData.flatten;
		^this;
	}

	// ===================== parse =======================

	prepare{ arg maxEntries = 100, maxAtomsPerSequence = 10, maxAtmos = 1;
		
		var condition = Condition(false); // since I use it everywhere..
		var seqDict, atomDict; // Dictionaries used to re-organize entries

		// var condition = Condition(false);
		"OrganicDb: Now parsing and pre-processing database".postln;
		
		entries = rawData.deepCopy; // so that rawData is kept intact

		// clean up and preprocess entries
		this.preprocess(entries);

		// clean up and preprocess atoms
		atoms = rawAtoms; 
		atoms = atoms.select({arg i; i.size >= 2 }); // consider only groups of at least 2 atoms
		atoms.do{ arg group;
			group.do {arg item; 
				this.preprocess(item); // preprocess them
			};
		};

		// save data according to type
		sequences = entries.select{arg i; i["typeOfSoundObject"]=="sequence"};
		atmos = entries.select{arg i; i["typeOfSoundObject"]=="midground"};
		atmos = atmos.asArray;
		air = entries.select{arg i; i["typeOfSoundObject"]=="background"};
		air = air.asArray; // it's better to have everything registered as Array even if it contains just one entry for consistency
		
		"----".postln;
		"OrganicDb: done parsing, list of returned entries follow:".postln;
		"Background".postln;
		air.do{arg i; i["title"].postln};
		"Midground".postln;
		atmos.do{arg i; i["title"].postln};
		"Sequences".postln;
		sequences.do{arg i; i["title"].postln};
		"Atoms".postln;
		atoms.do{arg j; 
			j.do{ arg i, k; 
				("Atomic group " ++ k ++":").postln;
				i.do{arg item;
					item["title"].postln;
				};
			};
		};
		"----".postln;
		
		atoms = atoms[0]; // it only contains an array that contains other arrays

		// update entries
		if (atoms.isKindOf(Array)) {
			entries = atoms.flatten(2) ++ sequences ++ atmos ++ air;
		} {
			entries = sequences ++ atmos ++ air;
		};
		
		condition.test_(true);
		condition.signal;
		^condition;
	}

	// ===================== download files =======================
	
	downloadFiles{
		var condition = Condition(false);     // Condition to notify external processes
		var condInternal = Condition(false); // Condition to notify internal processes

		
		// download all entries
		"OrganicDb: Now downloading audio-files from the server".postln;
		fork{
			entries.do{
				arg item; 
				
				var fileName = auxilaryFolderPath +/+ item["title"];
				
				condInternal.test_(false);
				condInternal.signal;
				
				("OrganicDb: Now downloading " ++ item["title"]).postln;
				item["downloadPath"].asUnixPath.postln;
				("curl -u soda:files -o " ++ fileName.asUnixPath ++ " " ++ item["downloadPath"].asUnixPath).unixCmd({
					"OrganicDb: done !".postln;
					condInternal.test_(true);
					condInternal.signal;
				});	
				condInternal.wait;
			};
			
			condition.test_(true);
			condition.signal;
		};
		
		^condition;
	}


	// ===================== preprocess =======================

	analyze{

		// construct behavior lists and perform conversions whenever applicable
		var condition = Condition.new(false);

		// fork{
		// construct behavior list for each entry - defaults are given for those cases where values are not found or are erroneous
		
		// all entries
		atmos.do{
			arg item;
			this.pr_completeAtmos(item);
		};

		// airs
		air.do{
			arg item;
			this.pr_completeAir(item);
		};

		// sequences
		sequences.do{
			arg item; 
			this.pr_completeSequence(item);
		};

		// atoms
		atoms.do{arg array;
			array.do{ arg item;
				// atoms need both sequence and atom completion
				this.pr_completeSequence(item); 
				this.pr_completeAtom(item);
			};
		};
		
		if (atoms.isKindOf(Array)) {
			entries = atoms.flatten(2) ++ sequences ++ atmos ++ air;
		} {
			entries = sequences ++ atmos ++ air;
		};

		condition.test_(true);
		condition.signal;
		^condition;
	}

	// ================== hard-coded rules for behavior-lists ==========

	pr_completeAir{ arg item; 
		// create behavior List for air-specific rules

		// typeOfSpace
		if ( (item["typeOfSpace"] != "interior") || (item["typeOfSpace"] != "exterior")) { // if neither interior or exterior
			// use default value and register change
			item["typeOfSpace"] = "exterior"; 
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'typeOfSpace' set to exterior");
		};

		// relative amplitude
		if ( item["relativeAmplitude"].isNil) { // if nil
			// use default value and register change
			item["relativeAmplitude"] = 0.1; 
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'relativeAmplitude' set to 0.1");
		} { // else if out of range
			if (item["relativeAmplitude"].asFloat.inRange(0,1).not){
				// use default value and register change
				item["relativeAmplitude"] = 0.1; 
				completions = completions.add(item["title"]
					.asString ++ " -> " ++ 
					"'relativeAmplitude' set to 0.1");
			} { // else simply convert and use as is
				item["relativeAmplitude"] = 
				item["relativeAmplitude"].asFloat;
			}
		};
	}

	pr_completeAtmos{ arg item; 
		// create behavior List for atmo-specific rules
		
		// relative amplitude
		if ( item["relativeAmplitude"].isNil) { // if nil
			// use default value and register change
			item["relativeAmplitude"] = 0.2; 
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'relativeAmplitude' set to 0.2");
		} { // else if out of range
			if (item["relativeAmplitude"].asFloat.inRange(0,1).not){
				// use default value and register change
				item["relativeAmplitude"] = 0.2; 
				completions = completions.add(item["title"]
					.asString ++ " -> " ++ 
					"'relativeAmplitude' set to 0.2");
			} { // else simply convert and use as is
				item["relativeAmplitude"] = 
				item["relativeAmplitude"].asFloat;
			}
		};
	}

	pr_completeSequence{ arg item; 
		// create behavior List for sequence-specific rules

		// relative amplitude
		if ( item["relativeAmplitude"].isNil) { // if nil
			// use default value and register change
			item["relativeAmplitude"] = 0.5; 
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'relativeAmplitude' set to 0.5");
		} { // else if out of range
			if (item["relativeAmplitude"].asFloat.inRange(0,1).not){
				// use default value and register change
				item["relativeAmplitude"] = 0.5; 
				completions = completions.add(item["title"]
					.asString ++ " -> " ++ 
					"'relativeAmplitude' set to 0.5");
			} { // else simply convert and use as is
				item["relativeAmplitude"] = 
				item["relativeAmplitude"].asFloat;
			}
		};

		// rangeOfMovement
		if ( item["rangeOfMovement"].isNil) { // if nil
			// use default value and register change
			item["rangeOfMovement"] = [0,0,0]; 
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'rangeOfMovement' set to  [0,0,0]");
		} { // else if not an Array 
			if ((item["rangeOfMovement"].interpret.class==Array).not) {
				// use default value and register change
				item["rangeOfMovement"] = [0,0,0]; 
				completions = completions.add(item["title"].asString 
					++ " -> " ++ "'rangeOfMovement' set to  [0,0,0]");
			} {
				// else simply convert and use as is
				item["rangeOfMovement"] = 
				item["rangeOfMovement"].interpret.abs;
			}
		};

		// speed of movement
		if ( item["speedOfMovement"].isNil) { // if nil 
			// use default value and register change
			item["speedOfMovement"] = 0.4;  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'speedOfMovement' set to 0.4");
		} { // else if slow or fast
			if ((item["speedOfMovement"] != "slow") && (item["speedOfMovement"] != "fast")) {
				item["speedOfMovement"] = 
				item["speedOfMovement"].asFloat;
			} {
				case {item["speedOfMovement"] == "slow"}
				{item["speedOfMovement"] = 0.2;}
				{item["speedOfMovement"] == "fast"}
				{item["speedOfMovement"] = 0.8;};
			}
		};	

		// acceleration
		if ( item["acceleration"].isNil) { // if nil
			// use default value and register change
			item["acceleration"] = "absolute";  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'acceleration' set to 'absolute'");
		} { // else if out of range
			if ((item["acceleration"] != "absolute") 
				&& (item["acceleration"] != "angular")
				&& (item["acceleration"] != "coriolis") ){
					// use default value and register change
					item["acceleration"] = "absolute";  
					completions = completions.add(item["title"].asString 
						++ " -> " ++ "'acceleration' set to 'absolute'");
				} { // else simply convert and use as is
					item["acceleration"] = 
					item["acceleration"];
				}
		};	

		// typeOfShot
		if ( item["typeOfShot"].isNil) { // if nil
			// use default value and register change
			item["typeOfShot"] = "CU";  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'typeOfShot' set to 'CU'");
		} { // else if out of range
			if ((item["typeOfShot"] != "CU") 
				&& (item["typeOfShot"] != "MS")
				&& (item["typeOfShot"] != "LS") ){
					// use default value and register change
					item["typeOfShot"] = "CU";  
					completions = completions.add(item["title"].asString 
						++ " -> " ++ "'typeOfShot' set to 'CU'");
				} { // else simply convert and use as is
					item["typeOfShot"] = item["typeOfShot"];
				}
		};	

		// size
		if ( item["size"].isNil) { // if nil
			// use default value and register change
			item["size"] = 0.2;  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'size' set to 0.2");
		} { // else simply convert and use as is
			item["size"] = 
			item["size"].asFloat;
		};

		// repeatable
		if ( item["repeatable"].isNil) { // if nil
			// use default value and register change
			item["repeatable"] = 0;  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'repeatable' set to 0");
		} { // else simply convert and use as is
			item["repeatable"] = 
			item["repeatable"].asFloat.asInteger;
		};	

	}

	pr_completeAtom{ arg item; 
		// create behavior List for atom-specific rules
		
		// frequency
		if ( item["frequency"].isNil) { // if nil
			// use default value and register change
			item["frequency"] = 0.3;  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'frequency' set to '0.3'");
		} { // else if out of range
			if (item["frequency"].asFloat.inRange(0,10000).not) {
				// use default value and register change
				item["frequency"] = 0.3;  
				completions = completions.add(item["title"].asString 
					++ " -> " ++ "'frequency' set to '0.3'");
			} { // else simply convert and use as is
				item["frequency"] = item["frequency"].asFloat/1000;
			}
		};	

		// concatenation
		if ( item["concatenation"].isNil) { // if nil
			// use default value and register change
			item["concatenation"] = 3;  
			completions = completions.add(item["title"].asString 
				++ " -> " ++ "'concatenation' set to '3'");
		} { // else if out of range
			if (item["concatenation"].asInteger.inRange(0,20).not) {
				// use default value and register change
				item["concatenation"] = "3";  
				completions = completions.add(item["title"].asString 
					++ " -> " ++ "'concatenation' set to '3'");
			} { // else simply convert and use as is
				item["concatenation"] = item["concatenation"].asInteger;
			}
		};	
	}

	preprocess{ arg group;

		group.do{arg item; 
			
			var additionalTags, class;

			// remove irrelevant tags
			item.removeAt("collectionNameSoda");
			item.removeAt("contents_lemma_en");
			item.removeAt("contents_text_en");
			item.removeAt("highlightedText");
			item.removeAt("luceneDocId");
			item.removeAt("periodEndYear");
			item.removeAt("periodStartYear");
			item.removeAt("tags");
			item.removeAt("uuid");

			// simplify some tags
			if (item["typeOfSoundObject"].isKindOf(Array)) {
				item["typeOfSoundObject"] = item["typeOfSoundObject"][0];
			};
			if (item["typeOfSpace"].isKindOf(Array)) {
				item["typeOfSpace"] = item["typeOfSpace"][0];
			};
			
			// downloadPath is the "url"
			item.put("downloadPath", item["url"]);

			// relative path is calculated with respect to the ftp hierarchy and a library path
			if (soundLibraryPath[soundLibraryPath.size-1]==$/) { // if soundLibraryPath ends in /
				item.put("filePath", soundLibraryPath ++ item["url"].replace("ftp://91.212.167.101/","")); 
			} {
				item.put("filePath", soundLibraryPath ++ item["url"].replace("ftp://91.212.167.101","")); 	
			};

			// TEST ONLY
			// item.put("filePath", "/Users/marinos/projects/ongoing/SoDA/SoDA SC prototype/modules/SoundScapeComposer/temporary_files/" ++ item["title"]);

			// retrieve and store class information
			item["classes"].do{arg i; 
				// i["facetId"].postln;
				case 
				{i["facetId"].beginsWith("protonext_")}
				{class = i["facetId"].replace("protonext_","")}
				{i["facetId"].beginsWith("protontop_")}
				{class = i["facetId"].replace("protontop_","")}
				{i["facetId"].beginsWith("soda_")}
				{class = i["facetId"].replace("soda_","")}
				// { true } // in all other cases
				// {class = "no_class"};
			};
			item.put("class",class);

			// search for additional tags and add them as entries
			additionalTags = (item["classes"].collect{arg j; j["additionalInfo"]}).flatten;
			additionalTags = additionalTags.do{
				arg i; 
				item.put(
					i["firstElem"].replace("http://arcadia.celi.it/ontologies/soda#",""), 
					i["secondElem"].replace("^^http://www.w3.org/2001/XMLSchema#decimal","")
				)
			};		
			
			item.removeAt("classes");
			item.removeAt("classesExtended");
		};
	}

}

