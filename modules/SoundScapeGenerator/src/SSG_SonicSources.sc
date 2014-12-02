// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// SonicSources are abstractions of sonic sources. Internally they may be synthesized by an arbitrary bumber of audio files (wrapped as SSG_Verticis) that are sequenced using Streams of arbitrarily complexity

// SonicAtmosphere is an abstraction for an ever-playing background 'atmosphere' specific to a soundscape. Note that in the synthDefs looping is on because of the particular nature of atmospheres, we typically want no gaps

// ========================= Abstract Sonic Source =======================

SSG_AbstractSonicSource {

	// ========================= static variables  =======================
	classvar < group;     // (CtkGroup) a group for all synths specific to the class
	classvar counter = 0;  // (Integer) global counter used to created unique IDs

	// ==================== public instance variables ===================
	var < size;         // (Number) the relative size of the sound source
	var < position;     // (CtkControl) the current position of the sound in a x,y,z Cartesian format
	var < relativeAmplitude; // (Number) the relative amplitud of the sound in question
	var < id;           // (Integer)
	var < label;        // (Symbol)
	var < info;         // (String)
	var < donePlaying;     // (Boolean) if the source has finished playing
	var < donePlayingCond; // (Condition) if the source has finished playing

	// ==================== private instance variables ===================
	var sequence;     // (SSG_Sequence) the sequence to be played back
	var bus;          // (CtkAudio) the audio to be spatialized
	var pWait;        // (Stream) the wait pattern (for scheduling the sonic event) IMPORTANT -> pWait is the wait pattern between the END of an event and the start of the subsequent (and not between two subsequent starts)
	var playSynth;        // (CtkNote) the spatializing synth
	var playRoutine;      // (Task) the play-back routine

	var doneFunc;        // (Function) to be evaluated when done playing or on stop

	// ========================= initClass =======================

	*initClass{

		Class.initClassTree(SSG_Sequence);
		Class.initClassTree(CtkGroup);

		// group
		group = CtkGroup.new(0, server:Server.default);
	}

	// ========================= constructor =======================

	*new {

		arg                // expected arguments:
		sequence = nil,    // (SSG_Sequence) the sequence to trigger/localize
		pWait = 0,       // (ListPattern/Number) waitTime before first apperance and between each individual repetitions
		size = 0.1,          // (Number) the size of the sonic source
		relativeAmplitude = 0.5, // (Number) the relative amplitude of the Source
		label = \untitled, // (Symbol) a custom name for the object
		info = "";         // (String) a custom info-text for object

		^super.new.pr_init(sequence, pWait, size, relativeAmplitude, label, info);
	}

	pr_init{ arg sequence_, pWait_, size_, relativeAmplitude_, label_, info_;

		// type-check arguments
		case
		{sequence_.isKindOf(SSG_Sequence).not && sequence_.isKindOf(SSG_AbstractSonicObject).not}
		{ Error("SSG_AbstractSonicSource: 'sequence' should be an instance of SSG_Sequence or of SSG_AbstractSonicObject.").throw }

		// { // this a recursive clause
		// 	if (pWait_.isKindOf(ListPattern)) {
		// 		pWait_.list.deepCollect(inf, { arg item;
		// 			var recursiveFunc = thisFunction;
		// 			if (item.isKindOf(ListPattern)) {
		// 				item.list.deepCollect(inf, recursiveFunc);
		// 			} {
		// 				if ( item.isKindOf(Number)) {
		// 					true
		// 				} {
		// 					false
		// 				}
		// 			}
		// 		}).flatten(inf).any(_.not); // first flatten then test
		// 	} {
		// 		if (pWait_.isKindOf(Number)) {
		// 			false
		// 		} {
		// 			true
		// 		}
		// 	}
		// }
		{
			pWait_.isKindOf(Pattern).not && pWait_.isKindOf(Number).not;
		}
		{ Error("SSG_AbstractSonicSource: 'pWait' should be an instance of Number or of Pattern with a list comprised of only instances of Number or of other ListPattern objects that recursively respect this rule.").throw }

		{size_.isKindOf(Number).not}
		{ Error("SSG_AbstractSonicEvent: 'size' should be an instance of Number.").throw }

		{relativeAmplitude_.isKindOf(Number).not}
		{ Error("SSG_AbstractSonicEvent: 'relativeAmplitude' should be an instance of Number.").throw }

		{label_.class != Symbol} { Error("SSG_AbstractSonicSource: 'label' argument should be an instance of Symbol").throw }
		{info_.class != String} { Error("SSG_AbstractSonicSource: 'info' argument should be an instance of String").throw };


		// init instance variables

		// if an SSG_AbstractSonicObject is given instead of a SSG_Sequence then create a SSG_Sequence out of it
		case
		{ sequence_.isKindOf(SSG_Sequence)}
		{ sequence = sequence_;}
		{ sequence_.isKindOf(SSG_AbstractSonicObject)}
		{ sequence = SSG_Sequence(sequence_, sequence_.duration) };

		label = label_;
		info = info_;
		size = size_;
		relativeAmplitude = relativeAmplitude_;

		pWait = Pseq([pWait_],1).asStream;      // encapsulate in a Pseq to avoid having a naked Number converted to Stream (this would cause the do method to behave in different than the desirable way)

		position = CtkControl(3,[0,0,0]); // initialize position to 0

		donePlaying = true;
		donePlayingCond = Condition(true);

		// generate and assign a unique id
		counter = counter + 1;
		id = counter;
	}

	// ========================= RT methods =====================

	play { arg outBus = 0, doneAction={};

		// bus
		if ((outBus.isKindOf(CtkAudio)
			|| outBus.isKindOf(Bus)
			|| outBus.isKindOf(Integer) ).not) {
				// type check
				Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): argument 'bus' should be an instance of CtkAudio or of Integer (denoting a bus)").throw;
			};

		doneFunc = doneAction;

		// if already playing then stop
		if (playRoutine.isPlaying) {
			playRoutine.stop;
			playRoutine.reset;
		};
		playSynth.free;

		donePlaying = false;
		donePlayingCond.test_(false);
		donePlayingCond.signal;

		Server.default.waitForBoot({ // boot server just in case..

			// make sure group is playing
			group.play;

			// play position
			position.play;

			playRoutine = Task {
				pWait.do{ arg time;
					time.postln.wait;  // waitTime
					sequence.play(outBus,group,relativeAmplitude); // play-back audio
					sequence.donePlayingCond.wait;
					
					// sequence.stop;
					// sequence.reset;
				};


				fork { // fork else nothing after playRoutine.stop would be executed
					this.stop;
					this.reset;
					// playRoutine.stop;
					// playRoutine.reset;
					// donePlaying = true;
					// donePlayingCond.test_(true);
					// donePlayingCond.signal;
					// doneFunc.value;
					// doneFunc = {}; // so that if doneFunc has been called once it won't be called again if stop is called
				};
			};
			playRoutine.play;
		});
	}

	stop {
		sequence.stop;
		playRoutine.stop;
		donePlaying = true;
		donePlayingCond.test_(true);
		donePlayingCond.signal;
		doneFunc.value; // call doneFunc
	}

	reset {
		sequence.reset;
		playRoutine.reset;
		pWait.reset;
	}

	// ========================= NRT methods =====================

	exportScore { arg timeOffset = 0, maxDuration = 600, outBus = 0;

		var score =  CtkScore.new; // the final score
		var time = timeOffset;     // the time of the next note event
		var array;     // array of Scores - DEBUGGING

		// bus
		if ((outBus.isKindOf(CtkAudio)
			|| outBus.isKindOf(Bus)
			|| outBus.isKindOf(Integer) ).not) {
				// type check
				Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): argument 'bus' should be an instance of CtkAudio or of Integer (denoting a bus)").throw;
			};

		this.stop;  // stop and
		this.reset; // reset just in case;

		block { arg break;
			pWait.do{ arg duration;

				var seqScore;   // a score for each individual sequence's appearence

				// update time
				time = time + duration;

				// score for each sequence's appearence
				seqScore = sequence.exportScore(time, maxDuration, outBus, SSG_AbstractSonicSource.group,relativeAmplitude);

				// score.add(seqScore);
				score.merge(seqScore); // merge score

				// update time with the duration of the seqScore
				// it relies on CtkScore.lastNoteEndTime which is an added method

				time = time + seqScore.lastNoteEndTime;

				if (time > maxDuration) {
					break.value();
				};

				// label.postln;

				// debbugging
				// array = array.add(seqScore);
			}
		};

		// add position to score
		score.add(position);

		// reset (just in case someones wants to play it again)
		this.reset;

		^score; // return score
		// ^array;
	}

	// ========================= Setters  =====================

	setSequence {

		arg sequence_;

		if ((sequence_.isKindOf(SSG_Sequence) || sequence_.isKindOf(SSG_AbstractSonicObject)).not)
		{ Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): 'sequence' should be an instance of SSG_Sequence or of SSG_AbstractSonicObject.").throw };

		case
		{ sequence_.isKindOf(SSG_Sequence)}
		{ sequence = sequence_;}
		{ sequence_.isKindOf(SSG_AbstractSonicObject)}
		{ sequence = SSG_Sequence(sequence_, sequence_.duration) };

		this.stop;
		this.reset;

		^this;
	}


	setLabel {

		arg label_;

		// type-check argument and assing if ok
		if (label_.class != Symbol) {
			Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): Error, argument was not an instance of Symbol.").throw;
		} {
			label = label_;
			("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): label has been succesfully set to:" + label ++ ".").postln;
		}

		^this;
	}

	setInfo {

		arg info_;

		// type-check argument and assing if ok
		if (info_.class != String) {
			Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of String.").throw;
		} {
			info = info_;
			("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): Info has been succesfully set to:" + info).postln;
		}

		^this;

	}

}


