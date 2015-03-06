
// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// SSG_Runner

SSG_Runner {

	// ========================= static variables  =======================
	classvar synthDefs;       // (CtkProtoNotes) the various synthDefs used
	classvar localizeGroup, zoneGroup, renderGroup;   // (CtkGroup) local groups to ensure order of execution

	// ========================= instance variables  ====================

	var decoder;        // (FoaDecoderMatrix) the decoder
	var sonicSpace;     // (Array of SSG_SoundZones) the soundZones
	var listener;       // (SSG_AbstractListener) the listener

	var zoneSynths;         // (Array of CtkNotes) synths for the global zone sound and routing to master out
	var renderSynth;          // (CtkNote) the master decoding synth

	var zoneBuses;          // (Array of CtkAudio) buses to route audio to the zoneSynths
	var renderBus;         // // (CtkAudio) the master bus with the mix for the decoding synth
	var <readyCond; // (Condition) condition to indicate when bouncing is ready 

	// ===================== initClass  =========================

	*initClass {

		// init dependent classes
		Class.initClassTree(OSCresponder);
		Class.initClassTree(SynthDescLib);
		Class.initClassTree(SynthDef);
		Class.initClassTree(CtkProtoNotes);
		Class.initClassTree(CtkGroup);
		Class.initClassTree(FoaDecoderMatrix);

		localizeGroup = CtkGroup.new(0, addAction:\after, target:SSG_AbstractSonicSource.group, server:Server.default);
		zoneGroup = CtkGroup.new(0, addAction:\after, target:localizeGroup, server:Server.default);
		renderGroup = CtkGroup.new(0, addAction:\after, target:zoneGroup, server:Server.default);

		synthDefs = CtkProtoNotes(
			SynthDef(\localize, {
				arg out = 0, inBus, positionBus, listenerPositionBus,
				distance = 1, amp=0, duration, listeningRadius=200,
				size = 0.1;

				var signal = In.ar(inBus,4);  // b-format in

				// calculate relative to listener position
				// var position = In.kr(positionBus,3)
				//- In.kr(listenerPositionBus,3);
				 var position = In.kr(listenerPositionBus,3)
				- In.kr(positionBus,3);

				// calculate radius (r = sqrt(x^2 + y^2 + z^2)) with respect to size
				var radius = (Mix.new(position**2).abs.sqrt - (size/2))
				.max(0.0000001); // distance cannot be zero, size is assumed to be the diameter of a spherical object

				// calculate distortion angle
				var angle = radius.linlin(0,5,0,pi/2); // ??????

				// calculate azimuth (arctan(y/x))
				var azimuth = ( position[0] / position[1] ).atan;

				// calculate zenith (acos(z/r))
				var zenith = (( position[0].squared + position[1].squared).sqrt / position[2]).atan;

				// localize
				signal = FoaPush.ar(signal,angle, K2A.ar(azimuth), K2A.ar(zenith));

				// consider distance
				signal = signal * (radius.linexp(0,listeningRadius,2,1)-1); // because linexp cannot have 0 as an outMax

				// add proximity effect
				signal = FoaProximity.ar(HPF.ar(signal, 20),radius); // fltering is necessary because of FoaProximity's implementation

				Out.ar(out, signal);
			}),
			SynthDef(\zoneProfile, {
				arg inBus, out=0, roomSize = 100, reverbTime = 0.3,
				reverbDamp = 0.8, earlyMix = 0.5, lateMix = 0.3,
				maxRoomSize = 101, impulseBuf, alpha = 1, filter = 1,
				zonePosition=#[0,0,0], zoneBounds=#[1,1,1],
				listenerPositionBus;
				var signal = In.ar(inBus, 4);
				var listenerPosition = In.kr(listenerPositionBus,3);

				signal = [ // only the left channel of a stereo reverb for each channel (FreeVerb doesn't work here, it produces nan (why???))
					GVerb.ar(signal[0],roomSize,reverbTime, reverbDamp,
						1,0,1,earlyMix,lateMix,maxRoomSize)[0],
					GVerb.ar(signal[1],roomSize,reverbTime, reverbDamp,
						1,0,1,earlyMix,lateMix,maxRoomSize)[0],
					GVerb.ar(signal[2],roomSize,reverbTime, reverbDamp,
						1,0,1,earlyMix,lateMix,maxRoomSize)[0],
					GVerb.ar(signal[3],roomSize,reverbTime, reverbDamp,
						1,0,1,earlyMix,lateMix,maxRoomSize)[0],
				];

				signal = (
					// if listener is OUTSIDE this zone filter/process accordingly (filter/alpha should be that of the zone the listener is currently IN!!!)
					InRange.kr(listenerPosition[0],
						zonePosition[0], zonePosition[0]+zoneBounds[0]) &
					InRange.kr(listenerPosition[1],
						zonePosition[1], zonePosition[1]+zoneBounds[1]) &
					InRange.kr(listenerPosition[2],
						zonePosition[2], zonePosition[2]+zoneBounds[2])
				).if(signal,RLPF.ar(signal,350*filter,1/filter)*alpha);

				Out.ar(out, signal);
			})
		);
	}

	// ===================== new/init =========================

	*new {

		arg
		sonicSpace,           // (Array of SSG_SoundZones) the sonic space
		listener,             // (SSG_AbstractListener) the listener
		decoder;              // (FoaDecoderMatrix) the decoder

		^super.new.pr_init(sonicSpace, listener, decoder);
	}

	pr_init{
		arg sonicSpace_, listener_, decoder_;

		// type check
		case
		{sonicSpace_.class!=Array} {
			Error("SSG_Runner: argument 'sonicSpace' should be an instance of Array containing SSG_SoundZone objects").throw;}
		{sonicSpace_.every(_.isKindOf(SSG_SoundZone)).not} {
			Error("SSG_Runner: argument 'sonicSpace' should be an instance of Array containing SSG_SoundZone objects").throw;}
		{listener_.isKindOf(SSG_AbstractListener).not} {
			Error("SSG_Runner: argument 'listener' should be an instance of SSG_AbstractListener").throw;}
		{decoder_.class!=FoaDecoderMatrix} {
			Error("SSG_Runner: argument 'decoder' should be an instance of FoaDecoderMatrix").throw;};

		// init variables
		listener = listener_;
		sonicSpace = sonicSpace_;
		decoder = decoder_;

		// condition
		readyCond = Condition.new(false);

		// add decoder synthDef (now that the decoder is known)
		synthDefs.add(
			SynthDef(\decode, {
				arg out=0, inBus;
				var signal = In.ar(inBus, 4);
				// decode
				signal = FoaDecode.ar(signal,decoder)*0.9; // just a bit of attenuation
				Out.ar(out,signal);
			}));

		// buses
		renderBus = CtkAudio(4);
		zoneBuses = Array.fill(sonicSpace.size,{ CtkAudio(4) });
		// isListenerInZoneBus = CtkControl((sonicSpace.size,0));

	}

	// ===================== RT methods =========================

	play { arg outBus = 0;

		// localizeSynth is used to localize each individual event, then sound is sent to the zone's global bus and the overall sound of each zone is processed with respect to the listener's position and the reverb/resonance/etc characteristics of each zone as well as the opacity characteristics of other nearby zones. Finally the renderSynth produces the final mix.

		// groups
		SSG_AbstractSonicSource.group.play;
		localizeGroup.play;
		zoneGroup.play;
		renderGroup.play;

		// buses
		zoneBuses.do{arg i; i.play};
		renderBus.play;
		// isListenerInZoneBus.play;

		// listener
		listener.play;

		sonicSpace.do{ arg zone, index;

			// zone bus
			var zoneBus = zoneBuses[index];
			zoneBus.play;

			// play all soundSoudrces on every zone
			zone.soundSources.do{ arg soundSource;

				// !! ADD A SPECIAL CASE CLAUSE FOR SSG_SoundCloud

				if (soundSource.isKindOf(SSG_SonicAtmosphere)) {
					soundSource.play(zoneBus);
				} {
					var bus, synth;

					bus = CtkAudio(4).play;

					synth = synthDefs[\localize].note(
						target: localizeGroup
					).out_(zoneBus).inBus_(bus)
					.positionBus_(soundSource.position)
					.listenerPositionBus_(listener.position)
					.listeningRadius_(listener.listeningRadius)
					.size_(soundSource.size).play;

					soundSource.play(bus, {bus.free; synth.free});
				}

				// I have to make sure that localize synths only play back when needed
			};

			// process with the characteristics of each zone
			zoneSynths = zoneSynths.add(
				synthDefs[\zoneProfile].note(
					target: zoneGroup
				)
				.filter_(zone.filterSlope)
				.alpha_(zone.opacity)
				.roomSize_(zone.reverbProfile[0])
				.reverbTime_(zone.reverbProfile[1])
				.reverbDamp_(zone.reverbProfile[2])
				.earlyMix_(zone.reverbProfile[3])
				.lateMix_(zone.reverbProfile[4])
				.maxRoomSize_(zone.reverbProfile[0]+1)
				.zonePosition_(zone.position.asArray)
				.zoneBounds_(zone.bounds.asArray)
				.listenerPositionBus_(listener.position)
				// .impulseBuf_()
				.out_(renderBus).inBus_(zoneBus).play;
			);

		};

		// decode signal
		renderSynth = synthDefs[\decode].note(
			target: renderGroup
		).out_(outBus).inBus_(renderBus).play;
	}

	stop {
		// stop and reset all scheduled soundSources
		sonicSpace.do{ arg zone, index;
			zone.soundSources.do{ arg soundSource;
				soundSource.stop;
				soundSource.reset;
			};
		};

		// are they stoped
		zoneSynths.do{arg i; i.free};
		renderSynth.free;
	}

	// ===================== NRT methods =========================


	bounce { arg duration = 600, path = nil;

		var masterScore = CtkScore.new; // the master score
		var file; // the path to write audio to

		// type checking
		if (path.notNil) {
			if (path.isKindOf(String).not) {
				Error("SSG_Runner: path should be an instance of String or Nil").throw;
			};

			if ((PathName(path).extension!="aiff")) {
				Error("SSG_Runner: Only 'aiff' extension is supported").throw;
			};

			file = path;

		} {
			file = PathName(thisProcess.nowExecutingPath).pathOnly ++ Date.getDate.asSortableString ++ "_" ++UniqueID.next.asString ++ ".aiff";
		};

		// export all scores and merge them to the master
		sonicSpace.do{ arg zone, index;

			// zone bus
			var zoneBus = zoneBuses[index];

			zone.soundSources.do{ arg soundSource;
				var score = CtkScore.new;  // the score

				// !! ADD A SPECIAL CASE CLAUSE FOR SSG_SoundCloud

				if (soundSource.isKindOf(SSG_SonicAtmosphere)) {
					score = soundSource.exportScore(0,duration,zoneBus);
				} {
					var bus = CtkAudio.new(4);
					var synth = synthDefs[\localize].note(
						target: localizeGroup
					).out_(zoneBus).inBus_(bus)
					.positionBus_(soundSource.position)
					.listenerPositionBus_(listener.position)
					.listeningRadius_(listener.listeningRadius)
					.size_(soundSource.size);
					score.add(bus);
					score.add(synth);
					score.add(soundSource.exportScore(0,duration,bus));
				};
				masterScore.merge(score); // merge to master score

			};

			// process with the characteristics of each zone
			masterScore.add(
				synthDefs[\zoneProfile].note(
					target: zoneGroup
				)
				.filter_(zone.filterSlope)
				.alpha_(zone.opacity)
				.roomSize_(zone.reverbProfile[0])
				.reverbTime_(zone.reverbProfile[1])
				.reverbDamp_(zone.reverbProfile[2])
				.earlyMix_(zone.reverbProfile[3])
				.lateMix_(zone.reverbProfile[4])
				.maxRoomSize_(zone.reverbProfile[0]+1)
				.zonePosition_(zone.position.asArray)
				.zoneBounds_(zone.bounds.asArray)
				.listenerPositionBus_(listener.position)
				.out_(renderBus).inBus_(zoneBus);
			);
		};

		// decode signal
		masterScore.add(synthDefs[\decode].note(
			target: renderGroup
		).out_(0).inBus_(renderBus));

		// add buses and groups to the masterScore
		masterScore.add(SSG_AbstractSonicSource.group);
		masterScore.add(localizeGroup);
		masterScore.add(zoneGroup);
		masterScore.add(renderGroup);
		zoneBuses.do{arg item;
			masterScore.add(item);
		};
		masterScore.add(renderBus);
		// masterScore.add(listener.position);
		masterScore.add(listener.exportScore);

		// bounce score
		masterScore.write(file,duration,48000,"AIFF","int24",options:ServerOptions.new.numOutputBusChannels_(decoder.numChannels), action:{	
			readyCond.test_(true);
			readyCond.signal;}
		);
	}
}


