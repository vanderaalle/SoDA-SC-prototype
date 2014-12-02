// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the SoundScape Generator, which has been developed as part of the Sound Design Accelerator (SoDA) project

// SSG_SoundZone unit tests


TestSSG_SoundZone : UnitTest {

	classvar path;

	var soundZone, atmosphere, event, vertexA, vertexB; 
	var arrayOfZones, arrayOfIds;

	*initClass { 
		path = this.filenameSymbol.asString; // the path to the current file
	}

	setUp {
		vertexA = SSG_Vertex.new(
			PathName(path).pathOnly ++ "test sound files/7985.aif");
		vertexB = SSG_Vertex.new(Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff");
		atmosphere = SSG_SonicAtmosphere(vertexA, 10);
		event = SSG_FixedSound(vertexB, 10, position:0.5@0.1@0.9);
	}

	test_chunk {

		// create a new soundZone
		soundZone = SSG_SoundZone(
			10@10@10,            // bounds
			0@0@0,               // position
			[atmosphere, event], // soundSources
			1,                   // opacity
			1,                   // filterSlope
			nil,                 // reverbProfile
			nil,                 // resonanceProfile
			\aSoundZone, "no info" // label/info
		);
		
		"Testing object's type.".postln;
		this.assertEquals(soundZone.class, SSG_SoundZone,
			"Testing object's type.", false);

		"Testing returned values and types".postln;
		this.assertEquals( soundZone.label, \aSoundZone, "Testing if 'label' (instance variable) has been initialized correctly.", false);
		this.assertEquals( soundZone.info, "no info", "Testing if 'info' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.bounds, 10@10@10, "Testing if 'bounds' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.position, 0@0@0, "Testing if 'position' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.soundSources, [atmosphere, event], "Testing if 'soundSources' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.opacity, 1, "Testing if 'opacity' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.filterSlope, 1, "Testing if 'filterSlope' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.reverbProfile, nil, "Testing if 'reverbProfile' (instance variable) has been initialized correctly.", false);
		this.assertEquals(soundZone.resonanceProfile, nil, "Testing if 'resonanceProfile' (instance variable) has been initialized correctly.", false);

		"Testing if automatically generated ids are trully unique".postln;
		arrayOfZones = Array.fill(100,{
			SSG_SoundZone(10@10@10,0@0@0,[atmosphere, event],1,1)});
		arrayOfIds = arrayOfZones.collect(_.id);
		this.assertEquals( arrayOfIds.as(IdentitySet).size, 100, "Testing if automatically generate ids are trully unique.", false);

		"Testing public methods".postln;
		soundZone.setInfo("another kind of text");
		this.assertEquals( soundZone.info, "another kind of text", "Testing if 'setInfo' method behaves as expected.", false);

		soundZone.setLabel(\anotherLabel);
		this.assertEquals( soundZone.label, \anotherLabel, "Testing if 'setLabel' method behaves as expected.", false);
		
		soundZone.addSoundSource(event);
		this.assertEquals( soundZone.soundSources, [atmosphere, event, event], "Testing if 'addSoundSource' method behaves as expected.", false);
	}
	
}