// ========================= SONIC ATMOSPHERE =======================

SSG_SonicAtmosphere : SSG_AbstractSonicSource {


	// ===================== new/init =========================

	*new {

		arg              // expected arguments:
		sequence = nil,    // (SSG_Sequence/SSG_AbstractSonicObject) the sequence to trigger/localize
		relativeAmplitude = 0.2,  // (Number) the relative amplitude of the object
		label = \untitled, // (Symbol) a custom name for the object
		info = "";         // (String) a custom info-text for object

		^super.new(sequence, Pseq([0],inf), 0, relativeAmplitude, label, info).pr_sub_init;
	}

	pr_sub_init{
		// pWait = Pseq([0],inf).asStream;
	}

	position{ // overload position getter since there is no position
		^nil
	}

	size { // overload size getter since there is no position
		^nil
	}

}


SSG_FixedSound : SSG_AbstractSonicSource {

	*new {
		arg                // expected arguments:
		sequence = nil,    // (SSG_Sequence/SSG_AbstractSonicObject) the sequence to trigger/localize

		pWait = 0,       // (ListPattern/Number) waitTime before first apperance and between each individual repetitions
		size = 0,          // (Number) the size of the sonic source
		relativeAmplitude = 0.5,  // (Number) the relative amplitude of the object
		position = 0@0@0,  // (Cartesian) the fixed position of the sonic source in meters
		label = \untitled, // (Symbol) a custom name for the object
		info = "";         // (String) a custom info-text for object

		^super.new(sequence,pWait, size, relativeAmplitude, label,info)
		.pr_sub_sub_init(position);
	}

	pr_sub_sub_init { arg position_;

		// type check
		if (position_.isKindOf(Cartesian).not)
		{ Error("SSG_FixedSound: 'position' should be an instance of Cartesian.").throw };

		position.set(position_.asArray);
	}

}


