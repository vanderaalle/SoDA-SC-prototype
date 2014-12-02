
// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project


TestSSG_AbstractSonicSource : UnitTest {

	classvar path;     // the path to the current folder (used to load locally stored files)

	var file, vertex, sequence;  // a default vertex object to use for the tests  
	var sonicEvent;
	var score;  // NRT score to test
	
	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
		vertex = SSG_SonicObject.new(file);
		sequence = SSG_Sequence(Pseq([vertex,vertex],1), Pseq([3,5],1));
	}
 
	test_chunk {
		
		// create a new SSG_AbstractSonicEvent
		sonicEvent = SSG_AbstractSonicSource.new(
			sequence,
			Pseq([0,4],1),  // pWait
			0.2,  // size
			0.5, // relative amplitude
			\anAbstractSonicSource, // label
			"this is a test SSG_AbstractSonicSource object" // info
		);

		"Testing SSG_AbstractSonicSource's object's type.".postln;
		this.assertEquals(sonicEvent.class, SSG_AbstractSonicSource,
			"Testing object's type.", false);

		"Testing some public accessors.".postln;
		this.assertEquals( sonicEvent.label, \anAbstractSonicSource, "Testing if 'label' (instance variable) has been initialized correctly.", false);
		this.assertEquals( sonicEvent.info, "this is a test SSG_AbstractSonicSource object", "Testing if 'info' (instance variable) has been initialized correctly.", false);

		Server.default.waitForBoot({

			fork {
				"Testing play method (there should be audio in the first 4 outputs  immediately, then a pause for 4 seconds and then audio again)".postln;
		
				sonicEvent.play(0, {"doneAction called !".postln;});
			
				10.wait;
				sonicEvent.stop;
				
				"Testing NRT (a score is created and played back simultaneously)".postln;
				score = sonicEvent.exportScore();
				score.play;
			}
		})
	}
}


TestSSG_SonicAtmosphere : UnitTest {
	
	classvar path;     // the path to the current folder (used to load locally stored files)

	var file, vertex;  // a default vertex object to use for the tests  
	var sonicAtmosphere;
	var score; // score object to test
	
	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {
		file = PathName(path).pathOnly ++ "/../test sound files/7985.aif";
		vertex = SSG_SonicObject.new(file);
	}
 
	test_chunk {
		
		// create a new SSG_AbstractSonicEvent
		sonicAtmosphere = SSG_SonicAtmosphere.new(
			vertex,
			0.5, // relative amplitude
			\anAbstractSonicSource, // label
			"this is a test SSG_AbstractSonicSource object" // info
		);

		Server.default.waitForBoot({
			
			fork {

				"Testing play method (there should be audio repeating constantly in the first 4 outputs)".postln;
		
				sonicAtmosphere.play(0);

				5.wait;
				sonicAtmosphere.stop;

				"Testing NRT (a score is created and played back simultaneously)".postln;
				score = sonicAtmosphere.exportScore;
				score.play
			}
		})
	}
}



TestSSG_FixedSound : UnitTest  {

	classvar path;     // the path to the current folder (used to load locally stored files)

	var file, vertex, sequence;  // a default vertex object to use for the tests  
	var sonicEvent;
	var score, pollingSynth;  // score and pollingSynth

	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
		vertex = SSG_SonicObject.new(file);
		sequence = SSG_Sequence(Pseq([vertex,vertex],1), Pseq([3,5],1));
	}
 
	test_chunk {
		
		// create a new SSG_AbstractSonicEvent
		sonicEvent = SSG_FixedSound.new(
			sequence,
			Pseq([0],1),  // pWait
			0.2,  // size
			0.5, // relative amplitude
			15@45@32, // cartesian
			\anAbstractSonicEvent, // label
			"this is a test SSG_AbstractSonicEvent object" // info
		);

		"Testing SSG_AbstractSonicEvent's object's type.".postln;
		this.assertEquals(sonicEvent.class, SSG_FixedSound,
			"Testing object's type.", false);
		
		pollingSynth = CtkNoteObject(SynthDef(\SSG_SonicSourcePollingSynth, 
			{In.kr(sonicEvent.position).poll;}
		));

		Server.default.waitForBoot({

			fork {
				"Testing play method (there should be audio in the first 4 outputs  immediately, then a pause for 4 seconds and then audio again)".postln;
		
				sonicEvent.play(0);	
		
				"Testing position output. Polling should now start and output only 0s for 4 seconds.".postln;
			
				4.wait;
				sonicEvent.stop;
			
				score = sonicEvent.exportScore;
				score.add(pollingSynth.note(0,10));
			
				score.play
			}	
		})
	}
}


