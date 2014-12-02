// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project


// ===================== Abstract Listener  =========================



SSG_AbstractListener {

	classvar < group;
	classvar counter = 0;   // (Integer) used for the unique ids 
	
	var < position;         // (CtkControl) the bus holding the position
	var < listeningRadius;  // (Number) the distance in meters over which listening occurs (inf is allowed)
	var < id;               // (Integer) a unique id
	var < label;            // (Symbol) a user defined label
	var < info;             // (String) user-defined custom text

	// ===================== new/init  =========================
	
	*initClass{

		Class.initClassTree(CtkGroup);
		
		// group
		group = CtkGroup.new(0, server:Server.default);
	}

	*new {
		
		arg
		listeningRadius = inf,     // (Number) the distance in meters over which listening occurs (inf is allowed)
		label = \no,              // (Symbol) a custom name for the object
		info = "";                // (String) a custom info-text for object

		^super.new.pr_init(listeningRadius,label,info);
	}

	pr_init{
		arg listeningRadius_, label_, info_;

		// type checking arguments
		case
		{listeningRadius_.isKindOf(Number).not} { Error("SSG_AbstractListener: 'listeningRadius' argument should be an instance of Number").throw }
		{label_.class != Symbol} { Error("SSG_AbstractListener: 'label' argument should be an instance of Symbol").throw }
		{info_.class != String} { Error("SSG_AbstractListener: 'info' argument should be an instance of String").throw };
		
		// init member variables
		listeningRadius = listeningRadius_;
		label = label_;
		info = info_;

		// position bus
		position = CtkControl(3,[0@0@0]);
				
		// generate and assign a unique id
		counter = counter + 1;
		id = counter;
	}

	// ===================== setters  =========================

	setLabel {

		arg label_; 
		
		// type-check argument and assing if ok
		if (label_.class != Symbol) {
			Error("SSG_AbstractListener \(id:" ++ " " ++ id ++ "\): Error, argument was not an instance of Symbol.").throw;
		} {
			label = label_;
			("SSG_AbstractListener \(id:" ++ " " ++ id ++ "\): label has been succesfully set to:" + label ++ ".").postln;
		}

		^this;
	}
	
	setInfo {
		
		arg info_; 
		
		// type-check argument and assing if ok
		if (info_.class != String) {
			Error("SSG_AbstractListener \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of String.").throw;
		} {
			info = info_;
			("SSG_AbstractListener \(id:" ++ " " ++ id ++ "\): Info has been succesfully set to:" + info).postln;
		}

		^this;
	}

	setListeningRadius {
		
		arg radius_; 
		
		// type-check argument and assing if ok
		if (radius_.isKindOf(Number).not) {
			Error("SSG_AbstractListener \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of Number.").throw;
		} {
			listeningRadius = radius_;
			("SSG_AbstractListener \(id:" ++ " " ++ id ++ "\): ListeningRadius has been succesfully set to:" + listeningRadius).postln;
		}

		^this;
	}

	// ===================== play  =========================

	// all subclasses should have a play method
	play {
		this.subclassResponsibility(thisMethod);
	}

	// all subclasses should have a exportScore method
	exportScore {
		this.subclassResponsibility(thisMethod);
	}

}




// ===================== FixedListener  =========================


SSG_FixedListener : SSG_AbstractListener {

	*new{
		arg
		position = 0@0@0,         // (Cartesian) the fixed position
		listeningRadius = inf,    // (Number) the distance in meters over which listening occurs (inf is allowed)
		label = \no,              // (Symbol) a custom name for the object
		info = "";                // (String) a custom info-text for object

		^super.new(listeningRadius,label,info).pr_sub_init(position);
	}

	pr_sub_init{ arg position_;
		
		// type check
		if (position_.isKindOf(Cartesian).not)
		{ Error("SSG_FixedListener: 'position' should be an instance of Cartesian.").throw };
		
		position.set(position_.asArray);
	}

	play{ 
		Server.default.waitForBoot({
			position.play
		});
			^this;
	}

	exportScore{ 
		var score = CtkScore.new; 
		score.add(position);
		^score;
	}


	setNewPosition {
		arg position_;
		
		// type check
		if (position_.isKindOf(Cartesian).not)
		{ Error("SSG_FixedListener: 'position' should be an instance of Cartesian.").throw };
		
		position.set(position_.asArray.postln);
		^this;
	}

}

// SSG_AmbulatoryListenerEnv

SSG_AmbulatoryListenerEnv : SSG_AbstractListener {

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

	*new{
		arg trajectories = Env.newClear(8)!3,  // (Array of Envelopes) an Envelope defining the trajectory of the Object
		listeningRadius = inf,    // (Number) the distance in meters over which listening occurs (inf is allowed)
		label = \no,              // (Symbol) a custom name for the object
		info = "";                // (String) a custom info-text for object

		^super.new(listeningRadius,label,info)
		.pr_sub_sub_init(trajectories);
	}

	pr_sub_sub_init{
		arg trajectories_;

	// type check
		if (trajectories_.isKindOf(Array).not)
		{ Error("SSG_AmbulatoryListenerEnv: 'trajectories' should be an instance of Array containing 3 instances of Env.").throw };

		if (trajectories_.size!=3)
		{ Error("SSG_AmbulatoryListenerEnv: 'trajectories' should be an instance of Array containing 3 instances of Env.").throw };

		if (trajectories_.every(_.isKindOf(Env).not))
		{ Error("SSG_AmbulatoryListenerEnv: 'trajectories' should be an instance of Array containing 3 instances of Env.").throw };

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
			position.play;
			trajectorySynth = synthDefs[\envTrajectory].note(
				target: SSG_AbstractListener.group,
				addAction:\addToTail
			).out_(position).envX_(envelopeX).envY_(envelopeY)
			.envZ_(envelopeZ).play;
		});
		
		^this;
	}

	stop { // overload stop
		super.stop;
		trajectorySynth.free; // release trajectorySynth
	}

	exportScore { // overload export score

		var score = CtkScore.new; 

		var trajectorySynth = synthDefs[\envTrajectory].note(
			target: SSG_AbstractListener.group,
			addAction:\addToTail
		).out_(position).envX_(envelopeX).envY_(envelopeY)
		.envZ_(envelopeZ);

		score.add(position);
		score.add(trajectorySynth);
		^score;
	}

}

// SSG_AmbulatoryListenerFunc

// SSG_RandomWalker