SSG_AmbulatorySoundEnv : SSG_AbstractSonicSource {

	// ========================= static variables  =======================


	// classvar < group;     // (CtkGroup) a group for all synths specific to the class
	classvar synthDefs;    // (CtkProtoNotes) synthDefs specific to the class


	// ================== private instance variables  ==================

	var envelopeX, envelopeY, envelopeZ; // (Evn) the trajectory envelopes
	var trajectorySynth;                 // (CtkNote) the trajectory synth

	*initClass {

		Class.initClassTree(OSCresponder);
		Class.initClassTree(SynthDescLib);
		Class.initClassTree(SynthDef);
		Class.initClassTree(CtkProtoNotes);
		Class.initClassTree(CtkGroup);
		Class.initClassTree(Env);
		Class.initClassTree(Control);
		Class.initClassTree(EnvGen);
		Class.initClassTree(SSG_AbstractSonicSource);

		// synthDefs
		synthDefs = CtkProtoNotes(
			SynthDef(\envTrajectory, { arg out;
				var signal, envX, envY, envZ;
				envX = Control.names([\envX]).kr(Env.newClear(8));
				envY = Control.names([\envY]).kr(Env.newClear(8));
				envZ = Control.names([\envZ]).kr(Env.newClear(8));
				envX = EnvGen.kr(envX);
				envY = EnvGen.kr(envY);
				envZ = EnvGen.kr(envZ);
				signal = [envX, envY, envZ];
				Out.kr(out, signal);
			})
		);
	}

	*new {
		arg                // expected arguments:
		sequence = nil,    // (SSG_Sequence/SSG_AbstractSonicObject) the sequence to trigger/localize
		pWait = 0,       // (ListPattern/Number) waitTime before first apperance and between each individual repetitions
		size = 0,          // (Number) the size of the sonic source
		relativeAmplitude = 0.5,  // (Number) the relative amplitude of the object
		trajectories = Env.newClear(8)!3,  // (Array of Envelopes) an Envelope defining the trajectory of the Object
		label = \untitled, // (Symbol) a custom name for the object
		info = "";         // (String) a custom info-text for object

		^super.new(sequence,pWait, size, relativeAmplitude, label,info)
		.pr_sub_sub_init(trajectories);
	}

	pr_sub_sub_init { arg trajectories_;

		// type check
		if (trajectories_.isKindOf(Array).not)
		{ Error("SSG_AmbulatorySoundEnv: 'trajectories' should be an instance of Array containing 3 instances of Env.").throw };

		if (trajectories_.size!=3)
		{ Error("SSG_AmbulatorySoundEnv: 'trajectories' should be an instance of Array containing 3 instances of Env.").throw };

		if (trajectories_.every(_.isKindOf(Env).not))
		{ Error("SSG_AmbulatorySoundEnv: 'trajectories' should be an instance of Array containing 3 instances of Env.").throw };

		envelopeX = trajectories_[0];
		envelopeY = trajectories_[1];
		envelopeZ = trajectories_[2];
	}

	play { arg outBus=0, doneAction={};

		var newDoneAction = {
			doneAction.value;
			trajectorySynth.free;
		};

		Server.default.waitForBoot({

			super.play(outBus, newDoneAction); // call super.play

			SSG_AbstractSonicSource.group.play; // we use the super super group for this
			position.play; // play Bus

			// play trajectorySynth
			trajectorySynth = synthDefs[\envTrajectory].note(
				target: SSG_AbstractSonicSource.group,
				addAction:\addToTail
			).out_(position)
			.envX_(envelopeX).envY_(envelopeY)
			.envZ_(envelopeZ).play;

		});

	}

	stop { // overload stop
		super.stop;
		trajectorySynth.free; // release trajectorySynth
	}

	exportScore { // overload export score

		arg timeOffset = 0, maxDuration = 600, outBus = 0;

		// trajectory Synth
		var trajectorySynth = synthDefs[\envTrajectory].note(
			target: SSG_AbstractSonicSource.group,
			addAction:\addToTail
		).out_(position)
		.envX_(envelopeX).envY_(envelopeY)
		.envZ_(envelopeZ);

		var score = super.exportScore(timeOffset, maxDuration, outBus); // call super.exportScore

		score.add(trajectorySynth); // add trajectorySynth to score
		^score;  // return score
	}

}



