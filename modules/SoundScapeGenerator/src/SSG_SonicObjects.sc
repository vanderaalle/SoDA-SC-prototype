// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// Vertex Class Definition 

// Vertices are in essence just wrappers around actual audio files


// KNOWN BUG: Currently Disk Mode does not work as expected... (diskin will only load portions of the buffer which means that they have to be loaded again before playback..)

SSG_AbstractSonicObject {

	*new {  
		^super.new.pr_init(); 
	}

	pr_init {
		^this;
	}
	
	// impose architecture to subclasses 
	id {
		this.subclassResponsibility(thisMethod);
	}

	label{
		this.subclassResponsibility(thisMethod);
	}	

	info{
		this.subclassResponsibility(thisMethod);
	}

	duration{
		this.subclassResponsibility(thisMethod);
	}

	path{
		this.subclassResponsibility(thisMethod);
	}

	numChannels{
		this.subclassResponsibility(thisMethod);
	}

	buffer{
		this.subclassResponsibility(thisMethod);
	}

	disk{
		this.subclassResponsibility(thisMethod);
	}

	loaded{
		this.subclassResponsibility(thisMethod);
	}

	loadedCond{
		this.subclassResponsibility(thisMethod);
	}

	synthDefTag{
		this.subclassResponsibility(thisMethod);
	}

	play{
		this.subclassResponsibility(thisMethod);
	}

	load{
		this.subclassResponsibility(thisMethod);
	}

	free{
		this.subclassResponsibility(thisMethod);
	}

	delete{
		this.subclassResponsibility(thisMethod);
	}

	setLabel{
		this.subclassResponsibility(thisMethod);
	}

	setInfo{
		this.subclassResponsibility(thisMethod);
	}

}

SSG_NullSonicObject : SSG_AbstractSonicObject {
	
	*new { 
		^super.new.pr_init(); 
	}

	pr_init {
		^this;
	}
	
	// ============= overloaded methods  ==========================

	id { ^nil }
	label {^\nullSonicObject }
	info {^"this object does not point at any kind of audio data"}
	duration {^0}
	path{^nil}
	numChannels{^nil}
	buffer{^nil}
	disk{^nil}
	loaded{^true}
	loadedCond{^Condition(true)}
	synthDefTag{^nil}
	play { ^this }
	load { ^this } 
	free { ^this } 
	setLabel { ^this } 
	setInfo { ^this } 
}

