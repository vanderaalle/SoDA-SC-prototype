// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013-14

// this file is part of the SoundScape Composer, which has been developed as part of the Sound Design Accelerator (SoDA) project

// Interpret interprets the (prepared) Organic database and populates the SonicSpace accordingly so that SSC may compute the SoundScape

SSC_Interpreter {


	var database;        // (SSC_OrganicDb) The prepated organic database 
	
	var < memory;          // (Dictionary of Dictionary) Internal Memory module

	var sonicSpace;      // (SSG_SoundZone)

	var	duration;
	var	density;
	var	spread;

	var densityFactor;  // a densityFactor used to some calculations
	var zonePosition;     // (Cartesian) the absolute position of the SonicSpace
	var zoneBounds;       // (Cartesian) the bounds position of the SonicSpace

	// ===================== initClass  =======================


	// ===================== new/init =======================

	*new{
		arg database; // (SSC_OrganicDb) 
		
		^super.new.pr_init(database);
	}

	pr_init{ arg database_; 
		// type check
		case 
		{database_.isKindOf(SSC_OrganicDb).not}
		{Error("SSC_Interpreter: 'database' should be an instance of SSC_OrganicDb").throw};

		database = database_;
		
		// init memory
		memory = Dictionary.new;

		// "SSC_Interpreter: Database loaded.".postln;
		
		// example of Memory per entry
		// memory["title"] = Dictionary[
		// 	"occurences"->[],    // this should contain an Array of start/end times for each Source
		// 	"position"->Cartesian
		//  "trajectory"->ArrayOfEnv
		// ];
		^this;
	}

	// ===================== interpret =======================
	
	interpret{ arg 
		duration_ = 240,          // (Number) total duration of soundscape
		density_ = 2,             // (Integer) how many simultaneous different events (approximation)
		spread_ = 200;            // (Number) spatial spread of soundscape

		var sequences, air, atoms, airObject;   // the sequences and the atmosphere
		var reverbProfile; // the reverb profile

		// type check here
		case
		{duration_.isKindOf(Number).not} {
			Error("SSC_Interpreter: argument 'duration' should be an instance of Number").throw;}
		{density_.isKindOf(Number).not} {
			Error("SSC_Interpreter: argument 'density' should be an instance of Integer").throw;}
		{spread_.isKindOf(Number).not} {
			Error("SSC_Interpreter: argument 'spread' should be an instance of Number").throw;};

		duration =duration_;
		density = density_;
		spread = spread_;

		// absolute zonePosition
		zonePosition = Cartesian(
			(spread/2).asInteger.neg, (spread/2).asInteger.neg, (spread/2).asInteger.neg
		); // so that the absolute center is at 0@0@0

		// bounds of sonicSpace
		zoneBounds = spread@spread@spread;

		// ================ air ============================
		"SSC_Interpreter: Configuring Background.".postln;
		// use just 1 atmo only for now
		air = database.air[0]; // select the first atmosphere
		airObject = SSG_SonicObject(air["filePath"], 
					air["title"].asSymbol,
					air["description"]
				); 
		air = SSG_SonicAtmosphere( 
			SSG_Sequence(
				Pseq([airObject],inf),
				Pseq([airObject.duration],inf),
				10, // fadeTime
			),
			air["relativeAmplitude"],
			air["title"].asSymbol,
			air["description"],
		);

		// ================ calculate reverb profile ==========

		if (database.air[0]["typeOfSpace"]=="interior") {
			reverbProfile = [spread,0.09,0.7,0.1,0.05];
			"SSC_Interpreter: Interior space identified, using reverb profile".postln;
		} {
			reverbProfile = [spread,0,0,0,0];
			"SSC_Interpreter: Exterior space identified, no reverb used".postln;
		};

		// ================ init sonicSpace ===================
		"SSC_Interpreter: Initialize sonicSpace.".postln;
		sonicSpace = SSG_SoundZone(
			bounds: zoneBounds,  
			position: zonePosition,
			soundSources: [air], // with the atmosphere & air
			opacity: 1, 
			filterSlope:1, 
			reverbProfile: reverbProfile
		);


		// ================ atmo ============================
		"SSC_Interpreter: Configuring Midground.".postln;
		database.atmos.do{
			arg atmo;
			var atmosphere = atmo;
			var atmoObject = SSG_SonicObject(atmosphere["filePath"], 
				atmosphere["title"].asSymbol,
				atmosphere["description"]
			); 
			if (atmoObject.isKindOf(SSG_NullSonicObject).not) {
				atmosphere = SSG_SonicAtmosphere( 
					SSG_Sequence(
						Pseq([atmoObject],inf),
						Pseq([atmoObject.duration],inf),
						10, // fadeTime
					),
					atmosphere["relativeAmplitude"],
					atmosphere["title"].asSymbol,
					atmosphere["description"],
				);
				sonicSpace.addSoundSource( atmosphere );
			}
		};	


		// ================ sequences ============================

		sequences = database.sequences;
		sequences.do{ arg sequence; // for each sequence
			("SSC_Interpret: Configuring and adding \'"++sequence["title"]++"\' to SonicSpace").postln;
			this.pr_interpretSequence(sequence); 
		};

		// ================ atoms ============================
		atoms = database.atoms;
		atoms.do{ arg atoms; // for each group of atoms
			"SSC_Interpret: Creating contingent sequence out of group of atoms.".postln;
			this.pr_interpretAtoms(atoms); 
		};

		// return sonicSpace
		^sonicSpace;
	}


	// ================ sequences =========================
	
	pr_interpretSequence{
		arg sequence; 

		var object;           // the SonicObject

		// used to calculate timesOfOcucurence
		var timesOfOccurence; // when it will sound
		var allowedTimeSlots; // a Set with the allowed time-slots in 5sec resolution for
		var timeSlotsResolution = 2; // the resolution of the timeSlots (to limit processing)

		var objectDuration;      // the duration of the sound object
		
		// register to memory
		memory.put(sequence["title"], Dictionary["occurences"->[]]); // occurences have to be initialized because I use .add later

		// register sound object
		object = SSG_SonicObject(sequence["filePath"], 
			sequence["title"].asSymbol,
			sequence["description"]
		);		

		// calculate object's duration
		objectDuration = object.duration.asInteger;

		// calculate time of Occurence (consider density and tags)
		timesOfOccurence = Array.new;

		// an array with the allowed time slots
		if (duration > objectDuration) {
			allowedTimeSlots = (0,(timeSlotsResolution+1)..(duration-objectDuration)).asSet; 
		} {
			"!!! SSC_Interpreter: Something looks wrong - the duration of an  sequence is greater than the total duration of the soundscape..".postln;
			allowedTimeSlots = (0,(timeSlotsResolution+1)..duration).asSet; 	
		};

		
		// delete slots already occupied by other Sources (with respect to density)

		memory.do{arg item; 
			item["occurences"].do{
				arg array; 
				var range = Range(array[0],array[1]-array[0]);
				// if part of the range is contained in allowedTimeSlots
				if (allowedTimeSlots.any(range.includes(_))) { 
					range.do{ arg j;
						allowedTimeSlots.remove(j);
					};
				};
			};
		};
					
		if (allowedTimeSlots.size>0) { // if there are time-slots
			// select a random time
			var time = allowedTimeSlots.choose;
			// register it to memory
			memory[sequence["title"]]["occurences"].add([
				time, time + objectDuration
			]);
			// and add it to timesOdOccurence Array
			timesOfOccurence.add(time); 
			// ["timesOfOccurence",timesOfOccurence].postln; // debug
		} {
			// if there are no time slots (THERE SHOULD BE REALLY) put it somewhere randomly
			var time = rrand(0,duration);

			"SSC_Interpret: No available time slots, sequence is randomly positioned somewhere.".postln;
			
			// register it to memory
			memory[sequence["title"]]["occurences"].add([
				time, time + objectDuration
			]);
			// and add it to timesOdOccurence Array
			timesOfOccurence.add(time); 
			// object = nil; // let garbage collector know
		};	
		
		// create source
		this.pr_createSource(sequence, object, timesOfOccurence);
	}

	// ================ atoms ============================
	pr_interpretAtoms{ arg atoms;

		var objects;                // sonic Objects
		var atomSequence;           // the SonicAtomSequence
		var pSound, pDur, fadeTime; // AtomSequence's arguments

		// used to calculare timesOfOcucurence
		var timesOfOccurence; // when it will sound
		var allowedTimeSlots; // a Set with the allowed time-slots in 5sec resolution for
		var timeSlotsResolution = 2; // the resolution of the timeSlots (to limit processing)

		var atomSequenceDuration;      // the duration of the sound object
		var concatenation;             // concatenation index
		
		// calculate concatenation index
		if (atoms[0]["concatenation"] < atoms.size) {
			concatenation = atoms[0]["concatenation"];
		} {
			concatenation = atoms.size;
		};
		
		// register to memory
		memory.put(atoms[0]["title"], Dictionary["occurences"->[]]); // occurences have to be initialized because I use .add later

		// make sure all atoms do exist 
		// atoms = atoms.select{arg i; i["filePath"].isFile.postln};
				
		// create sound objects out of atoms
		objects = Array.new;
		atoms.do{ arg atom; 
			objects = objects.add(
				// register sound object
				SSG_SonicObject(atom["filePath"], 
					atom["title"].asSymbol,
					atom["description"]
				);	
			);
		};

		// consider frequency/concatenation of the first atom only (it is the OrganicDb's job to normalize any incosistencies)
		
		pSound = Pxrand( 
			(objects.scramble[(0..concatenation)] 
				++ SSG_NullSonicObject.new),
			inf
		);
		
		// pDur
		pDur = Prand([ objects.choose.duration + atoms[0]["frequency"]],rrand(5,15));

		// create atomSequence
		atomSequence = SSG_Sequence(pSound, pDur, 0.05);

		// calculate average atomSequence duration
		atomSequenceDuration = objects.choose.duration.asInteger * concatenation;

		// debug
		// objects.do{
		// 	arg i; 
		// 	[i.label, i.duration].postln;
		// };

		// calculate time of Occurence (consider density and tags)
		timesOfOccurence = Array.new;

		// an array with the allowed time slots
		if (duration > atomSequenceDuration) {
			allowedTimeSlots = (0,timeSlotsResolution..(duration-atomSequenceDuration)).asSet; 
		} {
			"!!! SSC_Interpreter: Something looks wrong - the average duration of an atomic sequence is greater than the total duration of the soundscape..".postln;
			allowedTimeSlots = (0,timeSlotsResolution..duration).asSet; 	
		};

		// delete slots already occupied by other Sources (with respect to density)
		memory.do{arg item; 
			item["occurences"].do{
				arg array; 
				var range = Range(array[0],array[1]-array[0]);
				// if part of the range is contained in allowedTimeSlots
				if (allowedTimeSlots.any(range.includes(_))) { 
						range.do{ arg j;
							allowedTimeSlots.remove(j);
					}
				};
			};
		};

		// ["allowedTimeSlots",allowedTimeSlots].postln; // debug
		
		if (allowedTimeSlots.size>0) { // if there are time-slots
			// select a random time
			var time = allowedTimeSlots.choose;
			// register it to memory
			memory[atoms[0]["title"]]
			["occurences"].add([
				time, time + atomSequenceDuration
			]);
			// and add it to timesOdOccurence Array
			timesOfOccurence.add(time); 
			// ["timesOfOccurence",timesOfOccurence].postln; // debug
		} {
			// if there are no time slots (THERE SHOULD BE REALLY) put it somewhere randomly
			var time = rrand(0,duration);
			"SSC_Interpret: No available time slots, sequence is randomly positioned somewhere.".postln;
			
			// register it to memory
			memory[atoms[0]["title"]]
			["occurences"].add([
				time, time + atomSequenceDuration
			]);
			
			// and add it to timesOdOccurence Array
			timesOfOccurence.add(time); 
			// object = nil; // let garbage collector know
		};		

		// create source
		if ( timesOfOccurence.size > 0 ) { // if the object is to occur 
			this.pr_createSource(atoms[0], atomSequence, timesOfOccurence);
		};
	}
	
	// ================ create Source ============================

	pr_createSource{ arg sequence, object, timesOfOccurence;

		var source;           // the source

		// ("SSC_Interpreter: " ++ sequence["title"] ++ " will occur @ " ++ timesOfOccurence.asString).postln;

		if ( sequence["rangeOfMovement"] == [0,0,0]) { // in case it is static
			// calculate position
			var position; 
			var breakIndex = 50; // only try 50 times to find an empty slot

			if (sequence["typeOfShot"]=="CU") { // if close-up
				{ // recursive clause 
					var func = thisFunction;
					var tolerance = Cartesian(  // tolerance (how near to an existing source is sth allowed to be)
						(spread/10).asInteger, 
						(spread/10).asInteger,
						(spread/10).asInteger
					);
					
					position = zonePosition.rrand(zoneBounds + zonePosition); // a random position	
					position = position + 15; // make sure source is at least 15 meters away from the listener
					position.z = rrand(-7,7); // limit height 

					if (memory.collect({arg i; i["position"]}).any({ // if sth else is already located nearby
						arg item;  
						item.isWithinArea(position, tolerance);
					})) {  if (breakIndex>0) { // try only breadIndex times
						breakIndex = breakIndex - 1;	
						func.value; // find another values
					}} 
				}.value;

			} { // else place somewhere near the listener (since localisation info are already registered in the recorded)
				if (spread < 20) {
					position = (0@0@0).rrand(2@2@2);
				} {
					position = (0@0@0).rrand(10@10@4);
				};
			};

			// register to memory
			memory[sequence["title"]]["position"] = position;

			// [sequence["typeOfShot"], position, memory[sequence["title"]]["position"]].postln; 

			source = SSG_FixedSound(
				object, // sonicObject
				Pseq(timesOfOccurence,1),  // pWait
				sequence["size"],         // size
				sequence["relativeAmplitude"], // amplitude
				position, // position
				sequence["title"].asSymbol,
				sequence["description"],
			);

		} { // else it has to be ambulatory
			
			// NOTE: it doesn't matter if trajectories exceed the limits of the sonicSpace, since no filtering/acoustic opacity takes place  

			// NOTE: currently Ambulatory Sounds do not consult the memory module
			var trajectories; 

			// calculate trajectories
			case 
			{sequence["acceleration"]=="absolute"}
			{
				var positionA;
				var positionB;
				var timeToEnd;

				// find two random positions within sonicSpace (taking into acount rangeOfMovement) 				
				positionA = zonePosition.rrand(zoneBounds + zonePosition); 
				positionB = (
					positionA + (sequence["rangeOfMovement"].asCartesian / 2)
				).rrand( 
					sequence["rangeOfMovement"].asCartesian + positionA
				); 
								
				timeToEnd = positionA.dist(positionB).abs / 
				sequence["speedOfMovement"];
				
				// trajectories
				trajectories = [
					Env([positionA.x,positionB.x],[timeToEnd]),
					Env([positionA.y,positionB.y],[timeToEnd]),
					Env([positionA.z,positionB.z],[timeToEnd])
				];

			}
			{sequence["acceleration"]=="angular"}
			{
				var positions;
				var distance; 
				var timeToEnd;

				// find two random positions within sonicSpace (taking into acount rangeOfMovement) 
				positions = [zonePosition.rrand(zoneBounds + zonePosition)]; 
				rrand(3,8).do{
					positions.add(
						positions[0].rrand(sequence["rangeOfMovement"]
							.asCartesian + positions[0])
					);
				};	

				// calculate total distance covered
				distance = 0;
				positions.do{arg item, index; 
					if (index>0) {
						distance = distance + item.dist(positions[index-1]);
					}
				};

				// calculate how much time is needed to be covered
				timeToEnd = (distance / sequence["speedOfMovement"])!(positions.size-1);
				
				trajectories = [ 
					Env(positions.collect({arg item; item.x}),timeToEnd),
					Env(positions.collect({arg item; item.y}),timeToEnd),
					Env(positions.collect({arg item; item.z}),timeToEnd)
				];

			}
			{sequence["acceleration"]=="coriolis"}
			{
				var positions;
				var distance; 
				var timeToEnd;
				var curves; 
				
				// find two random positions within sonicSpace (taking into acount rangeOfMovement) 
				positions = [zonePosition.rrand(zoneBounds + zonePosition)]; 
				rrand(3,8).do{
					positions.add(
						positions[0].rrand(sequence["rangeOfMovement"]
							.asCartesian + positions[0])
					);
				};	
				
				// calculate total distance covered
				distance = 0;
				positions.do{arg item, index; 
					if (index>0) {
						distance = distance + item.dist(positions[index-1]);
					}
				};
				
				// calculate how much time is needed to be covered
				timeToEnd = (distance / sequence["speedOfMovement"])!(positions.size-1);
				
				// accelerated speed is achieved with random curves
				curves = Array.fill(3, {
					Array.fill(positions.size, { 10.rand2 })
				});
				
				trajectories = [ 
					Env(positions.collect({arg item; item.x}),timeToEnd, 
						curves[0]),
					Env(positions.collect({arg item; item.y}),timeToEnd, 
						curves[1]),
					Env(positions.collect({arg item; item.z}),timeToEnd, 
						curves[2])
				];
			};

			[sequence["acceleration"], trajectories];

			source = SSG_AmbulatorySoundEnv(
				object, // filePath
				Pseq(timesOfOccurence,1),  // pWait
				sequence["size"],         // size
				sequence["relativeAmplitude"], // amplitude
				trajectories,         // trajectories
				sequence["title"].asSymbol,
				sequence["description"],
			);
		};

		// add source
		sonicSpace.addSoundSource( source ); // add source

	}

}

