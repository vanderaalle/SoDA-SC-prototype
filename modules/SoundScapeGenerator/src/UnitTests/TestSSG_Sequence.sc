// CIRMA, Univerista Degli Studi di Torino
// developed by Marinos Koutsomichalis (marinos.koutsomichalis@unito.it)
// (c) 2013

// this file is part of the Sound Design Accelerator (SoDA) project

// SSG_AbstractSonicSource unit tests


TestSSG_Sequence : UnitTest {
	
	var file, vertex;  // a default vertex object to use for the tests  
	var sonicSource;
	var arrayOfSonicSources, arrayOfIds;  // used for the uniqueID test

	setUp {
		file = Platform.resourceDir +/+ "sounds/a11wlk01-44_1.aiff";
		vertex = SSG_SonicObject.new(file);
	}
	
	test_chunk {
		
		// create a new SSG_Sequence
		sonicSource = SSG_Sequence.new(
			Pseq([vertex,nil,Prand([vertex,nil],2),nil],inf), // pSonicObject
			Prand([1,2,Prand([3,4],2)],inf),  // pDur
			0.7,  // fadeTIme
			\aChunk, // label
			"this is a test SSG_Sequence object" // info
		);

		"Testing SSG_Sequence object's type.".postln;
		this.assertEquals( sonicSource.class, SSG_Sequence,
			"Testing object's type.", false);

		"Testing some public accessors.".postln;
		this.assertEquals( sonicSource.label, \aChunk, "Testing if 'label' (instance variable) has been initialized correctly.", false);
		this.assertEquals( sonicSource.info, "this is a test SSG_Sequence object", "Testing if 'info' (instance variable) has been initialized correctly.", false);
		this.assertEquals( sonicSource.fadeTime, 0.7, "Testing if 'fadeTIme' (instance variable) has been initialized correctly.", false);

		"Testing if automatically generated ids are trully unique".postln;
		arrayOfSonicSources = Array.fill(100,{SSG_Sequence.new()});
		arrayOfIds = arrayOfSonicSources.collect(_.id);
		this.assertEquals( arrayOfIds.as(IdentitySet).size, 100, "Testing if automatically generate ids are trully unique.", false);
		
		"Testing Exception-Handling".postln;
		try {
			SSG_Sequence.new(1);
		} { arg error;
			this.assertEquals( error.what, "SSG_Sequence: 'pSonicObject' should be an instance of SSG_AbstractSonicObject or nil or an instance of ListPattern with a list comprised of only SSG_AbstractSonicObject objects or nil or of other ListPattern objects that recursively respect this rule.", "Testing if object returns the correct error.", false);
		};

		try {
			SSG_Sequence.new(vertex,\something);
		} { arg error;
			this.assertEquals( error.what, "SSG_Sequence: 'pDur' should be an instance of Float or Integer or of ListPattern with a list comprised of only instances of Float or of Integer or of other ListPattern objects that recursively respect this rule.","Testing if object returns the correct error.", false);
		};

		try {
			SSG_Sequence.new(vertex,1,nil);
		} { arg error;
			this.assertEquals( error.what, "SSG_Sequence: 'fadeTIme' argument should be an instance of Number","Testing if object returns the correct error.", false);
		};

		try {
			SSG_Sequence.new(vertex,1,1,nil);
		} { arg error;
			this.assertEquals( error.what, "SSG_Sequence: 'label' argument should be an instance of Symbol","Testing if object returns the correct error.", false);
		};

		try {
			SSG_Sequence.new(vertex,1,1,\aSymbol,\anotherSymbol);
		} { arg error;
			this.assertEquals( error.what, "SSG_Sequence: 'info' argument should be an instance of String","Testing if object returns the correct error.", false);
		};

		"Testing public methods".postln;
		this.assertEquals( sonicSource.nextSonicObject, vertex,"Testing if SSG_SonicObject object is properly returned.", false);
		this.assertEquals( sonicSource.nextSonicObject.class, SSG_NullSonicObject,"Testing if nils are properly converted to instances of SSG_NullSonicObject.", false);
		this.assertEquals( sonicSource.nextDur.class, Float,"Testing if dur values returned are of the correct type.", false);

		"Testing public setters".postln;
		// setPDur
		sonicSource.setPDur(Pseq([1,2,Pseq([2.0,3.0],2)],5));
		this.assertEquals( sonicSource.nextDur, 1,"Testing if 'pDur' value has been properly set.", false);
		
		// setLabel 
		sonicSource.setLabel(\anotherLabel);
		this.assertEquals( sonicSource.label, \anotherLabel,"Testing if 'label' value has been properly set.", false);

		// setInfo
		sonicSource.setInfo("test information text");
		this.assertEquals( sonicSource.info, "test information text","Testing if 'info' value has been properly set.", false);
		
		// setPSonicObject
		sonicSource.setPSonicObject(Pseq([nil,vertex],inf));
		this.assertEquals( sonicSource.nextSonicObject.class, SSG_NullSonicObject ,"Testing if 'pSonicObject' value is of proper type.", false);
		this.assertEquals( sonicSource.nextSonicObject, vertex ,"Testing if 'pSonicObject' value has been properly set.", false);

		// setFadeTime
		sonicSource.setFadeTime(4);
		this.assertEquals( sonicSource.fadeTime, 4,"Testing if 'fadeTime' value has been properly set.", false);

		// test playback/reset
		// create a new SSG_Sequence
		sonicSource = SSG_Sequence.new(
			Pseq([vertex,nil,vertex],1), // pSonicObject
			Pseq([4,2,4],1),  // pDur
			0.1,  // fadeTIme
			\aChunk, // label
			"this is a test SSG_Sequence object" // info
		);
		
		"Testing playback in RT, you should hear 4 seconds of sound followed by 2 seconds of silence and another 4seconds of sound, and then the same again.".postln;
		Server.default.waitForBoot({
			fork {
				sonicSource.play(0);
				sonicSource.donePlayingCond.wait;
				sonicSource.reset;
				sonicSource.play(0);
			}
		})
	}
}