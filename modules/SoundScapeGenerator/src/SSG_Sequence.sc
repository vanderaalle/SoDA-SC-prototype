// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// SSG_Sequence

SSG_Sequence {

	// ============= class variables =========================
	classvar counter = 0; // global counter used to created unique IDs

	classvar encoderMono;       // (FoaEncoderMatrix) ambisonics encoder for mono signals
	classvar encoderStereo;      // (FoaEncoderMatrix) ambisonics encoder for stereo signals
	classvar synthDefs;         // (CtkProtoNotes) holds all the SynthDefs for playback
	classvar decoder;           // (FoaDecoderMatrix) used for tests only


	// ============= public instance variables ================
	var < id;         // (Integer) a unique id
	var < label;      // (Symbol) a user-defined custom label
	var < info;       // (String) additional user-defined information
	var < fadeTime;   // (Number) cross-fade time between successive Vertices
	var < donePlaying; // (Boolean) if the source has finished playing
	var < donePlayingCond; // (Condition) if the source has finished playing

	// var < durationOfLastGeneratedScore; // (Number) an ugly hack that should be replaced with a proper -duration method in CtkScore

	// ============= private instance variables ================
	var pSonicObject;      // (ListPattern/SSG_AbstractSonicObject/Nil) the ListPattern with the Vertices
	var pDur;         // (ListPattern/Number) the ListPattern with the Durations
	var playRoutine;      // (Task) the play-back routine
	var playSynth;        // (CtkNote) the currently playing-back synth

	// ===================== initClass ========================

	*initClass {

		// init dependent classes
		Class.initClassTree(OSCresponder);
		Class.initClassTree(SynthDescLib);
		Class.initClassTree(SynthDef);
		Class.initClassTree(CtkProtoNotes);
		//Class.initClassTree(CtkGroup);
		Class.initClassTree(FoaEncoderMatrix);

		// init encoders
		encoderMono = FoaEncoderMatrix.newOmni;  // encode to an omniDirectional ever present signal
		encoderStereo = FoaEncoderMatrix.newStereo;
		encoderStereo = FoaEncoderMatrix.newStereo;

		// decoder (for tests only)
		decoder = FoaDecoderMatrix.newQuad();

		// init synthDefs
		synthDefs = CtkProtoNotes(
			SynthDef(\monoDisk, {
				arg out = 0, fadeTime = 1, amplitude = 1, gate = 1, buffer,
				loop=0, amp = 1;
				var signal, env, envgen;
				env = Env.asr(fadeTime,1,fadeTime,1);
				envgen = EnvGen.ar(env, gate, doneAction:2);
				/// envgen = EnvGen.ar(env, gate);
				signal = VDiskIn.ar(1, buffer, BufRateScale.kr(buffer),
					loop:loop);
				signal = signal * envgen * amplitude;
				signal = LeakDC.ar(signal); // remove DC
				signal = signal * amp;
				signal = FoaEncode.ar(signal, encoderMono); // encode
				Out.ar(out, signal);
			}),
			SynthDef(\monoRAM, {
				arg out = 0, fadeTime = 1, amplitude = 1, gate = 1, buffer,
				loop=0, amp=1;
				var signal, env, envgen;
				env = Env.asr(fadeTime,1,fadeTime, 1);
				envgen = EnvGen.ar(env, gate, doneAction:2);
				// envgen = EnvGen.ar(env, gate);
				signal = PlayBuf.ar(1, buffer, BufRateScale.kr(buffer),
					loop:loop);
				signal = signal * envgen * amplitude;
				signal = LeakDC.ar(signal); // remove DC
				signal = signal * amp;
				signal = FoaEncode.ar(signal, encoderMono); // encode
				// signal = FoaDecode.ar(signal, decoder);
				Out.ar(out, signal);
			}),
			SynthDef(\stereoDisk, {
				arg out = 0, fadeTime = 1, amplitude = 1, gate = 1, buffer,
				loop=0, amp=1;
				var signal, env, envgen;
				env = Env.asr(fadeTime,1,fadeTime, 1);
				envgen = EnvGen.ar(env, gate, doneAction:2);
				// envgen = EnvGen.ar(env, gate);
				signal = VDiskIn.ar(2, buffer, BufRateScale.kr(buffer),
					loop:loop);
				signal = signal * envgen * amplitude;
				signal = LeakDC.ar(signal); // remove DC
				signal = signal * amp;
				signal = FoaEncode.ar(signal, encoderStereo);
				Out.ar(out, signal);
			}),
			SynthDef(\stereoRAM, {
				arg out = 0, fadeTime = 1, amplitude = 1, gate = 1, buffer,
				loop=0, amp=1;
				var signal, env, envgen;
				env = Env.asr(fadeTime,1,fadeTime, 1);
				envgen = EnvGen.ar(env, gate, doneAction:2);
				// envgen = EnvGen.ar(env, gate);
				signal = PlayBuf.ar(2, buffer, BufRateScale.kr(buffer),
					loop:loop);
				signal = signal * envgen * amplitude;
				signal = LeakDC.ar(signal); // remove DC
				signal = signal * amp;
				signal = FoaEncode.ar(signal, encoderStereo);
				Out.ar(out, signal);
			})
		);
	}


	// ========================== new/init =====================


	*new{

		arg              // expected arguments:
		pSonicObject = nil,   // (ListPattern/SSG_AbstractSonicObject/Nil) vertex pattern
		pDur = 1,        // (ListPattern/Number) duration pattern (in seconds)
		fadeTime = 1,  // (Number) cross-fade time between successive Vertices
		label = \no,     // (Symbol) a custom name for the object
		info = "";       // (String) a custom info-text for object

		^super.new.pr_init(pSonicObject, pDur, fadeTime, label, info);
	}

	pr_init{

		arg pSonicObject_, pDur_, fadeTime_, label_, info_;

		case  // type-check all arguments here

		// type-check pSonicObject_
		{ // this a recursive clause
			if ( pSonicObject_.isKindOf(ListPattern) ) {
				pSonicObject_.list.deepCollect(inf, { arg item;
					var recursiveFunc = thisFunction;
					if (item.isKindOf(ListPattern)) {
						item.list.deepCollect(inf, recursiveFunc);
					} {
						if (item.isKindOf(SSG_AbstractSonicObject) || item.isNil ) {
							true
						} {
							false
						}
					}
				}).flatten(inf).any(_.not); // first flatten then test
			} {
				if ( pSonicObject_.isKindOf(SSG_AbstractSonicObject) || pSonicObject_.isNil ) {
					false
				} {
					true
				}
			}
		}
		{ Error("SSG_Sequence: 'pSonicObject' should be an instance of SSG_AbstractSonicObject or nil or an instance of ListPattern with a list comprised of only SSG_AbstractSonicObject objects or nil or of other ListPattern objects that recursively respect this rule.").throw }

		// type-check pDur_
		// { // this a recursive clause
		// 	if (pDur_.isKindOf(ListPattern)) {
		// 		pDur_.list.deepCollect(inf, { arg item;
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
		// 		if (pDur_.isKindOf(Number)) {
		// 			false
		// 		} {
		// 			true
		// 		}
		// 	}
		// }
		{
			pDur_.isKindOf(Pattern).not && pDur_.isKindOf(Number).not;
		}
		{ Error("SSG_Sequence: 'pDur' should be an instance of Float or Integer or a Patter.").throw }

		// type-check label_
		{label_.class != Symbol} { Error("SSG_Sequence: 'label' argument should be an instance of Symbol").throw }

		// type-check info_
		{info_.class != String} { Error("SSG_Sequence: 'info' argument should be an instance of String").throw }

		// type-check fadeTime_
		{fadeTime_.isKindOf(Number).not} { Error("SSG_Sequence: 'fadeTIme' argument should be an instance of Number").throw };

		// generate and assign a unique id
		counter = counter + 1;
		id = counter;

		// assign instance variables
		pSonicObject = Pseq([pSonicObject_],1).asStream;      // encapsulate in a Pseq to avoid having a naked SSG_SonicObject converted to Stream (this would cause the do method to behave in different than the desirable way)
		pDur = Pseq([pDur_],1).asStream; // encapsulate in a Pseq to avoid having a naked number converted to Stream (this would cause the do method to behave in different than the desirable way)
		label = label_;
		info = info_;
		fadeTime = fadeTime_;
		donePlaying = true;
		donePlayingCond = Condition.new(true);
	}

	// ========================= generic play/stop/reset ================

	play{ arg outBus = 0, group = 0, amp = 1; // find a way to use RootNode instead

		var currentSonicObject, nextSonicObject;  // the algorithm will attempt to load to RAM the next sonic-object while the currentSonicObject is playing to avoid discontinuities


		// if already playing then stop
		if (donePlaying.not) {
			"WIll Stop".postln;
			this.stop;
			this.reset;
		};

		// update playing status
		donePlaying = false;
		donePlayingCond.test_(false);
		donePlayingCond.signal;

		// group
		if ((group.isKindOf(CtkGroup) 
			|| group.isKindOf(Group) 
			|| group.isKindOf(Integer)).not) {
				// type check
				Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\): argument 'group' should be an instance of CtkGroup or of Group or of Integer (denoting group node)").throw;
			} {
				if (group.isKindOf(CtkGroup)) {
					group.play; // if all is ok play it
				}
			};

		// make sure previous synth will fade
		playSynth !? ({ playSynth.gate_(0); });

		Server.default.waitForBoot({ // boot server just in case..

			// play-back
			playRoutine = Task{

				// first load the firt two sound objects
				currentSonicObject = pSonicObject.next;
				currentSonicObject !? ({ // if currentSonicObject is not nil
					currentSonicObject.load;  // load buffer
					currentSonicObject.loadedCond.wait; // wait until it is loaded
				});
				nextSonicObject = pSonicObject.next;
				nextSonicObject !? ({ // if nextSonicObject is not nil
					if (nextSonicObject.loaded.not) {
						nextSonicObject.load;  // load buffer
						// nextSonicObject.loadedCond.wait; // wait until it is loaded
					};
				});

				pDur.do{ arg duration; // iterate through all duration values
					var vertex = currentSonicObject;
					vertex !? ({ // if vertex is not nil (which could be the case if pSonicObject stream has finished)
						
						// 1.postln;

						// vertex.buffer.duration.postln;
						// Server.default.sync;

						// play synthDef
						playSynth = synthDefs[vertex.synthDefTag]
						.note( target: group ).out_(outBus)
						.fadeTime_(fadeTime).buffer_(vertex.buffer)
						.amp_(amp).play;

						// schedule release
						// playSynth.release(duration);	
						playSynth.gate_(0,duration-fadeTime);
						// fork{ (duration+fadeTime).wait; playSynth.free }; // for some reason .free(time) won't work here..
					});
					
					// pre-load next sonic object
					currentSonicObject = nextSonicObject; // set next SonicObject as the new currentSonicObject
					nextSonicObject = pSonicObject.next;
					nextSonicObject !? ({ // if nextSonicObject is not nil
						// if not already loaded
						if (nextSonicObject.loaded.not) {
							nextSonicObject.load;  // load buffer
						};
						// DO NOT Wait until it's loaded (hopefully it will be by the time is needed - big files should be loaded to disk anyway) to avoid time discontinuites
						// nextSonicObject.loadedCond.wait; // wait until it is loaded			
					});

					// wait for duration minus fadeTime
					(duration-fadeTime).wait; 
				};

				// update playing status
				donePlaying = true;
				donePlayingCond.test_(true);
				donePlayingCond.signal;

				// stop and reset so that is ready to play again
				fork {
					this.stop;
					this.reset;
				}
			};

			playRoutine.play; // play the routine
		});
		^this;
	}

	stop {
		playRoutine.stop;
		playSynth.free;
		donePlaying = true;
		donePlayingCond.test_(true);
		donePlayingCond.signal;
		^this;
	}

	reset {
		playRoutine.reset;
		pSonicObject.reset;
		pDur.reset;
		^this;
	}

	// ===================== Non Real Time  =====================

	exportScore{ arg timeOffset = 0, maxDuration = 600, outBus = 0, group = 0, amp=1;

		var score = CtkScore.new;  // a new empty score;
		var time = timeOffset;     // the time of the next note event
		var buffersID = IdentitySet.new; // the ids of the buffers that have been added to the score (to avoid adding them multiple times)           

		// group
		if ((group.isKindOf(CtkGroup) 
			|| group.isKindOf(Group) 
			|| group.isKindOf(Integer)).not) {
				// type check
				Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\): argument 'group' should be an instance of CtkGroup or of Group or of Integer (denoting group node)").throw;
			} {
				if (group.isKindOf(CtkGroup)) {
					group.addTo(score); // if all is ok then add it to Score
				}
			};

		// bus
		if ((outBus.isKindOf(CtkAudio) 
			|| outBus.isKindOf(Bus)
			|| outBus.isKindOf(Integer) ).not) {
				// type check
				Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\): argument 'bus' should be an instance of CtkAudio or of Integer (denoting a bus)").throw;
			};

		this.stop;  // stop and 
		this.reset; // reset sequence just in case;

		block { arg break;
			pDur.do{ arg duration;

				var sonicObject = pSonicObject.next;

				if ( (sonicObject != nil) 
					&& (sonicObject.class!=SSG_NullSonicObject)) 
				// sonicObject !? ({ // if not nil
				{
					var note = synthDefs[sonicObject.synthDefTag]
					.note(time, duration + fadeTime, target:group)
					.out_(outBus).fadeTime_(fadeTime).amp_(amp)
					.buffer_(sonicObject.buffer);

					// add buffer to score
					if (buffersID.includes(sonicObject.buffer.bufnum).not) {
						sonicObject.buffer.addTo(score);
						buffersID.add(sonicObject.buffer.bufnum);
					};

					// add synth to score
					score.add(note);

					// release note
					score.add(note.gate_(0,duration-fadeTime));
					// score.add(note.release(duration+fadeTime));
					// score.add(note.release(duration));
				};
				// );
				
				// update time
				time = time + duration - fadeTime; // to compensate for fadeTime
				// time.postln;

				if (time > maxDuration) {
					break.value();
				};
			}};

		this.stop.reset; // stop/reset 
		
		^score; // return score
	}

	// ========================== public setters =========================

	setLabel {

		arg label_;

		// type-check argument and assing if ok
		if (label_.class != Symbol) {
			Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\): Error, argument was not an instance of Symbol.").throw;
		} {
			label = label_;
			("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'label' has been succesfully set to:" + label ++ ".").postln;
		}
		^this;
	}

	setInfo {

		arg info_;

		// type-check argument and assing if ok
		if (info_.class != String) {
			Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of String.").throw;
		} {
			info = info_;
			("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'info' has been succesfully set to:" + info).postln;
		}
		^this;
	}


	setFadeTime {

		arg fadeTime_;

		// type-check argument and assing if ok
		if (fadeTime_.isKindOf(Number).not) {
			Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\):  Error, argument was not an instance of Number.").throw;
		} {
			fadeTime = fadeTime_;
			("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'fadeTime' has been succesfully set to:" + fadeTime).postln;
		}
		^this;
	}


	setPSonicObject {

		arg pSonicObject_;

		if ( // type-check argument (recursively) and assing if ok
			if ( pSonicObject_.isKindOf(ListPattern) ) {
				pSonicObject_.list.deepCollect(inf, { arg item;
					var recursiveFunc = thisFunction;
					if (item.isKindOf(ListPattern)) {
						item.list.deepCollect(inf, recursiveFunc);
					} {
						if (item.isKindOf(SSG_AbstractSonicObject) || item.isNil ) {
							true
						} {
							false
						}
					}
				}).flatten(inf).any(_.not); // first flatten then test
			} {
				if ( pSonicObject_.isKindOf(SSG_AbstractSonicObject) || pSonicObject_.isNil ) {
					false
				} {
					true
				}
			} ) {
				Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'pSonicObject' should be an instance of SSG_AbstractSonicObject or nil or an instance of ListPattern with a list comprised of only SSG_AbstractSonicObject objects or nil or of other ListPattern objects that recursively respect this rule.").throw;
			} {
				pSonicObject = Pseq([pSonicObject_],1).asStream;
				("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'pSonicObject' has been succesfully set.").postln;
			};
		this.reset;
		^this;
	}

	setPDur {

		arg pDur_;

		if ( // type-check argument (recursively) and assing if ok
			if (pDur_.isKindOf(ListPattern)) {
				pDur_.list.deepCollect(inf, { arg item;
					var recursiveFunc = thisFunction;
					if (item.isKindOf(ListPattern)) {
						item.list.deepCollect(inf, recursiveFunc);
					} {
						if ( item.isKindOf(Number)) {
							true
						} {
							false
						}
					}
				}).flatten(inf).any(_.not); // first flatten then test
			} {
				if (pDur_.isKindOf(Number)) {
					false
				} {
					true
				}
			}
		) {
			Error("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'pDur' should be an instance of Float or Integer or of ListPattern with a list comprised of only instances of Float or of Integer or of other ListPattern objects that recursively respect this rule.").throw;
		} {
			pDur = Pseq([pDur_],1).asStream;
			("SSG_Sequence \(id:" ++ " " ++ id ++ "\): 'pDur' has been succesfully set.").postln;
		};
		this.reset;
		^this;
	}


	// ========================== public methods =========================

	// these should not be implemented in subclasses and are here only for testing purposes 

	nextSonicObject {
		var next;
		next = pSonicObject.next;       // get next value from the stream
		if (next.isNil) {          // if nil
			^SSG_NullSonicObject.new; // return a new SSG_NullSonicObject
		} {
			^next;                 // else simply return the SSG_SonicObject
		}
	}

	nextDur {
		^pDur.next.asFloat;       // return next value from the stream
	}

}