SSG_SonicObject : SSG_AbstractSonicObject {

	// ============= class variables ==========================
	classvar counter = 0;  // (Integer) global counter used to created unique IDs
	classvar allPaths;     // (Dictionary) all paths in use should be registered here against unique ids so that no file is loaded to RAM twice. Two Vertices pointing at the same file should hold pointers to the same CtkBuffer. 
	classvar referencesCounter;     // (Dictionary) each id has a reference counter associated with it so that the -free method will decrement it accordingly and only free the audio data when there is nothing else pointing @ them. Likewise, if a new object is asked for an already registered path (whose data nevertheless have been freed), then a new object will be returned 
	
	// ============= public instance variables ================
	var < id;              // (Integer) a unique id
	var < label;           // (Symbol) a user-defined custom label
	var < info;            // (String) additional user-defined information
	var < duration;        // (Float) the duration of the SonicObject
	var < path;            // (String) the path of the audio file
	var < numChannels;     // (Integer) the number of channels of the SonicObject (currently only mono files are supported)
	var < sampleRate;      // (Integer) the audio file's sampling rate
	var < numFrames;       // (Integer) the number of frames of the file
	var < buffer;          // (CtkBuffer) the Buffer containing the audio
	var < disk;            // (Boolean) if true the file should be cued from the disk rather than loaded to RAM
	var < loaded;          // (Boolean) indicates whether the buffer has been loaded or not
	var < loadedCond;       // (Condition) a Condition indicating whether the buffer has been succesfully loaded to the Server or not. 
	var < synthDefTag;      // (Symbol) the name of the corrensponding synthDef Tag (this is used by instances of SSG_SonicSource to call the right synthDef from a CtkProtoNotes structure)

	// ============= private instance variables ================
	var freed = false;              // (Boolean) flag indicating whether the object is freed

	// ============= initClass  ================

	*initClass {
		Class.initClassTree(Dictionary);
		allPaths = IdentityDictionary.new;
		referencesCounter = IdentityDictionary.new;
	}

	// ============= constructor & private methods ================

	*new { 		

		arg              // expected arguments:
		path = "",       // (String) path to some file
		label = \no,     // (Symbol) a custom name for the object
		info = "",       // (String) a custom info-text for object
		disk = false;    // (Boolean) if true the file should be cued from the disk rather than loaded to RAM

		^super.new.pr_sub_init(path, label, info, disk);
	}
	
	pr_sub_init { arg path_, label_, info_, disk_;
		
		case  // ========= type-check all arguments here
		
		// type-check path_
		{ path_.class != String } { Error("SSG_SonicObject: 'path' argument should be an instance of String").throw }
		
		// type-check label_
		{label_.class != Symbol} { Error("SSG_SonicObject: 'label' argument should be an instance of Symbol").throw }

		// type-check info_
		{info_.class != String} { Error("SSG_SonicObject: 'info' argument should be an instance of String").throw }

		// type-check disk_
		{disk_.isKindOf(Boolean).not} { Error("SSG_SonicObject: 'disk' argument should be an instance of Boolean").throw };
		
		// if path doesn't exist return a NullSonicObject else proceed
		if ( PathName(path_).isFile.not ) { 
			// path_.postln;
			"SSG_SonicObject: path is invalid, an instance of SSG_NullSonicObject is returned instead.".postln;
			^SSG_NullSonicObject.new();
		} {

			// generate and assign a unique id
			counter = counter + 1;
			id = counter;    
			
			// init some variables
			label = label_;
			info = info_;
			path = path_;
			loaded = false;
			loadedCond = Condition(false);
			
			if (allPaths.includesKey(path_.asSymbol) && (referencesCounter[path_.asSymbol] != 0)) { // if path already registered 

				"SSG_SonicObject: Path already registered, returning a pointer instead".postln;

				// return a pointer to the existent buffer 
				buffer = allPaths[path_.asSymbol].buffer; 
				
				// assing synthDefTag
				disk = allPaths[path_.asSymbol].disk;

				// increment references counter
				referencesCounter[path_.asSymbol] = referencesCounter[path_.asSymbol] + 1;

			} { // else if path is new 

				// create a new buffer object
				if (disk_) { // if true cue from disk
					buffer = CtkBuffer.diskin(path); 
					"SSG_SonicObject: !!! Be careful disk mode is currently buggy and not fully implemented !!".postln;
				} { // else load to RAM
					buffer = CtkBuffer.playbuf(path); 
				};

				// buffer = CtkBuffer(path); 

				disk = disk_;

				// register object to database
				allPaths.put(path.asSymbol,this);
				referencesCounter.put(path_.asSymbol, 1);
			};

			// update the rest variables
			duration = buffer.duration;   
			numChannels = buffer.numChannels;
			sampleRate = buffer.sampleRate;
			numFrames = buffer.numFrames;

			// synthDefTag
			synthDefTag = (
				[\mono, \stereo][numChannels-1] ++ 
				Dictionary[\false->\RAM, \true->\Disk][disk.asSymbol]
			).asSymbol;

			// only mono/stereo files are currently supported !!
			if ( (numChannels !=1) && (numChannels !=2) ) {
				Error("SSG_SonicObject: only mono and stereo files are currently supported!").throw;
			};

			^this;
		}
	}



	// ============= public methods ================

	play { 
		if (freed.not) { // if not freed
			fork {
				if (loaded.not) {
					this.load(); 
					loadedCond.wait;
				};
				
				if (disk) {
					{ VDiskIn.ar(numChannels, buffer, buffer.sampleRate/Server.default.sampleRate) * Line.ar(1,1,duration,doneAction:2) }.play; // Line is only used for its doneAction
				} {			
					{ PlayBuf.ar(numChannels, buffer, buffer.sampleRate/Server.default.sampleRate) * Line.ar(1,1,duration,doneAction:2) }.play;  // Line is only used for its doneAction
				}
			};
		} {
			("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): The object has been freed and cannot be played back.").postln;
		}
	}

	load {  // * for RT use only
		if (freed.not) { // if not freed
			if (loaded.not) {
				Server.default.waitForBoot({
					buffer.load(sync:true, // load buffer
						onComplete: {                 // schedule on completion
							loaded = true;                 // update flag
							loadedCond.test_(true).signal; // update Condition
							("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): Audio data successfully loaded to memory.").postln;
						}
					);  
				});
			};
		} {
			("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): The object has been freed and cannot be loaded.").postln;
		};
		^this;
	}

	free {  // * for RT use only
		
		// set to freed
		freed = true;
		
		// check if there are other objects pointing to this buffer
		if (referencesCounter[path.asSymbol] <= 1) {
			buffer.free;
			loaded = false;                 // update flag
			loadedCond.test_(false);        // update Condition
			// update counter
			referencesCounter[path.asSymbol] = 0;
			// notify user
			("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): audio data successfully freed from memory.").postln;
		} {
			// update counter
			referencesCounter[path.asSymbol] = referencesCounter[path.asSymbol] - 1;
			// notify user
			("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): multiple references exist for this object, reference count decreament but data not freed.").postln;
		};
		^this;
	}

	// ======== delete and de-register vertex  ===============


	// delete {  
	// 	buffer.free;
	// 	allPaths.removeAt(path.asSymbol);
	// 	^nil;
	// }

	// ===================== public setters =========================

	setLabel {

		arg label_; 
		
		// type-check argument and assing if ok
		if (label_.class != Symbol) {
			Error("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): Error, argument was not an instance of Symbol.").throw;
		} {
			label = label_;
			("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): label has been succesfully set to:" + label ++ ".").postln;
		}

		^this;
	}

	setInfo {
		
		arg info_; 
		
		// type-check argument and assing if ok
		if (info_.class != String) {
			Error("SSG_SonicObject \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of String.").throw;
		} {
			info = info_;
			("SSG_SonicObject \(id:" ++ " " ++ id ++ "\): Info has been succesfully set to:" + info).postln;
		}

		^this;
	}

}