TestSSG_AmbulatorySoundEnv : UnitTest  {

	classvar path;     // the path to the current folder (used to load locally stored files)

	var file, vertex, sequence;  // a default vertex object to use for the tests  
	var sonicEvent;
	var score, pollingSynth;

	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
		vertex = SSG_SonicObject.new(file);
		sequence = SSG_Sequence(Pseq([vertex],inf), Pseq([12],1));
	}
 
	test_chunk {
		
		// create a new SSG_AbstractSonicEvent
		sonicEvent = SSG_AmbulatorySoundEnv.new(
			sequence,
			Pseq([0],1),  // pWait
			0.2,  // size
			0.5, // relative amplitude
			[ 
				Env([0,10,5,10],[2,3,5],[-1,2,0]),
				Env([10,0,5,10],[2,3,5],[-1,2,0]),
				Env([0,1,-4,10],[2,3,5],[-1,2,0])
			], // trajectories
			\anAbstractSonicEvent, // label
			"this is a test SSG_AbstractSonicEvent object" // info
		);

		"Testing SSG_AbstractSonicEvent's object's type.".postln;
		this.assertEquals(sonicEvent.class, SSG_AmbulatorySoundEnv,
			"Testing object's type.", false);
	
		pollingSynth = CtkNoteObject(SynthDef(\SSG_SonicSourcePollingSynth, 
			{In.kr(sonicEvent.position).poll;}
		));
		
		Server.default.waitForBoot({

			fork {
				"Testing play method (there should be audio in the first 4 outputs  immediately, then a pause for 4 seconds and then audio again)".postln;
		
				sonicEvent.play(0);	
		
				"Testing position output. Polling should now start and output only 0s for 4 seconds.".postln;
				
				4.wait;
				sonicEvent.stop;
			
				score = sonicEvent.exportScore;
				score.add(pollingSynth.note(0,10));
			
				score.play
			}	
		})
	}
}



TestSSG_AmbulatorySoundFunc : UnitTest  {

	classvar path;     // the path to the current folder (used to load locally stored files)

	var file, vertex, sequence;  // a default vertex object to use for the tests  
	var sonicEvent;
	var score, pollingSynth;

	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
		vertex = SSG_SonicObject.new(file);
		sequence = SSG_Sequence(Pseq([vertex],inf), Pseq([12],1));

	}
 
	test_chunk {
		
		// create a new SSG_AbstractSonicEvent
		sonicEvent = SSG_AmbulatorySoundFunc.new(
			sequence,
			Pseq([0],1),  // pWait
			0.2,  // size
			0.5, // relative amplitude
			{
				Cartesian(60,30,rrand(0,100));
			}, // trajectory func
			1, // func wait time
			\anAbstractSonicEvent, // label
			"this is a test SSG_AbstractSonicEvent object" // info
		);

		"Testing SSG_AbstractSonicEvent's object's type.".postln;
		this.assertEquals(sonicEvent.class, SSG_AmbulatorySoundFunc,
			"Testing object's type.", false);
	
		
		pollingSynth = CtkNoteObject(SynthDef(\SSG_SonicSourcePollingSynth, 
			{In.kr(sonicEvent.position,3).poll;}
		));
		
		Server.default.waitForBoot({
			
			fork {
				"Testing play method (there should be audio in the first 4 outputs  immediately, then a pause for 4 seconds and then audio again)".postln;
		
				sonicEvent.play(0);	
		
				"Testing position output. Polling should now start and output only 0s for 4 seconds.".postln;
				
				4.wait;
				sonicEvent.stop;
				
				score = sonicEvent.exportScore;
				score.add(pollingSynth.note(0,10));
				
				score.play
			}	
		})
	}
}



TestSSG_SoundCloud : UnitTest  {

	classvar path;     // the path to the current folder (used to load locally stored files)

	var file, vertex, sequence;  // a default vertex object to use for the tests  
	var sonicEvent;
	var score, pollingSynth;

	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
		vertex = SSG_SonicObject.new(file);
		sequence = SSG_Sequence(Pseq([vertex],inf), Pseq([10],1));
	}
 
	test_chunk {
		
		// create a new SSG_AbstractSonicEvent
		sonicEvent = SSG_SoundCloud.new(
			sequence,
			Pseq([0],1),  // pWait
			0.2,  // size
			0.5, // relative amplitude
			0@0@0, // position
			100@100@100, // bounds
			10, // density
			\anAbstractSonicEvent, // label
			"this is a test SSG_AbstractSonicEvent object" // info
		);

		"Testing SSG_AbstractSonicEvent's object's type.".postln;
		this.assertEquals(sonicEvent.class, SSG_AmbulatorySoundEnv,
			"Testing object's type.", false);
	
		// pollingSynth = CtkNoteObject(SynthDef(\SSG_SonicSourcePollingSynth,
		// 	{In.kr(sonicEvent.position).poll;}
		// ));
		
		Server.default.waitForBoot({

			fork {
				"Testing play method (there should be audio in the first 4 outputs  immediately, then a pause for 4 seconds and then audio again)".postln;
		
				sonicEvent.play(0);	
		
				"Testing position output. Polling should now start and output only 0s for 4 seconds.".postln;
				
				4.wait;
				sonicEvent.stop;
			
				score = sonicEvent.exportScore;
				// score.add(pollingSynth.note(0,10));
			
				score.play
			}	
		})
	}
}
