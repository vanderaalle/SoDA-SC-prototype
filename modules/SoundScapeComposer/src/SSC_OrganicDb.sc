// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013-14

// this file is part of the SoundScape Composer, which has been developed as part of the Sound Design Accelerator (SoDA) project

// This class parses, analyses and preprocesses an Organic Database and exports a behavior list


SSC_OrganicDb {

	// member variables

	var < auxilaryFolderPath; // the Path to the auxilaryFolder

	var < rawData;         // (Array of Dictionary) the raw Data
	var < entries;         // (Array of Dictionary) processed Data

	var < sequences;     // (Array of Dictionary) SonicSequences
	var < atoms;         // (Array of Dictionary) Atoms grouped upon relevance
	var < atmos;         // (Array) Atmospheres

	var < groupedSequences;  // (Array of Array of Dictionary) grouped according to correlation
	var < groupedAtoms;      // (Array of Array of Dictionary) grouped according to correlation

	var < completions;       // (Array of String) a list of all completions made to construct the behaviorlist (used mostly for debugging)


	// ===================== initClass =======================

	*initClass {
		Class.initClassTree(JSONFileReader);
	}


	// ===================== new/init =======================

	*new{ arg path; // Auxilary Folder path

		^super.new.pr_init(path);
	}

	pr_init{ 

		arg path_; // Auxilary Folder Path

		case 
		{ path_.isKindOf(String).not && PathName(path_).isFolder.not}
		{ Error("SSC_OrganicDb: 'path' argument should be an instance of String pointing at an existent folder").throw};

		// all auxilary files should go here
		auxilaryFolderPath = path_;

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
		
		// clean up and preprocess
		entries.do{arg item; 
			
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
			
			// add address to the file's distant location
			item.put("downloadPath", item["url"]++item["title"]);
			item.put("filePath", auxilaryFolderPath++item["title"]);

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

		// save data according to type
		sequences = entries.select{arg i; i["typeOfSoundObject"]=="Sequence"};
		atmos = entries.select{arg i; i["typeOfSoundObject"]=="Atmos"};
		atoms = entries.select{arg i; i["typeOfSoundObject"]=="Atom"};

		// // if correlation is true, group sequences according to relevance
		// if (correlation) {
		// 	groupedSequences = nil; // delete prevously groupedSequences
		// 	while ({ sequences.size > 0 }, { 			// group sequences according to relevance
		// 		// group and copy relevant sequences 
		// 		groupedSequences = groupedSequences.add(
		// 			sequences.select{arg i; 
		// 				i["description"]==sequences[0]["description"]
		// 			});
		// 		// delete them
		// 		sequences = sequences.reject{arg i; 
		// 			i["description"]==sequences[0]["description"]
		// 		};		
		// 	});
		// 	sequences = groupedSequences.flatten; // rename 
		// };
		
		// group atoms 
		groupedAtoms = nil;
		// group atoms according to relevance !!	
		while ({ atoms.size > 0 }, {
			// group and copy relevant atoms 
			groupedAtoms = groupedAtoms.add(
				atoms.select{arg i; 
					i["description"]==atoms[0]["description"]
				});
			// delete them
			atoms = atoms.reject{arg i; 
				i["description"]==atoms[0]["description"]
			};	
		});

		// "3".postln;
		// groupedAtoms.do{ arg i;
		// 	i.do{arg j; 
		// 		[j["class"], j["concatenation"]].postln;
		// 	}
		// };

		// groups of atoms should feature at least 3 or at least "concatenations" (if smaller that 3) elements
		groupedAtoms = groupedAtoms.select{ arg i; 
			i.size > min(3,(3 ? i[0]["concatenation"]));
		};	

		// limit atoms
		groupedAtoms = groupedAtoms.collect{arg i; 
			if (i.size > maxAtomsPerSequence ) {
				i[(0..maxAtomsPerSequence)]
			} { i };
		};

		// ------ Make Sure entries exist for all key-words
		// sequences 
		// first put everything in an IdentityDictionary according to their info class
		seqDict = IdentityDictionary.new;
		sequences.do{arg i;
			seqDict[i["class"].asSymbol] = seqDict[i["class"].asSymbol].add(i) };
		// then re-arrange them so that elements from all groups are present
		sequences = seqDict.asArray.flop.flatten.as(OrderedIdentitySet).asArray;

		// atoms (consider info class to be that of the first element)
		// first put everything in an IdentityDictionary according to their info class
		atomDict = IdentityDictionary.new;
		groupedAtoms.do{arg i;
			// i.do{arg j; j["class"].postln};
			atomDict[i[0]["class"].asSymbol] = atomDict[i[0]["class"].asSymbol].add(i) };
		// then re-arrange them so that elements from all groups are present
		"dictEntries".postln;
		atomDict.keysDo{arg i; i.postln};

		groupedAtoms = atomDict.asArray.flop.flatten.as(OrderedIdentitySet).asArray;
		atoms = groupedAtoms;
		// "result".postln;
		// atoms.do{arg i; i[0]["class"].postln};

		// SELECT ONLY A TOTAL OF MAX_ENTRIES ELEMENTS
		
		// make sure entries are equal to maxEntries
		while ( {(atoms.size + atmos.size + sequences.size) > maxEntries}, 
			{ 
				if (atoms.size > sequences.size) 
				{ atoms.pop; } { sequences.pop; };
			};
		);

		// re-organize
		groupedAtoms = atoms;
		entries = atmos ++ atoms.flatten ++ sequences;

		"OrganicDb: done parsing".postln;
		
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
				
				var fileName = auxilaryFolderPath ++ item["title"];
				
				condInternal.test_(false);
				condInternal.signal;
				
				("OrganicDb: Now downloading " ++ item["title"]).postln;
				("curl -o " ++ fileName.asUnixPath ++ " " ++ item["downloadPath"].asUnixPath).unixCmd({
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
		
		entries = atoms.flatten ++ sequences ++ atmos;

		condition.test_(true);
		condition.signal;
		// };

		^condition;
	}

	// ================== hard-coded rules for behavior-lists ==========

	pr_completeAtmos{ arg item; 
		// create behavior List for atmo-specific rules
		
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

}

