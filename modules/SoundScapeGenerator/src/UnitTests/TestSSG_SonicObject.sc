// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the Sound Design Accelerator (SoDA) project

// SSG_Vertex tests

TestSSG_SonicObject : UnitTest {
	
	var vertex, vertexB;  // a default vertex object to use for the tests  
	var arrayOfVertices, arrayOfIds;  // used for the uniqueID test
	var file;    // a test file

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
	}
	
	test_vertex {

		"Testing construction and public instance variables.".postln;
		vertex = SSG_SonicObject.new();
		this.assertEquals( vertex.class, SSG_NullSonicObject, "Testing if an SSG_NullSonicObject is returned for invalid paths.", false);
		vertex = SSG_SonicObject.new(file,\aNewFile,"custom info tag",false);
		this.assertEquals( vertex.id.class, Integer, "Testing id.", false);
		this.assertEquals( vertex.label, \aNewFile, "Testing label.", 
			false);
		this.assertEquals( vertex.info, "custom info tag", "Testing info.",
			false);
		this.assertFloatEquals( vertex.duration, 2.4380952380952, 
			"Testing duration.", 0.0001, false);
		this.assertEquals( vertex.path, file, "Testing path.", false);
		this.assertEquals( vertex.numChannels, 1, "Testing numChannels.",
			false);
		this.assertEquals( vertex.sampleRate, 44100, "Testing sampleRate.",
			false);
		this.assertEquals( vertex.numFrames, 107520, "Testing numFrames.", 
			false);
		this.assertEquals( vertex.buffer.class, CtkBuffer, 
			"Testing buffer.", false);
		this.assertEquals( vertex.disk, false, "Testing disk.", false);
		this.assertEquals( vertex.synthDefTag, \monoRAM, 
			"Testing synthDefTag.", false);

		"Testing exception handling.".postln;
		try {
			SSG_SonicObject.new(1);
		} { arg error;
			this.assertEquals( error.what, "SSG_SonicObject: 'path' argument should be an instance of String", "Testing if object returns the correct error.", false);
		};

		try {
			SSG_SonicObject.new(file,nil);
		} { arg error;
			this.assertEquals( error.what, "SSG_SonicObject: 'label' argument should be an instance of Symbol", "Testing if object returns the correct error.", false);
		};

		try {
			SSG_SonicObject.new(file,\aLabel,nil);
		} { arg error;
			this.assertEquals( error.what, "SSG_SonicObject: 'info' argument should be an instance of String", "Testing if object returns the correct error.", false);
		};

		try {
			SSG_SonicObject.new(file,\aLabel,"",nil);
		} { arg error;
			this.assertEquals( error.what, "SSG_SonicObject: 'disk' argument should be an instance of Boolean", "Testing if object returns the correct error.", false);
		};
		
		"Testing if automatically generated ids are trully unique".postln;
		arrayOfVertices = Array.fill(100,{SSG_SonicObject.new(file)});
		arrayOfIds = arrayOfVertices.collect(_.id);
		this.assertEquals( arrayOfIds.as(IdentitySet).size, 100, "Testing if automatically generate ids are trully unique.", false);

		Server.default.waitForBoot({
			fork{
				"Testing RT loading and play-back - You should hear some sound (if not debug!)".postln;
				vertex = SSG_SonicObject.new(file);
				vertex.play;
				vertex.duration.wait;
				"Testing RT loading and play-back from disk - You should hear a sound (if not debug!)".postln;
				vertex = SSG_SonicObject.new(file,disk:true);
				vertex.play;
			};
		});

	}

}