SSG_AmbulatorySoundFunc : SSG_AbstractSonicSource {

	classvar synthDefs;


	var trajectoryFunc;   // (Function) the callback Function for the trajectory
	var routine;          // (Routine) the routine scheduling the Function
	var funcWaitTime;     // (Number) the wait time between successive function calls

	// var controlSynth;     // (CtkNote) to update position in NRT

	*initClass {

		Class.initClassTree(OSCresponder);
		Class.initClassTree(SynthDescLib);
		Class.initClassTree(SynthDef);
		Class.initClassTree(CtkProtoNotes);
		Class.initClassTree(CtkGroup);
		Class.initClassTree(Env);
		Class.initClassTree(Control);
		Class.initClassTree(EnvGen);
		Class.initClassTree(SSG_AbstractSonicSource);

		// synthDefs
		synthDefs = CtkProtoNotes(
			SynthDef(\funcControl, { arg out, x, y, z;
				var signal = [x,y,z];
				Out.kr(out, signal);
			})
		);
	}

	*new {
		arg                // expected arguments:
		sequence = nil,    // (SSG_Sequence/SSG_AbstractSonicObject) the sequence to trigger/localize
		pWait = 0,       // (ListPattern/Number) waitTime before first apperance and between each individual repetitions
		size = 0,          // (Number) the size of the sonic source
		relativeAmplitude = 0.5,  // (Number) the relative amplitude of the object
		trajectoryFunc = {0@0@0},  // (Function) returning an instance of Cartesian and definining the positioning of the SonicSource at every given time
		funcWaitTime = 0.1,    // (Number) the wait time between successive function calls
		label = \untitled, // (Symbol) a custom name for the object
		info = "";         // (String) a custom info-text for object

		^super.new(sequence, pWait, size, label,info)
		.pr_sub_init(trajectoryFunc, funcWaitTime);
	}

	pr_sub_init { arg trajectoryFunc_, funcWaitTime_;

		// type check
		if (trajectoryFunc_.isKindOf(Function).not)
		{ Error("SSG_AmbulatorySoundFunc: 'trajectoryFunc' should be an instance of Function returning a Cartesian.").throw };

		if (funcWaitTime_.isKindOf(Number).not)
		{ Error("SSG_AmbulatorySoundFunc: 'funcWaitTime' should be an instance of Number.").throw };

		trajectoryFunc = trajectoryFunc_;
		funcWaitTime = funcWaitTime_;
	}

	play { arg outBus, doneAction={};

		Server.default.waitForBoot({

			super.play(outBus, doneAction); // call super.play

			SSG_AbstractSonicSource.group.play; // we use the super super group for this
			position.play; // play Bus

			// evaluate Func and update position
			routine = fork{
				loop{
					var coordinates = trajectoryFunc.value;
					position.set(coordinates.asArray);
					funcWaitTime.wait;
				}
			}
		});

	}

	exportScore { arg timeOffset = 0, maxDuration = 600, outBus = 0;

		var score = super.exportScore(timeOffset, maxDuration, outBus); // call super.exportScore

		var endTime = score.lastNoteEndTime;
		var time = 0;

		var coordinates = trajectoryFunc.value;  // spatial coordinates

		var controlSynth = synthDefs[\funcControl].note(0,endTime,
				target: SSG_AbstractSonicSource.group,
				addAction:\addToTail
			).out_(position).x_(coordinates.x).y_(coordinates.y)
			.z_(coordinates.z);

		score.add(controlSynth); // add controlSynth to score

		while { time < endTime }  // while there is timeLeft
		{
			// update time
			time = time + funcWaitTime;

			coordinates = trajectoryFunc.value; // calculate new coordinates
			// update controlSynth
			controlSynth.x_(coordinates.x,time).y_(coordinates.y,time)
			.z_(coordinates.z,time);
		};

		^score;  // return score
	}

	stop { // overload stop
		super.stop;
		routine.stop; // release trajectorySynth
	}

}


