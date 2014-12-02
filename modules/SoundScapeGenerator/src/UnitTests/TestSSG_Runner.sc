// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2014

// this file is part of the Sound Design Accelerator (SoDA) project


TestSSG_Runner : UnitTest {

	classvar path;     // the path to the current folder (used to load locally stored files)

	// sound sources
	var cityAmbience, bird1, bird2, car, dog;

	var zone1; 	// a sound zone
	var listener; // a listener
	var runner; // the runner
	var decoder; // the decoder

	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current folder
	}

	setUp {

		Server.default.waitForBoot({

			// ========================= static variables  =======================

			// city atmosphere
			cityAmbience = SSG_SonicAtmosphere(
				SSG_Vertex(   // sequence will be generated automatically given just a vertex
					PathName(path).pathOnly ++ "/../test sound files/city.aif")
			);

			bird1 = SSG_FixedSound(
				SSG_Vertex(PathName(path).pathOnly ++ "/../test sound files/bird1.wav"),
				Pseq([0,15],1),  0.1,  10@10@1
			); 

			bird2 = SSG_FixedSound(
				SSG_Vertex(PathName(path).pathOnly ++ "/../test sound files/bird2.wav"),
				Pseq([8,40],1),  0.1,  -10@5@1
			); 

			car = SSG_AmbulatorySoundEnv(
				SSG_Vertex(	PathName(path).pathOnly ++ "/../test sound files/car1.wav"),
				15, 4, [
					Env([0,100,50],[30,20]),
					Env([50,-140],[50]),
					Env([0.5,0.5],[50]),
				]
			);

			dog =  SSG_AmbulatorySoundEnv(
				SSG_Sequence(
					// pVertex
					Prand([ 
						SSG_Vertex(PathName(path).pathOnly ++ "/../test sound files/dog1.wav"), 
						SSG_Vertex(PathName(path).pathOnly ++ "/../test sound files/dog2.wav"), 
						SSG_Vertex(PathName(path).pathOnly ++ "/../test sound files/dog3.wav"), 
					],4),
					Prand([1,0.5,2],2)
				),
				Pseq([5,10,20],1),0.1,
				[
					Env([0,10,5],[5,20]),
					Env([5,-14],[40]),
					Env([0.5,0.5],[50]),
				]
			);

			listener = SSG_FixedListener(0@0@2,100);

			zone1 = SSG_SoundZone(500@500@10,0@0@0, [
				cityAmbience, car, bird1, bird2, dog
			]);
			
			decoder = FoaDecoderMatrix.newStereo;
			
			runner = SSG_Runner([zone1],listener, decoder);
		});
	}
	
	test_chunk {

		runner.play(0);
	}
	
}