SSG_SoundCloud  : SSG_AbstractSonicSource {

	// !!!! SSG_SoundCloud is in essence a container of several SSG_AmbulatorySoundFunc objects and not a true SSG_AbstractSonicSource, it inherints from the latter only to implement subtype polymorphism, therefore it MUST overload all its methods.

	var corner;            // (Cartesian) the positioning of the down/left/front corner of the cloud
	var bounds;            // (Cartesian) the spatial boundaries of the cloud
	var density;           // (Integer) how many overlapping events are allowed at a given time

	var elements;          // (Array of AmbulatorySoundFunc) several of AmbulatorySoundFunc objects whose position is modulated accordingly

	*new {
		arg                // expected arguments:
		sequence = nil,    // (SSG_Sequence/SSG_AbstractSonicObject) the sequence to trigger/localize
		pWait = 0,       // (ListPattern/Number) waitTime before first apperance and between each individual repetitions
		size = 0,          // (Number) the size of the sonic source
		relativeAmplitude = 0.5,  // (Number) the relative amplitude of the object
		position = 0@0@0,  // (Cartesian) the positioning of event (left/down/front corner )
		bounds = 10@10@10, // (Cartesian) the size of the cloud (x,y,z)
		density = 1,       // (Integer) how many overlapping events are allowed at a given time
		label = \untitled, // (Symbol) a custom name for the object
		info = "";         // (String) a custom info-text for object

		^super.new(sequence, pWait, size, relativeAmplitude, label,info)
		.pr_sub_init(position, bounds, density, pWait); // we need to re-implement pWait's calculation to allow for temporal variations
	}

	pr_sub_init { arg position_, bounds_, density_, pWait_;

		// type check
		if (position_.isKindOf(Cartesian).not)
		{ Error("SSG_SoundCloud: 'position' should be an instance of Cartesian.").throw };

		if (bounds_.isKindOf(Cartesian).not)
		{ Error("SSG_SoundCloud: 'bounds' should be an instance of Cartesian.").throw };

		if (density_.isKindOf(Integer).not)
		{ Error("SSG_SoundCloud: 'density' should be an instance of Integer.").throw };

		corner = position_;
		bounds = bounds_;
		density = density_;

		elements = Array.fill(density,{ arg i;

			var pWait =   // pWait_ has been already type-checked in super.new
			// modulate its values slightly
			Plazy({pWait_ * Pbrown(0.98,1.02,0,pWait_.repeats)});

			SSG_AmbulatorySoundFunc(
				sequence,
				pWait,
				size,
				{   // random coordinates within the cubic area
					var coordinates = Cartesian(
						bounds.x.rand + corner.x, // random x
						bounds.y.rand + corner.y, // random y
						bounds.z.rand + corner.z  // random z
					);
					coordinates;
				},
				rrand(0.1,0.4), // random waitTime (for now)
				(label++\_soundCloudElementNo++i.asSymbol).asSymbol,
				info++"----spawned by SoundCloud."
			);
		});
	}

	play { arg outBus, doneAction={};

		Server.default.waitForBoot({
			elements.do{arg item; item.play};
		});
	}

	exportScore{arg timeOffset = 0, maxDuration = 600, outBus = 0;
		var score = CtkScore.new;
		elements.do{arg item;
			var elementScore = item.exportScore(timeOffset, maxDuration,
				outBus);
			score.merge(elementScore);
		};
		^score;
	}

	stop { // overload stop
		elements.do{arg item; item.stop};
	}

	reset {
		elements.do{arg item; item.reset};
	}

	setLabel {

		arg label_;

		// type-check argument and assing if ok
		if (label_.class != Symbol) {
			Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): Error, argument was not an instance of Symbol.").throw;
		} {
			label = label_;
			("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): label has been succesfully set to:" + label ++ ".").postln;
		}

		^this;
	}

	setInfo {

		arg info_;

		// type-check argument and assing if ok
		if (info_.class != String) {
			Error("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of String.").throw;
		} {
			info = info_;
			("SSG_AbstractSonicSource \(id:" ++ " " ++ id ++ "\): Info has been succesfully set to:" + info).postln;
		}

		^this;
	}

	setSequence {

		arg sequence_;

		if ((sequence_.isKindOf(SSG_Sequence) || sequence_.isKindOf(SSG_AbstractSonicObject)).not)
		{ Error("SSG_SonicCloud \(id:" ++ " " ++ id ++ "\): 'sequence' should be an instance of SSG_Sequence or of SSG_AbstractSonicObject.").throw };

		case
		{ sequence_.isKindOf(SSG_Sequence)}
		{ sequence = sequence_;}
		{ sequence_.isKindOf(SSG_AbstractSonicObject)}
		{ sequence = SSG_Sequence(sequence_, sequence_.duration) };

		this.stop;
		this.reset;

		^this;
	}

	position {
		^elements.collect(_.position); // return an array of the positions
	}

}


